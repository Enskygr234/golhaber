package com.enskygr.golhaber.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Gol bildirimi kapsamı. */
object GoalMode {
    const val OFF = 0
    const val FAVORITES = 1
    const val ALL = 2
}

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("golhaber", Context.MODE_PRIVATE)

    var goalMode: Int
        get() = sp.getInt("goalMode", GoalMode.FAVORITES)
        set(v) = sp.edit().putInt("goalMode", v).apply()

    var notifyTransfer: Boolean
        get() = sp.getBoolean("notifyTransfer", true)
        set(v) = sp.edit().putBoolean("notifyTransfer", v).apply()

    var notifyFinal: Boolean
        get() = sp.getBoolean("notifyFinal", true)
        set(v) = sp.edit().putBoolean("notifyFinal", v).apply()

    /** whistle | crowd | classic */
    var sound: String
        get() = sp.getString("sound", "whistle") ?: "whistle"
        set(v) = sp.edit().putString("sound", v).apply()

    var favoriteTeams: Set<String>
        get() = sp.getStringSet("favTeams", emptySet())?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("favTeams", v.toSet()).apply()

    var followedMatches: Set<String>
        get() = sp.getStringSet("followed", emptySet())?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("followed", v.toSet()).apply()

    var leagues: Set<String>
        get() = sp.getStringSet("leagues", null)?.toSet() ?: Leagues.all.map { it.code }.toSet()
        set(v) = sp.edit().putStringSet("leagues", v.toSet()).apply()

    /** maçId -> "ev-dep|DURUM" */
    var lastScores: Map<String, String>
        get() {
            val o = runCatching { JSONObject(sp.getString("lastScores", "{}") ?: "{}") }.getOrElse { JSONObject() }
            return o.keys().asSequence().associateWith { o.optString(it) }
        }
        set(v) = sp.edit().putString("lastScores", JSONObject(v).toString()).apply()

    var seenNews: List<String>
        get() {
            val a = runCatching { JSONArray(sp.getString("seenNews", "[]") ?: "[]") }.getOrElse { JSONArray() }
            return (0 until a.length()).map { a.optString(it) }
        }
        set(v) = sp.edit().putString("seenNews", JSONArray(v.takeLast(300)).toString()).apply()

    fun enabledLeagues(): List<League> = Leagues.all.filter { it.code in leagues }
}
