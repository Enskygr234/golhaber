package com.enskygr.golhaber.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enskygr.golhaber.data.NewsItem
import com.enskygr.golhaber.data.NewsTopics
import com.enskygr.golhaber.ui.theme.GH
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val badgeColors = listOf(
    Color(0xFF2563EB), Color(0xFFDC2626), Color(0xFF7C3AED), Color(0xFFEA580C),
    Color(0xFF0891B2), Color(0xFFDB2777), Color(0xFF16A34A), Color(0xFFCA8A04),
)

fun relativeTime(i: Instant?): String {
    if (i == null) return ""
    val d = Duration.between(i, Instant.now())
    return when {
        d.toMinutes() < 1 -> "az önce"
        d.toMinutes() < 60 -> "${d.toMinutes()} dk önce"
        d.toHours() < 24 -> "${d.toHours()} sa önce"
        d.toDays() < 7 -> "${d.toDays()} gün önce"
        else -> i.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR")))
    }
}

private fun isFresh(i: Instant?) = i != null && Duration.between(i, Instant.now()).toMinutes() < 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val open: (NewsItem) -> Unit = { n ->
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.link))) }
    }
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(NewsTopics.all, key = { it.label }) { t ->
                Chip(t.label, vm.topic == t, { vm.selectTopic(t) }, accent = GH.Gold)
            }
        }
        PullToRefreshBox(
            isRefreshing = vm.newsLoading && vm.news.isNotEmpty(),
            onRefresh = { vm.refreshNews() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    vm.newsLoading && vm.news.isEmpty() -> item { LoadingBlock() }
                    vm.newsError != null && vm.news.isEmpty() -> item {
                        CenterMessage("📡", "Bağlantı sorunu", vm.newsError ?: "", "Tekrar dene") { vm.refreshNews() }
                    }
                    vm.news.isEmpty() -> item {
                        CenterMessage("🗞️", "Henüz haber yok", "Bu konu için son 7 günde transfer haberi bulunamadı.")
                    }
                    else -> itemsIndexed(vm.news, key = { _, n -> n.link }) { i, n ->
                        if (i == 0) HeroNews(n) { open(n) } else NewsRow(n) { open(n) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroNews(n: NewsItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, GH.Gold.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF2B2410), GH.Surface, Color(0xFF0F2A1B))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("🔥 Transfer gündemi", color = GH.Gold)
            Text(n.title, color = GH.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            Meta(n)
        }
    }
}

@Composable
private fun NewsRow(n: NewsItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GH.Surface,
        border = BorderStroke(1.dp, GH.Line),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Initial(n.source, 36.dp, badgeColors[Math.floorMod(n.source.hashCode(), badgeColors.size)])
            HSpace(12)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    n.title,
                    color = GH.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Meta(n)
            }
        }
    }
}

@Composable
private fun Meta(n: NewsItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(n.source, color = GH.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("  ·  " + relativeTime(n.published), color = GH.Muted, fontSize = 12.sp, maxLines = 1)
        if (isFresh(n.published)) {
            HSpace(8)
            NewBadge()
        }
    }
}

@Composable
private fun NewBadge() {
    Text(
        "YENİ",
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GH.Live)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
    )
}
