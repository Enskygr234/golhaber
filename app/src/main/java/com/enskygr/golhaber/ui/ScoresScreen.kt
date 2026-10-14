package com.enskygr.golhaber.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enskygr.golhaber.data.Goal
import com.enskygr.golhaber.data.Match
import com.enskygr.golhaber.data.MatchState
import com.enskygr.golhaber.data.TeamSide
import com.enskygr.golhaber.data.teamMatches
import com.enskygr.golhaber.ui.theme.GH
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tr = Locale("tr", "TR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        DayRow(vm.dayOffset, vm::setDay)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Chip("Tümü", vm.selectedLeague == null, { vm.selectedLeague = null }) }
            items(vm.enabledLeagueList(), key = { it.code }) { lg ->
                Chip(lg.short, vm.selectedLeague == lg.code, { vm.selectedLeague = lg.code })
            }
        }
        PullToRefreshBox(
            isRefreshing = vm.scoresLoading && vm.matches.isNotEmpty(),
            onRefresh = { vm.refreshScores() },
            modifier = Modifier.fillMaxSize(),
        ) {
            val list = vm.visibleMatches()
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                when {
                    vm.scoresLoading && list.isEmpty() -> item { LoadingBlock() }
                    vm.scoresError != null && list.isEmpty() -> item {
                        CenterMessage("📡", "Bağlantı sorunu", vm.scoresError ?: "", "Tekrar dene") { vm.refreshScores() }
                    }
                    list.isEmpty() -> item {
                        CenterMessage("🗓️", "Bu tarihte maç yok", "Başka bir gün seç ya da Ayarlar'dan lig ekle.")
                    }
                    else -> {
                        val live = list.filter { it.isLive }
                        if (live.isNotEmpty() && vm.selectedLeague == null) {
                            item(key = "live_header") {
                                Row(Modifier.padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LiveDot()
                                    HSpace(8)
                                    SectionLabel("Şu an oynanıyor", color = GH.Live)
                                }
                            }
                        }
                        list.groupBy { it.league }.forEach { (lg, ms) ->
                            item(key = "h_${lg.code}") {
                                SectionLabel(lg.name, Modifier.padding(top = 14.dp, bottom = 6.dp))
                            }
                            items(ms, key = { "${lg.code}_${it.id}" }) { m ->
                                MatchCard(
                                    m = m,
                                    followed = m.id in vm.followed,
                                    favorite = teamMatches(m.home.name, vm.favoriteTeams) || teamMatches(m.away.name, vm.favoriteTeams),
                                    onFollow = { vm.toggleFollow(m) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(selected: Int, onSelect: (Int) -> Unit) {
    val today = LocalDate.now()
    val dayFmt = DateTimeFormatter.ofPattern("d MMM", tr)
    val wkFmt = DateTimeFormatter.ofPattern("EEE", tr)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (-2..2).forEach { off ->
            val d = today.plusDays(off.toLong())
            val label = when (off) {
                -1 -> "Dün"
                0 -> "Bugün"
                1 -> "Yarın"
                else -> d.format(wkFmt).replaceFirstChar { it.titlecase(tr) }
            }
            val sel = off == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) GH.Green else GH.Surface)
                    .clickable { onSelect(off) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, color = if (sel) GH.Bg else GH.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(d.format(dayFmt), color = if (sel) GH.Bg.copy(alpha = 0.7f) else GH.Muted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun MatchCard(m: Match, followed: Boolean, favorite: Boolean, onFollow: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GH.Surface,
        border = when {
            m.isLive -> BorderStroke(1.dp, GH.Live.copy(alpha = 0.55f))
            favorite || followed -> BorderStroke(1.dp, GH.Green.copy(alpha = 0.45f))
            else -> BorderStroke(1.dp, GH.Line)
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Column(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TeamRow(m.home, m, winner = m.state == MatchState.POST && (m.home.score ?: 0) > (m.away.score ?: 0))
                    TeamRow(m.away, m, winner = m.state == MatchState.POST && (m.away.score ?: 0) > (m.home.score ?: 0))
                }
                Box(Modifier.width(1.dp).height(48.dp).background(GH.Line))
                StatusBox(m)
                IconButton(onClick = onFollow, enabled = m.state != MatchState.POST) {
                    Icon(
                        if (followed) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = if (followed) "Bildirimleri kapat" else "Bu maçı takip et",
                        tint = if (followed) GH.Green else GH.Muted.copy(alpha = if (m.state == MatchState.POST) 0.3f else 1f),
                    )
                }
            }
            if (m.goals.isNotEmpty() && m.state != MatchState.PRE) {
                HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 8.dp, end = 10.dp), color = GH.Line)
                GoalList(m.goals)
            }
        }
    }
}

@Composable
private fun TeamRow(t: TeamSide, m: Match, winner: Boolean) {
    val dim = m.state == MatchState.POST && !winner
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(t.logo, t.name, 26.dp)
        HSpace(10)
        Text(
            t.name,
            modifier = Modifier.weight(1f),
            color = if (dim) GH.Muted else GH.Text,
            fontSize = 15.sp,
            fontWeight = if (winner) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (m.state != MatchState.PRE && m.state != MatchState.CANCELLED) {
            Text(
                "${t.score ?: 0}",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = when {
                    m.isLive -> GH.Text
                    dim -> GH.Muted
                    else -> GH.Text
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun StatusBox(m: Match) {
    Column(
        Modifier.width(62.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (m.state) {
            MatchState.LIVE -> {
                LiveDot(7)
                Text(m.statusText, color = GH.Live, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            MatchState.PRE -> {
                Text(m.statusText, color = GH.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("başlama", color = GH.Muted, fontSize = 10.sp)
            }
            MatchState.POST -> Text(m.statusText, color = GH.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            MatchState.CANCELLED -> Text(m.statusText, color = GH.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GoalList(goals: List<Goal>) {
    Column(Modifier.padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        goals.forEach { g ->
            val text = buildString {
                append("⚽ ").append(g.minute).append(" ").append(g.player)
                if (g.penalty) append(" (P)")
                if (g.ownGoal) append(" (KK)")
            }
            Text(
                text,
                modifier = Modifier.fillMaxWidth(),
                color = GH.Muted,
                fontSize = 12.sp,
                textAlign = if (g.home) TextAlign.Start else TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
