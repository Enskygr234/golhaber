package com.enskygr.golhaber.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enskygr.golhaber.ui.theme.GH

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Skorlar", Icons.Filled.Home),
    Tab("Transfer", Icons.AutoMirrored.Filled.List),
    Tab("Ayarlar", Icons.Filled.Settings),
)

@Composable
fun MainScreen(vm: MainViewModel, onAskPermission: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = GH.Bg,
        topBar = { TopBar(liveCount = vm.matches.count { it.isLive }) },
        bottomBar = {
            NavigationBar(containerColor = GH.Surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GH.Bg,
                            selectedTextColor = GH.Green,
                            indicatorColor = GH.Green,
                            unselectedIconColor = GH.Muted,
                            unselectedTextColor = GH.Muted,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ScoresScreen(vm)
                1 -> NewsScreen(vm)
                else -> SettingsScreen(vm, onAskPermission)
            }
        }
    }
}

@Composable
private fun TopBar(liveCount: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GH.Bg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = GH.Text)) { append("Gol") }
                withStyle(SpanStyle(color = GH.Green)) { append("Haber") }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.weight(1f))
        if (liveCount > 0) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GH.Live.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LiveDot()
                Text("$liveCount CANLI", color = GH.Live, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveDot(size: Int = 8) {
    val alpha = pulse()
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GH.Live.copy(alpha = alpha))
    )
}

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))
