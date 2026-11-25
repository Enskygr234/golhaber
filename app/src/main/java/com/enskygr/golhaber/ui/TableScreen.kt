package com.enskygr.golhaber.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.enskygr.golhaber.data.Leagues
import com.enskygr.golhaber.data.Scorer
import com.enskygr.golhaber.data.StandingGroup
import com.enskygr.golhaber.data.StandingRow
import com.enskygr.golhaber.data.teamMatches
import com.enskygr.golhaber.ui.theme.GH

fun noteColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val h = if (hex.startsWith("#")) hex else "#$hex"
    return runCatching { Color(android.graphics.Color.parseColor(h)) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.ensureTable() }
    Column(Modifier.fillMaxSize()) {
        Segmented(
            options = listOf("Puan Durumu", "Gol Krallığı"),
            selected = vm.tableMode,
            onSelect = vm::selectTableMode,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Leagues.all, key = { it.code }) { lg ->
                Chip(lg.short, vm.tableLeague == lg, { vm.selectTableLeague(lg) })
            }
        }
        val loading = if (vm.tableMode == 0) vm.standingsLoading else vm.leadersLoading
        val hasData = if (vm.tableMode == 0) vm.standings.isNotEmpty() else vm.leaders?.scorers?.isNotEmpty() == true
        PullToRefreshBox(
            isRefreshing = loading && hasData,
            onRefresh = { if (vm.tableMode == 0) vm.refreshStandings() else vm.refreshLeaders() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (vm.tableMode == 0) StandingsList(vm) else ScorersList(vm)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GH.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) GH.Green else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                color = if (sel) GH.Bg else GH.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------- Puan tablosu

@Composable
private fun StandingsList(vm: MainViewModel) {
    val groups = vm.standings
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    ) {
        when {
            vm.standingsLoading && groups.isEmpty() -> item { LoadingBlock() }
            groups.isEmpty() -> item {
                CenterMessage("📊", "Puan durumu yok", vm.standingsError ?: "Veri bulunamadı.", "Tekrar dene") { vm.refreshStandings() }
            }
            else -> {
                groups.forEachIndexed { gi, g ->
                    item(key = "g_$gi") { GroupTable(g, vm.favoriteTeams, showTitle = groups.size > 1) }
                }
                item(key = "legend") { Legend(groups) }
            }
        }
    }
}

@Composable
private fun GroupTable(g: StandingGroup, favorites: Set<String>, showTitle: Boolean) {
    Column(Modifier.padding(top = 8.dp)) {
        if (showTitle && g.name.isNotBlank()) SectionLabel(g.name, Modifier.padding(top = 6.dp, bottom = 6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GH.Surface)
                .padding(vertical = 6.dp),
        ) {
            TableHeader()
            g.rows.forEach { r -> TableRow(r, favorite = teamMatches(r.team, favorites)) }
        }
    }
}

private val numW = 26.dp
private val ptsW = 32.dp

