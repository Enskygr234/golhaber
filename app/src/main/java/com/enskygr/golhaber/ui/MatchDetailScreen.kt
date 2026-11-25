package com.enskygr.golhaber.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enskygr.golhaber.data.EventKind
import com.enskygr.golhaber.data.Lineup
import com.enskygr.golhaber.data.LineupPlayer
import com.enskygr.golhaber.data.Match
import com.enskygr.golhaber.data.MatchDetail
import com.enskygr.golhaber.data.MatchEvent
import com.enskygr.golhaber.data.MatchState
import com.enskygr.golhaber.data.StatLine
import com.enskygr.golhaber.ui.theme.GH
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MatchDetailScreen(vm: MainViewModel) {
    val m = vm.currentMatch ?: return
    BackHandler { vm.closeMatch() }
    var tab by rememberSaveable(m.id) { mutableIntStateOf(0) }
    val d = vm.detail

    Column(
        Modifier
            .fillMaxSize()
            .background(GH.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::closeMatch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = GH.Text)
            }
            Text(m.league.name, color = GH.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Header(m, d)

        Segmented(
            options = listOf("Olaylar", "İstatistik", "Kadro"),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            when {
                vm.detailLoading && d == null -> item { LoadingBlock() }
                d == null && vm.detailError != null && !(tab == 0 && fallbackEvents(m).isNotEmpty()) -> item {
                    CenterMessage("📡", "Bağlantı sorunu", vm.detailError ?: "", "Tekrar dene") { vm.retryDetail() }
                }
                tab == 0 -> eventsTab(m, d)
                tab == 1 -> statsTab(m, d)
                else -> lineupsTab(m, d)
            }
        }
    }
}

// ---------------------------------------------------------------- Üst bilgi

private val dayFmt = DateTimeFormatter.ofPattern("d MMMM EEEE · HH:mm", Locale("tr", "TR"))

