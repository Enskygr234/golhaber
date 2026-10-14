package com.enskygr.golhaber.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enskygr.golhaber.data.GoalMode
import com.enskygr.golhaber.data.Leagues
import com.enskygr.golhaber.data.PopularTeams
import com.enskygr.golhaber.notify.Notifier
import com.enskygr.golhaber.ui.theme.GH

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onAskPermission: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!vm.notifPermission) item {
            Card(border = GH.Live.copy(alpha = 0.5f)) {
                Text("🔕 Bildirimler kapalı", color = GH.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Gol ve transfer bildirimleri için izin vermen gerekiyor.", color = GH.Muted, fontSize = 13.sp)
                Button(
                    onClick = onAskPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = GH.Green, contentColor = GH.Bg),
                ) { Text("İzin ver", fontWeight = FontWeight.Bold) }
            }
        }

        item {
            Card {
                SectionLabel("Gol bildirimleri")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Kapalı", vm.goalMode == GoalMode.OFF, { vm.updateGoalMode(GoalMode.OFF) })
                    Chip("Favorilerim", vm.goalMode == GoalMode.FAVORITES, { vm.updateGoalMode(GoalMode.FAVORITES) })
                    Chip("Tüm maçlar", vm.goalMode == GoalMode.ALL, { vm.updateGoalMode(GoalMode.ALL) })
                }
                Text(
                    "Favorilerim: favori takımlarının maçları ve 🔔 ile takip ettiğin maçlar.",
                    color = GH.Muted, fontSize = 12.sp,
                )
                ToggleRow("Maç sonu bildirimi", "Takip ettiğin maç bitince sonucu gönder", vm.notifyFinal, vm::updateNotifyFinal)
                ToggleRow("Transfer haberleri", "Yeni transfer gelişmelerinde bildirim", vm.notifyTransfer, vm::updateNotifyTransfer)
            }
        }

        item {
            Card {
                SectionLabel("Bildirim sesi")
                Notifier.sounds.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.updateSound(s.key) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = vm.sound == s.key,
                            onClick = { vm.updateSound(s.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = GH.Green, unselectedColor = GH.Muted),
                        )
                        Text(s.label, color = GH.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { Notifier.preview(ctx, s.res) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Dinle", tint = GH.Green)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { Notifier.test(ctx, vm.sound) },
                    border = BorderStroke(1.dp, GH.Green),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Test bildirimi gönder", color = GH.Green, fontWeight = FontWeight.Bold) }
            }
        }

        item {
            Card {
                SectionLabel("Favori takımlar")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PopularTeams.all.forEach { t ->
                        Chip(t, t in vm.favoriteTeams, { vm.toggleTeam(t) })
                    }
                }
            }
        }

        item {
            Card {
                SectionLabel("Ligler")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Leagues.all.forEach { lg ->
                        Chip(lg.name, lg.code in vm.leagues, { vm.toggleLeague(lg.code) })
                    }
                }
            }
        }

        item {
            Card {
                SectionLabel("Hakkında")
                Text("GolHaber 1.0", color = GH.Text, fontWeight = FontWeight.Bold)
                Text(
                    "Skorlar: ESPN · Transfer haberleri: Google Haberler. Canlı maç varken arka planda yaklaşık 2 dakikada bir, diğer zamanlarda 15 dakikada bir kontrol edilir.",
                    color = GH.Muted, fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun Card(border: androidx.compose.ui.graphics.Color = GH.Line, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GH.Surface,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = GH.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = GH.Muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GH.Bg,
                checkedTrackColor = GH.Green,
                uncheckedThumbColor = GH.Muted,
                uncheckedTrackColor = GH.Surface2,
                uncheckedBorderColor = GH.Line,
            ),
        )
    }
}
