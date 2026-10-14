package com.enskygr.golhaber.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.enskygr.golhaber.MainActivity
import com.enskygr.golhaber.R

data class SoundOption(val key: String, val label: String, val res: Int)

object Notifier {
    const val CH_NEWS = "transfer_haber"
    private const val GREEN = 0xFF12B856.toInt()

    val sounds = listOf(
        SoundOption("whistle", "Hakem düdüğü", R.raw.whistle),
        SoundOption("crowd", "Tribün coşkusu", R.raw.crowd),
        SoundOption("classic", "Skorbord zili", R.raw.classic),
    )

    fun goalChannel(sound: String) = "gol_$sound"

    private fun soundUri(ctx: Context, res: Int): Uri =
        Uri.parse("android.resource://${ctx.packageName}/$res")

    private val audioAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Kanalın sesi oluşturulduktan sonra değişmediği için her ses ayrı kanal. */
    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        sounds.forEach { s ->
            val ch = NotificationChannel(goalChannel(s.key), "Gol bildirimleri · ${s.label}", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Takip ettiğin maçlarda gol ve maç sonu"
            ch.setSound(soundUri(ctx, s.res), audioAttrs)
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 180, 90, 180, 90, 420)
            ch.lightColor = GREEN
            ch.enableLights(true)
            nm.createNotificationChannel(ch)
        }
        val news = NotificationChannel(CH_NEWS, "Transfer haberleri", NotificationManager.IMPORTANCE_DEFAULT)
        news.description = "Son dakika transfer gelişmeleri"
        news.setSound(soundUri(ctx, R.raw.chime), audioAttrs)
        nm.createNotificationChannel(news)
    }

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    private fun show(ctx: Context, id: Int, channel: String, title: String, text: String, link: String? = null) {
        if (!canNotify(ctx)) return
        val intent = if (link != null) Intent(Intent.ACTION_VIEW, Uri.parse(link))
        else Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(GREEN)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(if (channel == CH_NEWS) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun goal(ctx: Context, sound: String, id: Int, title: String, text: String) =
        show(ctx, id, goalChannel(sound), title, text)

    fun news(ctx: Context, id: Int, title: String, text: String, link: String) =
        show(ctx, id, CH_NEWS, title, text, link)

    fun test(ctx: Context, sound: String) = goal(
        ctx, sound, 4242,
        "⚽ GOL! Ev Sahibi 2-1 Deplasman",
        "Bu bir test bildirimi. Takip ettiğin maçlarda gol olduğunda böyle haber vereceğiz.",
    )

    private var player: MediaPlayer? = null

    fun preview(ctx: Context, res: Int) {
        player?.release()
        player = MediaPlayer.create(ctx, res)?.apply {
            setOnCompletionListener { it.release(); if (player === it) player = null }
            start()
        }
    }
}
