package com.enskygr.golhaber.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Http {
    fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) GolHaber/1.0")
        c.setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

/** Takım adlarını karşılaştırmak için: küçük harf, aksansız. */
fun String.norm(): String {
    val lower = this.lowercase().replace('ı', 'i')
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()
}

fun teamMatches(teamName: String, favorites: Collection<String>): Boolean {
    val n = teamName.norm()
    return favorites.any { f ->
        val fn = f.norm()
        fn.isNotEmpty() && (n == fn || n.contains(fn) || fn.contains(n))
    }
}

object ScoreRepository {
    private val dateFmt = DateTimeFormatter.BASIC_ISO_DATE
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun fetch(leagues: List<League>, date: LocalDate): List<Match> = coroutineScope {
        val results = leagues.map { lg ->
            async(Dispatchers.IO) { runCatching { fetchLeague(lg, date) } }
        }.awaitAll()
        if (results.isNotEmpty() && results.all { it.isFailure }) {
            throw results.first().exceptionOrNull() ?: IOException("Bağlantı hatası")
        }
        results.flatMap { it.getOrDefault(emptyList()) }
            .sortedWith(compareBy({ Leagues.all.indexOf(it.league) }, { it.kickoff }))
    }

    private fun fetchLeague(lg: League, date: LocalDate): List<Match> {
        val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/${lg.code}/scoreboard?dates=${date.format(dateFmt)}"
        val json = JSONObject(Http.get(url))
        val events = json.optJSONArray("events") ?: return emptyList()
        return (0 until events.length()).mapNotNull { i ->
            runCatching { parseEvent(events.getJSONObject(i), lg) }.getOrNull()
        }
    }

    private fun parseInstant(s: String): Instant {
        return try {
            OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (e: Exception) {
            OffsetDateTime.parse(s.replace("Z", ":00Z"), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        }
    }

    private fun parseEvent(e: JSONObject, lg: League): Match? {
        val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: return null
        val comps = comp.optJSONArray("competitors") ?: return null
        var home: TeamSide? = null
        var away: TeamSide? = null
        for (j in 0 until comps.length()) {
            val c = comps.getJSONObject(j)
            val t = c.optJSONObject("team") ?: JSONObject()
            val name = t.optString("displayName").ifBlank { t.optString("name") }
            val side = TeamSide(
                id = t.optString("id"),
                name = name,
                shortName = t.optString("shortDisplayName").ifBlank { name },
                logo = t.optString("logo"),
                score = c.optString("score").toIntOrNull(),
            )
            if (c.optString("homeAway") == "home") home = side else away = side
        }
        val homeSide: TeamSide = home ?: return null
        val awaySide: TeamSide = away ?: return null

        val status = e.optJSONObject("status") ?: comp.optJSONObject("status")
        val type = status?.optJSONObject("type")
        val stateStr = type?.optString("state").orEmpty()
        val name = type?.optString("name").orEmpty()
        val state = when {
            name.contains("POSTPONED") || name.contains("CANCELED") || name.contains("ABANDONED") -> MatchState.CANCELLED
            stateStr == "in" -> MatchState.LIVE
            stateStr == "post" -> MatchState.POST
            else -> MatchState.PRE
        }
        val kickoff = parseInstant(e.optString("date"))
        val statusText = when (state) {
            MatchState.PRE -> kickoff.atZone(ZoneId.systemDefault()).format(timeFmt)
            MatchState.LIVE -> when {
                name == "STATUS_HALFTIME" -> "İY"
                else -> status?.optString("displayClock").orEmpty().ifBlank { "Canlı" }
            }
            MatchState.POST -> when {
                name.contains("PEN") -> "Pen."
                name.contains("AET") || name.contains("EXTRA") -> "UZ"
                else -> "MS"
            }
            MatchState.CANCELLED -> "ERT"
        }

        val goals = mutableListOf<Goal>()
        comp.optJSONArray("details")?.let { details ->
            for (k in 0 until details.length()) {
                val d = details.optJSONObject(k) ?: continue
                if (!d.optBoolean("scoringPlay")) continue
                val teamId = d.optJSONObject("team")?.optString("id").orEmpty()
                goals += Goal(
                    minute = d.optJSONObject("clock")?.optString("displayValue").orEmpty(),
                    player = d.optJSONArray("athletesInvolved")?.optJSONObject(0)?.optString("shortName")
                        ?.ifBlank { null }
                        ?: d.optJSONArray("athletesInvolved")?.optJSONObject(0)?.optString("displayName").orEmpty(),
                    home = teamId == homeSide.id,
                    penalty = d.optBoolean("penaltyKick"),
                    ownGoal = d.optBoolean("ownGoal"),
                )
            }
        }

        return Match(
            id = e.optString("id"),
            league = lg,
            kickoff = kickoff,
            state = state,
            statusText = statusText,
            home = homeSide,
            away = awaySide,
            goals = goals,
        )
    }
}

object NewsRepository {
    private fun url(query: String): String =
        "https://news.google.com/rss/search?q=" + URLEncoder.encode("$query when:7d", "UTF-8") +
            "&hl=tr&gl=TR&ceid=TR:tr"

    suspend fun fetch(query: String): List<NewsItem> = withContext(Dispatchers.IO) {
        parse(Http.get(url(query)))
            .distinctBy { it.title.norm() }
            .sortedByDescending { it.published ?: Instant.EPOCH }
    }

    private fun parse(xml: String): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        val out = mutableListOf<NewsItem>()
        var inItem = false
        var tag = ""
        val fields = HashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name
                    if (tag == "item") {
                        inItem = true
                        fields.clear()
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inItem && tag.isNotEmpty()) {
                    fields[tag] = (fields[tag] ?: "") + parser.text
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        inItem = false
                        toItem(fields)?.let { out += it }
                    }
                    tag = ""
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun toItem(f: Map<String, String>): NewsItem? {
        val rawTitle = f["title"]?.trim().orEmpty()
        val link = f["link"]?.trim().orEmpty()
        if (rawTitle.isEmpty() || link.isEmpty()) return null
        var source = f["source"]?.trim().orEmpty()
        var title = rawTitle
        val dash = rawTitle.lastIndexOf(" - ")
        if (dash > 0) {
            if (source.isEmpty()) source = rawTitle.substring(dash + 3).trim()
            title = rawTitle.substring(0, dash).trim()
        }
        val published = f["pubDate"]?.trim()?.let {
            runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
        }
        return NewsItem(title = title, source = source.ifEmpty { "Haber" }, link = link, published = published)
    }
}
