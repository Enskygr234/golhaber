package com.enskygr.golhaber.data

import java.time.Instant

data class League(val code: String, val name: String, val short: String)

object Leagues {
    val all = listOf(
        League("tur.1", "Süper Lig", "Süper Lig"),
        League("eng.1", "Premier Lig", "Premier Lig"),
        League("esp.1", "La Liga", "La Liga"),
        League("ita.1", "Serie A", "Serie A"),
        League("ger.1", "Bundesliga", "Bundesliga"),
        League("uefa.champions", "UEFA Şampiyonlar Ligi", "Ş. Ligi"),
    )

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
) {
    val scoreText: String get() = "${home.score ?: 0}-${away.score ?: 0}"
    val isLive: Boolean get() = state == MatchState.LIVE
}

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
