package com.enskygr.golhaber.data

import java.time.Instant

/** [defaultOn] = ilk kurulumda açık gelsin mi. */
data class League(val code: String, val name: String, val short: String, val defaultOn: Boolean = true)

object Leagues {
    val all = listOf(
        League("tur.1", "Süper Lig", "Süper Lig"),
        League("tur.2", "TFF 1. Lig", "1. Lig", defaultOn = false),
        League("eng.1", "Premier Lig", "Premier Lig"),
        League("esp.1", "La Liga", "La Liga"),
        League("ita.1", "Serie A", "Serie A"),
        League("ger.1", "Bundesliga", "Bundesliga"),
        League("fra.1", "Ligue 1", "Ligue 1", defaultOn = false),
        League("ned.1", "Eredivisie", "Eredivisie", defaultOn = false),
        League("por.1", "Liga Portugal", "Portekiz", defaultOn = false),
        League("uefa.champions", "UEFA Şampiyonlar Ligi", "Ş. Ligi"),
        League("uefa.europa", "UEFA Avrupa Ligi", "Avrupa Ligi", defaultOn = false),
        League("uefa.europa.conf", "UEFA Konferans Ligi", "Konferans", defaultOn = false),
    )

    /** İlk kurulumda açık gelen ligler (eski sürümün altı ligi). */
    val defaults: Set<String> get() = all.filter { it.defaultOn }.map { it.code }.toSet()

    fun byCode(code: String): League? = all.firstOrNull { it.code == code }
}

enum class MatchState { PRE, LIVE, POST, CANCELLED }

data class TeamSide(
    val id: String,
    val name: String,
    val shortName: String,
    val logo: String,
    val score: Int?,
)

data class Goal(
    val minute: String,
    val player: String,
    val home: Boolean,
    val penalty: Boolean,
    val ownGoal: Boolean,
)

data class Match(
    val id: String,
    val league: League,
    val kickoff: Instant,
    val state: MatchState,
    val statusText: String,
    val home: TeamSide,
    val away: TeamSide,
    val goals: List<Goal>,
    /** Kırmızı kartlar (ikinci sarıdan gelenler dahil). */
    val redCards: List<CardEvent> = emptyList(),
) {
    val scoreText: String get() = "${home.score ?: 0}-${away.score ?: 0}"
    val isLive: Boolean get() = state == MatchState.LIVE
}

data class CardEvent(val minute: String, val player: String, val home: Boolean)

// ---------------- Puan durumu

data class StandingRow(
    val rank: Int,
    val teamId: String,
    val team: String,
    val shortName: String,
    val logo: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int,
    /** ESPN'in renk notu (#RRGGBB), ör. Şampiyonlar Ligi ya da küme düşme bölgesi. */
    val noteColor: String?,
    val note: String?,
) {
    val goalDiff: Int get() = goalsFor - goalsAgainst
}

data class StandingGroup(val name: String, val rows: List<StandingRow>)

// ---------------- Gol krallığı

data class Scorer(
    val name: String,
    val team: String,
    val teamLogo: String,
    val goals: Int,
    val assists: Int?,
    val penalties: Int?,
)

data class Leaders(
    val scorers: List<Scorer>,
    val assisters: List<Scorer>,
    /** true: ESPN istatistik servisinden; false: sezonun maç sonuçlarından hesaplandı. */
    val official: Boolean,
)

// ---------------- Maç detayı

enum class EventKind { GOAL, OWN_GOAL, PEN_GOAL, PEN_MISS, YELLOW, RED, SUB, HALF, VAR, OTHER }

data class MatchEvent(
    val kind: EventKind,
    val minute: String,
    val home: Boolean?,
    val player: String,
    /** Değişiklikte oyundan çıkan, golde asist yapan oyuncu. */
    val second: String?,
)

data class StatLine(val label: String, val home: String, val away: String, val homeVal: Float, val awayVal: Float)

data class LineupPlayer(val number: String, val name: String, val position: String, val subbedIn: Boolean, val subbedOut: Boolean)

data class Lineup(val formation: String, val starters: List<LineupPlayer>, val subs: List<LineupPlayer>)

data class MatchDetail(
    val events: List<MatchEvent>,
    val stats: List<StatLine>,
    val homeLineup: Lineup?,
    val awayLineup: Lineup?,
    val venue: String?,
    val referee: String?,
    val attendance: Int?,
    /** Özet uç noktası skoru da döner; canlı maçta liste yenilenmeden önce güncel kalır. */
    val homeScore: Int?,
    val awayScore: Int?,
    val statusText: String?,
)

data class NewsItem(
    val title: String,
    val source: String,
    val link: String,
    val published: Instant?,
)

data class NewsTopic(val label: String, val query: String)

object NewsTopics {
    val all = listOf(
        NewsTopic("Gündem", "futbol transfer"),
        NewsTopic("Galatasaray", "Galatasaray transfer"),
        NewsTopic("Fenerbahçe", "Fenerbahçe transfer"),
        NewsTopic("Beşiktaş", "Beşiktaş transfer"),
        NewsTopic("Trabzonspor", "Trabzonspor transfer"),
        NewsTopic("Süper Lig", "Süper Lig transfer"),
        NewsTopic("Premier Lig", "Premier Lig transfer"),
        NewsTopic("La Liga", "La Liga transfer"),
        NewsTopic("Serie A", "Serie A transfer"),
        NewsTopic("Bundesliga", "Bundesliga transfer"),
    )
}

object PopularTeams {
    val all = listOf(
        "Galatasaray", "Fenerbahçe", "Beşiktaş", "Trabzonspor", "Başakşehir", "Samsunspor", "Göztepe",
        "Real Madrid", "Barcelona", "Atlético Madrid",
        "Manchester City", "Liverpool", "Arsenal", "Manchester United", "Chelsea", "Tottenham Hotspur",
        "Bayern Munich", "Borussia Dortmund", "Bayer Leverkusen",
        "Inter Milan", "AC Milan", "Juventus", "Napoli",
    )
}
