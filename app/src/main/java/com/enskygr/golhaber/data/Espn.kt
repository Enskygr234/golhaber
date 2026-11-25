package com.enskygr.golhaber.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** ESPN'in herkese açık futbol servisleri (anahtar gerekmez). */
object Espn {
    const val SITE = "https://site.api.espn.com/apis/site/v2/sports/soccer"
    const val STANDINGS = "https://site.api.espn.com/apis/v2/sports/soccer"
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONObject.logo(): String =
    optString("logo").ifBlank { optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty() }

private fun JSONObject?.athleteName(): String {
    if (this == null) return ""
    val a = optJSONObject("athlete") ?: this
    return a.optString("displayName").ifBlank { a.optString("shortName") }.ifBlank { a.optString("fullName") }
}

// =====================================================================
// Puan durumu
// =====================================================================

object StandingsRepository {

    suspend fun fetch(lg: League): List<StandingGroup> = withContext(Dispatchers.IO) {
        val json = JSONObject(Http.get("${Espn.STANDINGS}/${lg.code}/standings", maxAgeMs = 5 * 60_000))
        val groups = json.optJSONArray("children").objects()
        val list = if (groups.isNotEmpty()) {
            groups.map { g -> StandingGroup(trGroupName(g.optString("name")), parseEntries(g.optJSONObject("standings"))) }
        } else {
            listOf(StandingGroup(lg.name, parseEntries(json.optJSONObject("standings"))))
        }
        list.filter { it.rows.isNotEmpty() }
    }

    private fun parseEntries(standings: JSONObject?): List<StandingRow> {
        val entries = standings?.optJSONArray("entries").objects()
        val rows = entries.mapNotNull { e ->
            val t = e.optJSONObject("team") ?: return@mapNotNull null
            val st = HashMap<String, Double>()
            e.optJSONArray("stats").objects().forEach { s ->
                val v = s.optDouble("value", Double.NaN)
                if (!v.isNaN()) {
                    s.optString("name").takeIf { it.isNotBlank() }?.let { st[it] = v }
                    s.optString("abbreviation").takeIf { it.isNotBlank() }?.let { st.putIfAbsent("@$it", v) }
                }
            }
            fun n(name: String, abbr: String) = (st[name] ?: st["@$abbr"] ?: 0.0).toInt()
            val note = e.optJSONObject("note")
            val name = t.optString("displayName").ifBlank { t.optString("name") }
            StandingRow(
                rank = n("rank", "R"),
                teamId = t.optString("id"),
                team = name,
                shortName = t.optString("shortDisplayName").ifBlank { name },
                logo = t.logo(),
                played = n("gamesPlayed", "GP"),
                won = n("wins", "W"),
                drawn = n("ties", "D"),
                lost = n("losses", "L"),
                goalsFor = n("pointsFor", "F"),
                goalsAgainst = n("pointsAgainst", "A"),
                points = n("points", "P"),
                noteColor = note?.optString("color")?.ifBlank { null },
                note = note?.optString("description")?.ifBlank { null }?.let { trNote(it) },
            )
        }
        // ESPN sıralamayı her zaman sıralı göndermiyor; rank yoksa puan/averaj/atılan gole göre sırala.
        val sorted = rows.sortedWith(
            compareBy<StandingRow> { if (it.rank > 0) it.rank else Int.MAX_VALUE }
                .thenByDescending { it.points }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsFor }
        )
        return sorted.mapIndexed { i, r -> if (r.rank > 0) r else r.copy(rank = i + 1) }
    }

    private fun trGroupName(s: String): String = when {
        s.isBlank() -> ""
        s.startsWith("Group ", true) -> s.replaceFirst("Group", "Grup", true)
        s.contains("League Phase", true) || s.contains("League Stage", true) -> "Lig Aşaması"
        else -> s.replace(Regex("\\b\\d{4}(-\\d{2,4})?\\b"), "").trim()
    }

    fun trNote(s: String): String {
        val l = s.lowercase()
        return when {
            l.contains("relegation") && l.contains("playoff") -> "Düşme play-off'u"
            l.contains("relegation") -> "Küme düşme"
            l.contains("champions league") && l.contains("qual") -> "Şampiyonlar Ligi eleme"
            l.contains("champions league") -> "Şampiyonlar Ligi"
            l.contains("conference league") -> "Konferans Ligi"
            l.contains("europa league") && l.contains("qual") -> "Avrupa Ligi eleme"
            l.contains("europa league") -> "Avrupa Ligi"
            l.contains("round of 16") || l.contains("knockout") && !l.contains("playoff") -> "Son 16"
            l.contains("playoff") || l.contains("play-off") -> "Play-off"
            l.contains("promotion") -> "Üst lige çıkış"
            l.contains("champion") -> "Şampiyon"
            l.contains("eliminated") -> "Elendi"
            else -> s
        }
    }
}

