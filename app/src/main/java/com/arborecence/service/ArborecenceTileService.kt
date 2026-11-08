package com.arborecence.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.arborecence.MainActivity

class ArborecenceTileService : TileService() {
    override fun onClick() {
        super.onClick()
        // Toque: inicia/abre o assistente de voz (usuário concede microfone na tela).
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("route", "assistant")
            putExtra("start_voice", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }
}
