package com.enskygr.golhaber.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enskygr.golhaber.data.Leagues
import com.enskygr.golhaber.data.Match
import com.enskygr.golhaber.data.MatchState
import com.enskygr.golhaber.data.NewsItem
import com.enskygr.golhaber.data.NewsRepository
import com.enskygr.golhaber.data.NewsTopic
import com.enskygr.golhaber.data.NewsTopics
import com.enskygr.golhaber.data.Prefs
import com.enskygr.golhaber.data.ScoreRepository
import com.enskygr.golhaber.notify.Notifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    // --- Skorlar
    var dayOffset by mutableIntStateOf(0)
        private set
    var selectedLeague by mutableStateOf<String?>(null)
    var matches by mutableStateOf<List<Match>>(emptyList())
        private set
    var scoresLoading by mutableStateOf(false)
        private set
    var scoresError by mutableStateOf<String?>(null)
        private set

    // --- Haberler
    var topic by mutableStateOf(NewsTopics.all.first())
        private set
    var news by mutableStateOf<List<NewsItem>>(emptyList())
        private set
    var newsLoading by mutableStateOf(false)
        private set
    var newsError by mutableStateOf<String?>(null)
        private set

    // --- Ayarlar (Compose için durum kopyaları)
    var goalMode by mutableIntStateOf(prefs.goalMode)
        private set
    var notifyTransfer by mutableStateOf(prefs.notifyTransfer)
        private set
    var notifyFinal by mutableStateOf(prefs.notifyFinal)
        private set
    var sound by mutableStateOf(prefs.sound)
        private set
    var favoriteTeams by mutableStateOf(prefs.favoriteTeams)
        private set
    var followed by mutableStateOf(prefs.followedMatches)
        private set
    var leagues by mutableStateOf(prefs.leagues)
        private set
    var notifPermission by mutableStateOf(Notifier.canNotify(app))
        private set

    private var scoresJob: Job? = null
    private var newsJob: Job? = null

    init {
        refreshScores()
        refreshNews()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (dayOffset == 0 && matches.any { it.state == MatchState.LIVE }) refreshScores(silent = true)
            }
        }
    }

    val date: LocalDate get() = LocalDate.now().plusDays(dayOffset.toLong())

    fun visibleMatches(): List<Match> {
        val sel = selectedLeague
        return if (sel == null) matches else matches.filter { it.league.code == sel }
    }

    fun setDay(offset: Int) {
        if (offset == dayOffset) return
        dayOffset = offset
        matches = emptyList()
        refreshScores()
    }

    fun refreshScores(silent: Boolean = false) {
        scoresJob?.cancel()
        scoresJob = viewModelScope.launch {
            if (!silent) scoresLoading = true
            try {
                matches = ScoreRepository.fetch(prefs.enabledLeagues(), date)
                scoresError = null
            } catch (e: Exception) {
                if (!silent) scoresError = "Skorlar yüklenemedi. İnternet bağlantını kontrol et."
            } finally {
                scoresLoading = false
            }
        }
    }

    fun selectTopic(t: NewsTopic) {
        if (t == topic) return
        topic = t
        news = emptyList()
        refreshNews()
    }

    fun refreshNews() {
        newsJob?.cancel()
        newsJob = viewModelScope.launch {
            newsLoading = true
            try {
                news = NewsRepository.fetch(topic.query).take(60)
                newsError = null
            } catch (e: Exception) {
                newsError = "Haberler yüklenemedi. İnternet bağlantını kontrol et."
            } finally {
                newsLoading = false
            }
        }
    }

    fun toggleFollow(m: Match) {
        val s = followed.toMutableSet()
        if (!s.add(m.id)) s.remove(m.id)
        followed = s; prefs.followedMatches = s
        if (goalMode == GoalMode_OFF) updateGoalMode(1)
    }

    fun updateGoalMode(v: Int) { goalMode = v; prefs.goalMode = v }
    fun updateNotifyTransfer(v: Boolean) { notifyTransfer = v; prefs.notifyTransfer = v }
    fun updateNotifyFinal(v: Boolean) { notifyFinal = v; prefs.notifyFinal = v }
    fun updateSound(v: String) { sound = v; prefs.sound = v }

    fun toggleTeam(name: String) {
        val s = favoriteTeams.toMutableSet()
        if (!s.add(name)) s.remove(name)
        favoriteTeams = s; prefs.favoriteTeams = s
    }

    fun toggleLeague(code: String) {
        val s = leagues.toMutableSet()
        if (!s.add(code)) s.remove(code)
        if (s.isEmpty()) return
        leagues = s; prefs.leagues = s
        if (selectedLeague != null && selectedLeague !in s) selectedLeague = null
        refreshScores()
    }

    fun refreshPermission() { notifPermission = Notifier.canNotify(getApplication()) }

    fun enabledLeagueList() = Leagues.all.filter { it.code in leagues }

    private companion object {
        const val GoalMode_OFF = 0
    }
}