// =====================================================================
// Maç detayı (özet uç noktası)
// =====================================================================

object DetailRepository {

    suspend fun fetch(lg: League, eventId: String, live: Boolean): MatchDetail = withContext(Dispatchers.IO) {
        val json = JSONObject(Http.get("${Espn.SITE}/${lg.code}/summary?event=$eventId", maxAgeMs = if (live) 0 else 60_000))
        parse(json)
    }

    private fun parse(j: JSONObject): MatchDetail {
        val comp = j.optJSONObject("header")?.optJSONArray("competitions")?.optJSONObject(0)
        val competitors = comp?.optJSONArray("competitors").objects()
        val homeC = competitors.firstOrNull { it.optString("homeAway") == "home" }
        val awayC = competitors.firstOrNull { it.optString("homeAway") == "away" }
        val homeId = homeC?.optJSONObject("team")?.optString("id")
            ?: competitors.getOrNull(0)?.optJSONObject("team")?.optString("id").orEmpty()

        val status = comp?.optJSONObject("status")
        val stType = status?.optJSONObject("type")
        val statusText = when (stType?.optString("state")) {
            "in" -> if (stType?.optString("name") == "STATUS_HALFTIME") "İY" else status?.optString("displayClock")?.ifBlank { null }
            "post" -> "MS"
            else -> null
        }

        val events = j.optJSONArray("keyEvents").objects().mapNotNull { parseEvent(it, homeId) }

        val stats = parseStats(j.optJSONObject("boxscore")?.optJSONArray("teams").objects(), homeId)

        var homeL: Lineup? = null
        var awayL: Lineup? = null
        j.optJSONArray("rosters").objects().forEach { r ->
            val lineup = parseLineup(r) ?: return@forEach
            val isHome = r.optString("homeAway").let { if (it.isBlank()) r.optJSONObject("team")?.optString("id") == homeId else it == "home" }
            if (isHome) homeL = lineup else awayL = lineup
        }

        val info = j.optJSONObject("gameInfo")
        val venue = info?.optJSONObject("venue")?.let { v ->
            val name = v.optString("fullName").ifBlank { v.optString("shortName") }
            val city = v.optJSONObject("address")?.optString("city").orEmpty()
            listOf(name, city).filter { it.isNotBlank() }.distinct().joinToString(", ")
        }?.ifBlank { null }
        val referee = info?.optJSONArray("officials").objects().firstOrNull()?.let {
            it.optString("displayName").ifBlank { it.optString("fullName") }
        }?.ifBlank { null }
        val attendance = info?.optInt("attendance", 0)?.takeIf { it > 0 }

        return MatchDetail(
            events = events,
            stats = stats,
            homeLineup = homeL,
            awayLineup = awayL,
            venue = venue,
            referee = referee,
            attendance = attendance,
            homeScore = homeC?.optString("score")?.toIntOrNull(),
            awayScore = awayC?.optString("score")?.toIntOrNull(),
            statusText = statusText,
        )
    }

