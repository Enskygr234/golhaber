package com.enskygr.golhaber.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.enskygr.golhaber.data.GoalMode
import com.enskygr.golhaber.data.Match
import com.enskygr.golhaber.data.MatchState
import com.enskygr.golhaber.data.NewsRepository
import com.enskygr.golhaber.data.Prefs
import com.enskygr.golhaber.data.ScoreRepository
import com.enskygr.golhaber.data.teamMatches
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Arka planda skorları ve transfer haberlerini kontrol eder.
 * Takip edilen canlı maç varken kendini 2 dakikada bir yeniden planlar.
 */
class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        var liveRelevant = false
        try {
            liveRelevant = checkGoals(prefs)
        } catch (_: Exception) {
        }
        try {
            checkNews(prefs)
        } catch (_: Exception) {
        }
        if (liveRelevant) Scheduler.scheduleLive(applicationContext, fromLiveChain = inputData.getBoolean(KEY_LIVE, false))
        return Result.success()
    }

    private fun isRelevant(m: Match, p: Prefs): Boolean = when (p.goalMode) {
        GoalMode.ALL -> true
        GoalMode.FAVORITES -> m.id in p.followedMatches ||
            teamMatches(m.home.name, p.favoriteTeams) || teamMatches(m.away.name, p.favoriteTeams)
        else -> false
    }

    private suspend fun checkGoals(p: Prefs): Boolean {
        if (p.goalMode == GoalMode.OFF) return false
        val today = LocalDate.now()
        val yesterdayLive = try {
            ScoreRepository.fetch(p.enabledLeagues(), today.minusDays(1)).filter { it.isLive }
        } catch (_: Exception) {
            emptyList()
        }
        val matches = (ScoreRepository.fetch(p.enabledLeagues(), today) + yesterdayLive).distinctBy { it.id }
        val old = p.lastScores
        val next = HashMap<String, String>()
        var anyLive = false
        for (m in matches) {
            val cur = "${m.scoreText}|${m.state.name}"
            next[m.id] = cur
            if (!isRelevant(m, p)) continue
            if (m.isLive) anyLive = true
            val prev = old[m.id] ?: continue
            val (prevScore, prevState) = prev.split("|").let { it[0] to it.getOrElse(1) { "" } }
            val prevTotal = prevScore.split("-").sumOf { it.toIntOrNull() ?: 0 }
            val curTotal = (m.home.score ?: 0) + (m.away.score ?: 0)
            val line = "${m.home.shortName} ${m.home.score ?: 0}-${m.away.score ?: 0} ${m.away.shortName}"
            if (curTotal > prevTotal && m.state != MatchState.PRE) {
                val last = m.goals.lastOrNull()
                val detail = buildString {
                    if (last != null && last.player.isNotBlank()) {
                        append(last.minute).append(" ").append(last.player)
                        if (last.penalty) append(" (P)")
                        if (last.ownGoal) append(" (KK)")
                        append(" · ")
                    } else if (m.isLive) append(m.statusText).append(" · ")
                    append(m.league.name)
                }
                Notifier.goal(applicationContext, p.sound, (m.id + m.scoreText).hashCode(), "⚽ GOL! $line", detail)
            } else if (p.notifyFinal && m.state == MatchState.POST && prevState == MatchState.LIVE.name) {
                Notifier.goal(applicationContext, p.sound, (m.id + "FT").hashCode(), "🏁 Maç sonu: $line", m.league.name)
            }
        }
        // Eski kayıtları koru ki gece yarısı geçişinde bilgi kaybolmasın
        p.lastScores = (old + next).entries.toList().takeLast(400).associate { it.key to it.value }
        return anyLive
    }

    private suspend fun checkNews(p: Prefs) {
        if (!p.notifyTransfer) return
        val queries = buildList {
            add("futbol transfer")
            p.favoriteTeams.take(2).forEach { add("$it transfer") }
        }
        val items = queries.flatMap { q -> runCatching { NewsRepository.fetch(q) }.getOrDefault(emptyList()) }
            .distinctBy { it.link }
        if (items.isEmpty()) return
        val seen = p.seenNews
        if (seen.isEmpty()) {
            p.seenNews = items.map { it.link }
            return
        }
        val seenSet = seen.toHashSet()
        val fresh = items.filter { it.link !in seenSet }.take(3)
        fresh.forEach { n ->
            Notifier.news(applicationContext, n.link.hashCode(), "📰 Transfer · ${n.source}", n.title, n.link)
        }
        p.seenNews = seen + items.map { it.link }.filter { it !in seenSet }
    }

    companion object {
        const val KEY_LIVE = "live"
    }
}

object Scheduler {
    private val netConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(netConstraint)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("golhaber_periodic", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun scheduleLive(ctx: Context, fromLiveChain: Boolean) {
        val req = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setConstraints(netConstraint)
            .setInputData(workDataOf(CheckWorker.KEY_LIVE to true))
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "golhaber_live",
            if (fromLiveChain) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
            req,
        )
    }

    /** Uygulama açıldığında hemen bir kontrol yap. */
    fun kick(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<CheckWorker>().setConstraints(netConstraint).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("golhaber_kick", ExistingWorkPolicy.KEEP, req)
    }
}
