package com.arborecence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_LEARNING = "learning"
    const val CHANNEL_ASSISTANT = "assistant"
    fun ensure(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_LEARNING, "Aprendizado (14 dias)", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Progresso do aprendizado com o app aberto"
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ASSISTANT, "Assistente de voz", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Microfone ativo somente após seu toque"
            setShowBadge(false)
        })
    }
}