    private fun parseEvent(e: JSONObject, homeId: String): MatchEvent? {
        val type = e.optJSONObject("type")
        val key = (type?.optString("type").orEmpty() + " " + type?.optString("text").orEmpty()).lowercase()
        val minute = e.optJSONObject("clock")?.optString("displayValue").orEmpty()
        val teamId = e.optJSONObject("team")?.optString("id")
        val home = teamId?.takeIf { it.isNotBlank() }?.let { it == homeId }
        val people = e.optJSONArray("participants").objects().map { it.athleteName() }.filter { it.isNotBlank() }
        val text = e.optString("text")

        val kind = when {
            Regex("\\bvar\\b").containsMatchIn(key) -> EventKind.VAR
            key.contains("own") && key.contains("goal") -> EventKind.OWN_GOAL
            key.contains("penalty") && (key.contains("miss") || key.contains("saved")) -> EventKind.PEN_MISS
            key.contains("penalty") && (key.contains("scored") || key.contains("goal")) -> EventKind.PEN_GOAL
            key.contains("goal") || e.optBoolean("scoringPlay") -> EventKind.GOAL
            Regex("\\bred\\b").containsMatchIn(key) -> EventKind.RED
            key.contains("yellow") -> EventKind.YELLOW
            key.contains("substitution") -> EventKind.SUB
            key.contains("halftime") || key.contains("half time") -> EventKind.HALF
            else -> return null // başlama, bitiş vb. gösterilmez
        }

        if (kind == EventKind.HALF) return MatchEvent(kind, minute, null, "Devre arası", null)

        if (kind == EventKind.SUB) {
            // Metin "Substitution, Takım. A replaces B." biçiminde: giren A, çıkan B.
            val idx = text.indexOf(" replaces ", ignoreCase = true)
            var inP = people.getOrNull(0).orEmpty()
            var outP = people.getOrNull(1)
            if (idx > 0) {
                val before = text.substring(0, idx)
                val after = text.substring(idx + 10)
                val inMatch = people.firstOrNull { before.contains(it) }
                val outMatch = people.firstOrNull { after.contains(it) }
                if (inMatch != null) inP = inMatch
                if (outMatch != null) outP = outMatch
                if (people.isEmpty()) {
                    inP = before.substringAfterLast(". ").trim()
                    outP = after.trim().removeSuffix(".")
                }
            }
            if (inP.isBlank()) return null
            return MatchEvent(kind, minute, home, inP, outP?.takeIf { it != inP })
        }

        val player = people.firstOrNull() ?: e.optString("shortText").ifBlank { return null }
        val second = if (kind == EventKind.GOAL || kind == EventKind.PEN_GOAL) people.getOrNull(1) else null
        return MatchEvent(kind, minute, home, player, second)
    }

    /** İstenen sırada ve Türkçe etiketle gösterilecek istatistikler. */
    private val statOrder = listOf(
        "possessionPct" to "Topla oynama",
        "totalShots" to "Şut",
        "shotsOnTarget" to "İsabetli şut",
        "blockedShots" to "Engellenen şut",
        "wonCorners" to "Korner",
        "offsides" to "Ofsayt",
        "foulsCommitted" to "Faul",
        "yellowCards" to "Sarı kart",
        "redCards" to "Kırmızı kart",
        "saves" to "Kurtarış",
        "totalPasses" to "Pas",
        "accuratePasses" to "İsabetli pas",
        "passPct" to "Pas isabeti",
        "totalCrosses" to "Orta",
        "totalTackles" to "Top kapma",
        "interceptions" to "Araya girme",
        "totalClearance" to "Uzaklaştırma",
    )

    private fun parseStats(teams: List<JSONObject>, homeId: String): List<StatLine> {
        if (teams.size < 2) return emptyList()
        val homeT = teams.firstOrNull { it.optString("homeAway") == "home" }
            ?: teams.firstOrNull { it.optJSONObject("team")?.optString("id") == homeId } ?: teams[0]
        val awayT = teams.first { it !== homeT }

        fun map(t: JSONObject): Map<String, Pair<String, Float>> =
            t.optJSONArray("statistics").objects().associate { s ->
                val shown = s.optString("displayValue")
                val num = shown.removeSuffix("%").replace(',', '.').toFloatOrNull() ?: s.optDouble("value", 0.0).toFloat()
                s.optString("name") to (shown to num)
            }

        val h = map(homeT)
        val a = map(awayT)
        return statOrder.mapNotNull { (key, label) ->
            val hv = h[key] ?: return@mapNotNull null
            val av = a[key] ?: return@mapNotNull null
            val pct = key.endsWith("Pct")
            fun fmt(p: Pair<String, Float>): Pair<String, Float> {
                if (!pct) return p.first.ifBlank { p.second.toInt().toString() } to p.second
                // ESPN bazen 0.85, bazen 85 gönderir.
                val v = if (p.second <= 1f && p.second > 0f) p.second * 100f else p.second
                return "${Math.round(v)}%" to v
            }
            val (hs, hn) = fmt(hv)
            val (asx, an) = fmt(av)
            if (hn == 0f && an == 0f && key != "redCards" && key != "yellowCards") return@mapNotNull null
            StatLine(label, hs, asx, hn, an)
        }
    }