@Composable
private fun TableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", Modifier.width(32.dp), color = GH.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("TAKIM", Modifier.weight(1f), color = GH.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        listOf("O", "G", "B", "M", "AV").forEach { NumCell(it, header = true) }
        Text("P", Modifier.width(ptsW), color = GH.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowScope.NumCell(v: String, header: Boolean = false) {
    Text(
        v,
        Modifier.width(numW),
        color = if (header) GH.Muted else GH.Text.copy(alpha = 0.85f),
        fontSize = if (header) 11.sp else 13.sp,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun TableRow(r: StandingRow, favorite: Boolean) {
    val strip = noteColor(r.noteColor)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (favorite) GH.Green.copy(alpha = 0.10f) else Color.Transparent)
            .padding(start = 0.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)).background(strip ?: Color.Transparent))
        Text(
            "${r.rank}",
            Modifier.width(32.dp),
            color = if (strip != null) GH.Text else GH.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        TeamLogo(r.logo, r.team, 22.dp)
        HSpace(8)
        Text(
            r.shortName,
            Modifier.weight(1f),
            color = if (favorite) GH.Green else GH.Text,
            fontSize = 14.sp,
            fontWeight = if (favorite) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NumCell("${r.played}")
        NumCell("${r.won}")
        NumCell("${r.drawn}")
        NumCell("${r.lost}")
        NumCell(if (r.goalDiff > 0) "+${r.goalDiff}" else "${r.goalDiff}")
        Text("${r.points}", Modifier.width(ptsW), color = GH.Text, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Legend(groups: List<StandingGroup>) {
    val notes = groups.flatMap { it.rows }
        .mapNotNull { r -> val c = noteColor(r.noteColor); if (c != null && r.note != null) r.note to c else null }
        .distinctBy { it.first }
    Column(Modifier.padding(top = 14.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        notes.forEach { (label, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
                HSpace(8)
                Text(label, color = GH.Muted, fontSize = 12.sp)
            }
        }
        Text("O: oynanan · G: galibiyet · B: beraberlik · M: mağlubiyet · AV: averaj · P: puan", color = GH.Muted, fontSize = 11.sp)
    }
}

// ---------------------------------------------------------------- Gol krallığı

@Composable
private fun ScorersList(vm: MainViewModel) {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    val l = vm.leaders
    val showAssists = l != null && l.assisters.isNotEmpty()
    val list: List<Scorer> = when {
        l == null -> emptyList()
        sub == 1 && showAssists -> l.assisters
        else -> l.scorers
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            vm.leadersLoading && list.isEmpty() -> item { LoadingBlock() }
            list.isEmpty() -> item {
                CenterMessage("👟", "Liste boş", vm.leadersError ?: "Veri bulunamadı.", "Tekrar dene") { vm.refreshLeaders() }
            }
            else -> {
                if (showAssists) item(key = "sub") {
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("⚽ Gol", sub == 0, { sub = 0 }, accent = GH.Gold)
                        Chip("🅰️ Asist", sub == 1, { sub = 1 }, accent = GH.Gold)
                    }
                }
                itemsIndexed(list, key = { i, s -> "${sub}_${i}_${s.name}" }) { i, s ->
                    ScorerRow(rankOf(list, i, assists = sub == 1 && showAssists), s, assists = sub == 1 && showAssists, favorite = teamMatches(s.team, vm.favoriteTeams))
                }
                if (l?.official == false) item(key = "note") {
                    Text(
                        "Bu lig için resmi istatistik gelmedi; liste sezonun maç sonuçlarından hesaplandı (kendi kalesine goller hariç). Asist bilgisi yok.",
                        Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
                        color = GH.Muted, fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/** Eşit sayıda gol atanlar aynı sırayı paylaşır. */
private fun rankOf(list: List<Scorer>, i: Int, assists: Boolean): Int {
    fun v(s: Scorer) = if (assists) s.assists ?: s.goals else s.goals
    var r = i
    while (r > 0 && v(list[r - 1]) == v(list[i])) r--
    return r + 1
}

@Composable
private fun ScorerRow(rank: Int, s: Scorer, assists: Boolean, favorite: Boolean) {
    val medal = when (rank) { 1 -> GH.Gold; 2 -> Color(0xFFC9D3DB); 3 -> Color(0xFFD08B4E); else -> null }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (favorite) GH.Green.copy(alpha = 0.10f) else GH.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(medal?.copy(alpha = 0.18f) ?: GH.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", color = medal ?: GH.Muted, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        HSpace(12)
        Column(Modifier.weight(1f)) {
            Text(s.name, color = GH.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (s.teamLogo.isNotBlank()) { TeamLogo(s.teamLogo, s.team, 16.dp); HSpace(6) }
                Text(s.team, color = GH.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${if (assists) s.assists ?: 0 else s.goals}", color = GH.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            val pens = s.penalties
            if (!assists && pens != null && pens > 0) Text("$pens penaltı", color = GH.Muted, fontSize = 10.sp)
        }
    }
}