@Composable
private fun Header(m: Match, d: MatchDetail?) {
    val homeScore = d?.homeScore ?: m.home.score
    val awayScore = d?.awayScore ?: m.away.score
    val status = when {
        m.state == MatchState.LIVE -> d?.statusText ?: m.statusText
        else -> m.statusText
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(GH.Surface)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamBlock(m.home.name, m.home.logo, Modifier.weight(1f))
            Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (m.state == MatchState.PRE || m.state == MatchState.CANCELLED) {
                    Text(m.statusText, color = if (m.state == MatchState.CANCELLED) GH.Gold else GH.Text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                } else {
                    Text("${homeScore ?: 0} - ${awayScore ?: 0}", color = GH.Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(4.dp))
                if (m.isLive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(7); HSpace(6)
                        Text(status, color = GH.Live, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (m.state == MatchState.POST) {
                    Text(when (status) { "MS" -> "Maç sonu"; "UZ" -> "Uzatmalar sonu"; "Pen." -> "Penaltılar sonu"; else -> status }, color = GH.Muted, fontSize = 13.sp)
                } else if (m.state == MatchState.PRE) {
                    Text("başlama saati", color = GH.Muted, fontSize = 12.sp)
                } else {
                    Text("ertelendi", color = GH.Muted, fontSize = 12.sp)
                }
            }
            TeamBlock(m.away.name, m.away.logo, Modifier.weight(1f))
        }
        val info = listOfNotNull(
            m.kickoff.atZone(ZoneId.systemDefault()).format(dayFmt).replaceFirstChar { it.titlecase(Locale("tr", "TR")) },
            d?.venue?.let { "🏟️ $it" },
            d?.referee?.let { "Hakem: $it" },
            d?.attendance?.let { "Seyirci: %,d".format(Locale("tr", "TR"), it) },
        )
        Spacer(Modifier.height(12.dp))
        info.forEach { Text(it, color = GH.Muted, fontSize = 12.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun TeamBlock(name: String, logo: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamLogo(logo, name, 52.dp)
        Spacer(Modifier.height(8.dp))
        Text(name, color = GH.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- Olaylar

/** Özet servisinden olay gelmezse skor listesindeki gol ve kırmızı kartlar gösterilir. */
private fun fallbackEvents(m: Match): List<MatchEvent> {
    val goals = m.goals.map {
        MatchEvent(
            kind = when { it.ownGoal -> EventKind.OWN_GOAL; it.penalty -> EventKind.PEN_GOAL; else -> EventKind.GOAL },
            minute = it.minute, home = it.home, player = it.player, second = null,
        )
    }
    val reds = m.redCards.map { MatchEvent(EventKind.RED, it.minute, it.home, it.player, null) }
    return (goals + reds).sortedBy { minuteValue(it.minute) }
}

private fun minuteValue(s: String): Int {
    val parts = s.replace("'", "").split("+").map { it.trim().toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } * 100 + parts.getOrElse(1) { 0 }
}

private fun androidx.compose.foundation.lazy.LazyListScope.eventsTab(m: Match, d: MatchDetail?) {
    val events = d?.events?.takeIf { it.isNotEmpty() } ?: fallbackEvents(m)
    if (events.isEmpty()) {
        item {
            CenterMessage(
                if (m.state == MatchState.PRE) "⏳" else "📝",
                if (m.state == MatchState.PRE) "Maç henüz başlamadı" else "Henüz olay yok",
                if (m.state == MatchState.PRE) "Goller, kartlar ve oyuncu değişiklikleri maç başlayınca burada görünür." else "Gol ya da kart olunca burada görünür.",
            )
        }
        return
    }
    items(events.size) { i -> EventRow(events[i]) }
}

@Composable
private fun EventRow(e: MatchEvent) {
    if (e.kind == EventKind.HALF) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = GH.Line)
            Text("  İLK YARI SONU  ", color = GH.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(Modifier.weight(1f), color = GH.Line)
        }
        return
    }
    val (icon, tag) = when (e.kind) {
        EventKind.GOAL -> "⚽" to null
        EventKind.PEN_GOAL -> "⚽" to "Penaltı"
        EventKind.OWN_GOAL -> "⚽" to "Kendi kalesine"
        EventKind.PEN_MISS -> "❌" to "Kaçan penaltı"
        EventKind.YELLOW -> "🟨" to null
        EventKind.RED -> "🟥" to null
        EventKind.SUB -> "🔁" to null
        EventKind.VAR -> "📺" to "VAR"
        else -> "•" to null
    }
    val isGoal = e.kind == EventKind.GOAL || e.kind == EventKind.PEN_GOAL || e.kind == EventKind.OWN_GOAL
    val alignEnd = e.home == false

    @Composable
    fun body() {
        Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(
                e.player,
                color = if (e.kind == EventKind.SUB) GH.Green else GH.Text,
                fontSize = 14.sp,
                fontWeight = if (isGoal) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                e.kind == EventKind.SUB && e.second != null -> e.second
                isGoal && e.second != null -> "Asist: ${e.second}"
                else -> tag
            }
            if (sub != null) {
                Text(
                    sub,
                    color = if (e.kind == EventKind.SUB) GH.Live.copy(alpha = 0.85f) else GH.Muted,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isGoal) GH.Green.copy(alpha = 0.08f) else GH.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!alignEnd) {
            MinuteBadge(e.minute); HSpace(10)
            Text(icon, fontSize = 16.sp); HSpace(10)
            Box(Modifier.weight(1f)) { body() }
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { body() }
            HSpace(10); Text(icon, fontSize = 16.sp)
            HSpace(10); MinuteBadge(e.minute)
        }
    }
}

@Composable
private fun MinuteBadge(minute: String) {
    Text(
        minute.ifBlank { "–" },
        Modifier.width(44.dp).clip(RoundedCornerShape(8.dp)).background(GH.Surface2).padding(vertical = 4.dp),
        color = GH.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
    )
}

// ---------------------------------------------------------------- İstatistik

private fun androidx.compose.foundation.lazy.LazyListScope.statsTab(m: Match, d: MatchDetail?) {
    val stats = d?.stats.orEmpty()
    if (stats.isEmpty()) {
        item {
            CenterMessage("📈", "İstatistik yok", if (m.state == MatchState.PRE) "İstatistikler maç başlayınca gelir." else "Bu maç için istatistik yayınlanmadı.")
        }
        return
    }
    item {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(m.home.logo, m.home.name, 22.dp)
            Spacer(Modifier.weight(1f))
            TeamLogo(m.away.logo, m.away.name, 22.dp)
        }
    }
    items(stats, key = { it.label }) { StatRow(it) }
}

@Composable
private fun StatRow(s: StatLine) {
    val total = s.homeVal + s.awayVal
    val hf = if (total > 0f) s.homeVal / total else 0.5f
    val homeLead = s.homeVal > s.awayVal
    val awayLead = s.awayVal > s.homeVal
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.home, color = if (homeLead) GH.Text else GH.Muted, fontWeight = if (homeLead) FontWeight.Black else FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.width(56.dp))
            Text(s.label, color = GH.Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(s.away, color = if (awayLead) GH.Text else GH.Muted, fontWeight = if (awayLead) FontWeight.Black else FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Bar(hf, if (homeLead) GH.Green else GH.Surface2, alignEnd = true, modifier = Modifier.weight(1f))
            Bar(1f - hf, if (awayLead) GH.Gold else GH.Surface2, alignEnd = false, modifier = Modifier.weight(1f))
        }
    }
}

/** Ortadan dışa doğru dolan çubuk: ev sahibi sağa, deplasman sola yaslı. */
@Composable
private fun Bar(fraction: Float, color: Color, alignEnd: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)).background(GH.Surface), contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
    }
}

// ---------------------------------------------------------------- Kadro

private fun androidx.compose.foundation.lazy.LazyListScope.lineupsTab(m: Match, d: MatchDetail?) {
    val h = d?.homeLineup
    val a = d?.awayLineup
    if (h == null && a == null) {
        item { CenterMessage("📋", "Kadro açıklanmadı", "İlk 11'ler genellikle maçtan yaklaşık bir saat önce açıklanır.") }
        return
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineupColumn(m.home.name, m.home.logo, h, Modifier.weight(1f))
            LineupColumn(m.away.name, m.away.logo, a, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LineupColumn(team: String, logo: String, l: Lineup?, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(GH.Surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(logo, team, 20.dp); HSpace(6)
            Text(l?.formation?.ifBlank { null } ?: "İlk 11", color = GH.Green, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        if (l == null) {
            Text("Açıklanmadı", color = GH.Muted, fontSize = 12.sp)
            return@Column
        }
        l.starters.forEach { PlayerLine(it, starter = true) }
        if (l.subs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Yedekler")
            l.subs.forEach { PlayerLine(it, starter = false) }
        }
    }
}

@Composable
private fun PlayerLine(p: LineupPlayer, starter: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            p.number.ifBlank { "·" },
            Modifier.width(22.dp),
            color = GH.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End,
        )
        HSpace(6)
        Text(
            p.name,
            Modifier.weight(1f),
            color = if (starter || p.subbedIn) GH.Text else GH.Muted,
            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        when {
            starter && p.subbedOut -> Text("▼", color = GH.Live, fontSize = 10.sp)
            !starter && p.subbedIn -> Text("▲", color = GH.Green, fontSize = 10.sp)
        }
    }
}