    private fun parseLineup(r: JSONObject): Lineup? {
        val players = r.optJSONArray("roster").objects().map { p ->
            p.optBoolean("starter") to LineupPlayer(
                number = p.optString("jersey"),
                name = p.athleteName(),
                position = p.optJSONObject("position")?.optString("abbreviation").orEmpty(),
                subbedIn = p.optBoolean("subbedIn"),
                subbedOut = p.optBoolean("subbedOut"),
            )
        }.filter { it.second.name.isNotBlank() }
        if (players.isEmpty()) return null
        return Lineup(
            formation = r.optString("formation"),
            starters = players.filter { it.first }.map { it.second },
            subs = players.filterNot { it.first }.map { it.second },
        )
    }
}

// =====================================================================
// Gol krallığı
// =====================================================================

object LeadersRepository {
    private val dateFmt = DateTimeFormatter.BASIC_ISO_DATE

    suspend fun fetch(lg: League): Leaders = withContext(Dispatchers.IO) {
        runCatching { official(lg) }.getOrNull()?.takeIf { it.scorers.isNotEmpty() } ?: computed(lg)
    }

    /** ESPN istatistik servisi: gol ve asist liderleri. */
    private fun official(lg: League): Leaders {
        val json = JSONObject(Http.get("${Espn.SITE}/${lg.code}/statistics", maxAgeMs = 10 * 60_000))
        val cats = (json.optJSONArray("stats") ?: json.optJSONArray("categories")
            ?: json.optJSONArray("leaders")).objects()
        fun find(word: String) = cats.firstOrNull { it.optString("name").lowercase().contains(word) }
        fun leaders(c: JSONObject?, isGoals: Boolean): List<Scorer> =
            c?.optJSONArray("leaders").objects().mapNotNull { l ->
                val a = l.optJSONObject("athlete") ?: return@mapNotNull null
                val team = a.optJSONObject("team") ?: l.optJSONObject("team")
                val value = l.optDouble("value", Double.NaN).takeIf { !it.isNaN() }?.toInt()
                    ?: Regex("(Goals|Assists):\\s*(\\d+)").find(l.optString("displayValue"))?.groupValues?.get(2)?.toIntOrNull()
                    ?: return@mapNotNull null
                Scorer(
                    name = a.optString("displayName").ifBlank { a.optString("shortName") },
                    team = team?.let { t -> t.optString("displayName").ifBlank { t.optString("name") } }.orEmpty(),
                    teamLogo = team?.logo().orEmpty(),
                    goals = value,
                    assists = if (isGoals) null else value,
                    penalties = null,
                ).takeIf { it.name.isNotBlank() }
            }.orEmpty()
        return Leaders(
            scorers = leaders(find("goal"), true),
            assisters = leaders(find("assist"), false),
            official = true,
        )
    }

    /**
     * Yedek yol: sezonun bütün maç sonuçlarını tek istekte çekip golleri sayar.
     * Asist bilgisi bu kaynakta yok.
     */
    private fun computed(lg: League): Leaders {
        val today = LocalDate.now()
        val seasonStart = if (today.monthValue >= 7) LocalDate.of(today.year, 7, 1) else LocalDate.of(today.year - 1, 7, 1)
        val url = "${Espn.SITE}/${lg.code}/scoreboard?dates=${seasonStart.format(dateFmt)}-${today.format(dateFmt)}&limit=1000"
        val json = JSONObject(Http.get(url, maxAgeMs = 10 * 60_000))

        data class Acc(var goals: Int = 0, var pens: Int = 0, var team: String = "", var logo: String = "")
        val acc = LinkedHashMap<String, Acc>()
        json.optJSONArray("events").objects().forEach { ev ->
            val m = runCatching { ScoreRepository.parseEvent(ev, lg) }.getOrNull() ?: return@forEach
            if (m.state == MatchState.PRE || m.state == MatchState.CANCELLED) return@forEach
            m.goals.filter { !it.ownGoal && it.player.isNotBlank() }.forEach { g ->
                val side = if (g.home) m.home else m.away
                val a = acc.getOrPut("${g.player}|${side.id}") { Acc(team = side.name, logo = side.logo) }
                a.goals++
                if (g.penalty) a.pens++
            }
        }
        val scorers = acc.map { (k, a) ->
            Scorer(k.substringBefore('|'), a.team, a.logo, a.goals, null, a.pens)
        }.sortedWith(compareByDescending<Scorer> { it.goals }.thenBy { it.penalties ?: 0 }.thenBy { it.name })
            .take(50)
        return Leaders(scorers, emptyList(), official = false)
    }
}
