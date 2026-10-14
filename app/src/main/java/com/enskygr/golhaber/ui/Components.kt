package com.enskygr.golhaber.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.enskygr.golhaber.ui.theme.GH

@Composable
fun pulse(): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    return a
}

@Composable
fun Initial(name: String, size: Dp, color: Color = GH.Surface2) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = GH.Text,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp,
        )
    }
}

@Composable
fun TeamLogo(url: String, name: String, size: Dp = 26.dp) {
    if (url.isBlank()) {
        Initial(name, size)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = name,
            modifier = Modifier.size(size),
            error = { Initial(name, size) },
            loading = { Initial(name, size) },
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, accent: Color = GH.Green) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else GH.Surface)
            .border(1.dp, if (selected) accent else GH.Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) GH.Bg else GH.Text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = GH.Muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
fun CenterMessage(emoji: String, title: String, sub: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 44.sp)
        Text(title, color = GH.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(sub, color = GH.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = GH.Green, contentColor = GH.Bg),
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GH.Green, strokeWidth = 3.dp)
    }
}
