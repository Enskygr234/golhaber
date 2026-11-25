package com.enskygr.golhaber.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enskygr.golhaber.data.DetailRepository
import com.enskygr.golhaber.data.League
import com.enskygr.golhaber.data.Leaders
import com.enskygr.golhaber.data.LeadersRepository
import com.enskygr.golhaber.data.Leagues
import com.enskygr.golhaber.data.MatchDetail
import com.enskygr.golhaber.data.StandingGroup
import com.enskygr.golhaber.data.StandingsRepository
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

    // --- Puan durumu & gol krallığı
    var tableLeague by mutableStateOf(Leagues.byCode(prefs.tableLeague) ?: Leagues.all.first())
        private set
    /** 0 = puan tablosu, 1 = gol krallığı */
    var tableMode by mutableIntStateOf(0)
        private set
    var standings by mutableStateOf<List<StandingGroup>>(emptyList())
        private set
    var standingsLoading by mutableStateOf(false)
        private set
    var standingsError by mutableStateOf<String?>(null)
        private set
    var leaders by mutableStateOf<Leaders?>(null)
        private set
    var leadersLoading by mutableStateOf(false)
        private set
    var leadersError by mutableStateOf<String?>(null)
        private set

    // --- Maç detayı
    var openMatchId by mutableStateOf<String?>(null)
        private set
    private var openMatchSnapshot: Match? = null
    var detail by mutableStateOf<MatchDetail?>(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    var detailError by mutableStateOf<String?>(null)
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
    private var standingsJob: Job? = null
    private var leadersJob: Job? = null
    private var detailJob: Job? = null

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

    // ---------------- Puan durumu

    /** Puan sekmesi ilk açıldığında ya da lig/mod değişince çağrılır. */
    fun ensureTable() {
        if (tableMode == 0 && standings.isEmpty() && !standingsLoading) refreshStandings()
        if (tableMode == 1 && leaders == null && !leadersLoading) refreshLeaders()
    }

    fun selectTableLeague(lg: League) {
        if (lg == tableLeague) return
        tableLeague = lg
        prefs.tableLeague = lg.code
        standings = emptyList(); standingsError = null
        leaders = null; leadersError = null
        standingsJob?.cancel(); leadersJob?.cancel()
        standingsLoading = false; leadersLoading = false
        ensureTable()
    }

    fun selectTableMode(mode: Int) {
        tableMode = mode
        ensureTable()
    }

    fun refreshStandings() {
        standingsJob?.cancel()
        val lg = tableLeague
        standingsJob = viewModelScope.launch {
            standingsLoading = true
            try {
                standings = StandingsRepository.fetch(lg)
                standingsError = if (standings.isEmpty()) "Bu lig için puan durumu henüz yayınlanmadı." else null
            } catch (e: Exception) {
                standingsError = "Puan durumu yüklenemedi. İnternet bağlantını kontrol et."
            } finally {
                standingsLoading = false
            }
        }
    }

    fun refreshLeaders() {
        leadersJob?.cancel()
        val lg = tableLeague
        leadersJob = viewModelScope.launch {
            leadersLoading = true
            try {
                val l = LeadersRepository.fetch(lg)
                leaders = l
                leadersError = if (l.scorers.isEmpty()) "Bu sezon henüz gol listesi oluşmadı." else null
            } catch (e: Exception) {
                leadersError = "Gol krallığı yüklenemedi. İnternet bağlantını kontrol et."
            } finally {
                leadersLoading = false
            }
        }
    }

    // ---------------- Maç detayı

    /** Açık maçın en güncel hali: skor listesi yenilendikçe değişir. */
    val currentMatch: Match?
        get() {
            val id = openMatchId ?: return null
            return matches.firstOrNull { it.id == id } ?: openMatchSnapshot
        }

    fun openMatch(m: Match) {
        openMatchId = m.id
        openMatchSnapshot = m
        detail = null
        detailError = null
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            var first = true
            while (true) {
                val cur = currentMatch ?: break
                if (first) detailLoading = true
                try {
                    detail = DetailRepository.fetch(cur.league, cur.id, live = cur.isLive)
                    detailError = null
                } catch (e: Exception) {
                    if (first) detailError = "Maç ayrıntıları yüklenemedi."
                } finally {
                    detailLoading = false
                }
                first = false
                if (!cur.isLive) break
                delay(30_000)
            }
        }
    }

    fun closeMatch() {
        detailJob?.cancel()
        openMatchId = null
        openMatchSnapshot = null
        detail = null
    }

    fun retryDetail() { currentMatch?.let { openMatch(it) } }

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
