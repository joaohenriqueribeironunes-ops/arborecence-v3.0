package com.arborecence.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.arborecence.MainActivity
import com.arborecence.Notifications
import com.arborecence.R
import com.arborecence.core.data.assistant.LocalAssistantEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAssistantForegroundService : LifecycleService() {
    @Inject lateinit var engine: LocalAssistantEngine

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { engine.release(); stopSelf(); return START_NOT_STICKY }
        val openPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).putExtra("route", "assistant"), PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 2, Intent(this, javaClass).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, Notifications.CHANNEL_ASSISTANT)
            .setSmallIcon(R.drawable.ic_tree).setContentTitle("Assistente Arborecence")
            .setContentText("Microfone ativo somente após seu toque. Diga \"encerrar\" para parar.")
            .setContentIntent(openPi).setOngoing(true)
            .addAction(0, "Encerrar", stopPi)
            .build()
        ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        return START_STICKY
    }

    override fun onDestroy() { engine.release(); super.onDestroy() }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
    companion object {
        const val ACTION_STOP = "com.arborecence.STOP_ASSISTANT"
        const val NOTIF_ID = 1002
        fun start(ctx: Context) = ctx.startForegroundService(Intent(ctx, VoiceAssistantForegroundService::class.java))
        fun stop(ctx: Context) = ctx.startService(Intent(ctx, VoiceAssistantForegroundService::class.java).setAction(ACTION_STOP))
    }
}
