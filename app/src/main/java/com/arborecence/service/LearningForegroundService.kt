package com.arborecence.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.arborecence.MainActivity
import com.arborecence.Notifications
import com.arborecence.R
import com.arborecence.core.common.GeminiConfig
import com.arborecence.core.data.ai.CurriculumPlan
import com.arborecence.core.data.ai.GeminiCurriculumClient
import com.arborecence.core.data.connectivity.ConnectivityObserver
import com.arborecence.core.domain.model.LearningProgress
import com.arborecence.core.domain.repository.KnowledgeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class LearningForegroundService : LifecycleService() {
    @Inject lateinit var knowledge: KnowledgeRepository
    @Inject lateinit var gemini: GeminiCurriculumClient
    @Inject lateinit var connectivity: ConnectivityObserver
    @Inject lateinit var config: GeminiConfig
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForegroundNotif("Preparando aprendizado…", "Aguardando conexão e app aberto")
        if (job == null) job = lifecycleScope.launch { runLearning() }
        return START_NOT_STICKY
    }

    private suspend fun runLearning() {
        if (!gemini.isConfigured(config.apiKey)) { notify("Gemini não configurado", "Defina GEMINI_API_KEY (ver instruções). Aprendizado pausado."); stopSelf(); return }
        var progress = knowledge.progress().first()
        var day = if (progress.day in 1..14) progress.day else 1
        val areas = progress.areasCovered.toMutableSet()
        while (day <= 14) {
            val topics = CurriculumPlan.topicsForDay(day)
            notify("Dia $day de 14", "Áreas: ${areas.size} • fichas: ${knowledge.cardCount()}")
            knowledge.updateProgress(LearningProgress(day, areas.toList(), knowledge.cardCount(), true, System.currentTimeMillis()))
            for ((area, topic) in topics) {
                // Pausa se sem internet; retoma sozinho
                var backoff = 2000L
                while (!connectivity.online().first()) { notify("Dia $day de 14 — pausado", "Sem internet. Retomo quando conectar."); delay(4000) }
                val res = gemini.fetchCard(config.apiKey, area, topic)
                res.onSuccess { card -> knowledge.addCards(listOf(card)); areas += area }
                    .onFailure { delay(min(backoff, 60_000L)); backoff *= 2 } // respeita cotas/backoff
                notify("Dia $day de 14", "${areas.size} áreas • ${knowledge.cardCount()} fichas")
                delay(1200) // ritmo gentil
            }
            day++
        }
        knowledge.updateProgress(LearningProgress(14, areas.toList(), knowledge.cardCount(), false, System.currentTimeMillis()))
        notify("Aprendizado concluído", "${knowledge.cardCount()} fichas locais prontas. O assistente pode usar a base offline.")
        stopSelf()
    }

    private fun startForegroundNotif(title: String, text: String) {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).putExtra("route", "learning"), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, Notifications.CHANNEL_LEARNING)
            .setSmallIcon(R.drawable.ic_tree).setContentTitle(title).setContentText(text)
            .setContentIntent(pi).setOngoing(true)
            .addAction(0, "Parar", PendingIntent.getService(this, 1, Intent(this, javaClass).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE))
            .build()
        ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
    private fun notify(title: String, text: String) = startForegroundNotif(title, text)

    override fun onDestroy() { job?.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
    companion object {
        const val ACTION_STOP = "com.arborecence.STOP_LEARNING"
        const val NOTIF_ID = 1001
        fun start(ctx: Context) = ctx.startForegroundService(Intent(ctx, LearningForegroundService::class.java))
    }
}
