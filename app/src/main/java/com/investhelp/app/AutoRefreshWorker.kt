package com.investhelp.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.ChangeHistoryRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.settings.SettingsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@HiltWorker
class AutoRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService,
    private val changeHistoryRepository: ChangeHistoryRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "auto_refresh_all"
        const val CHANNEL_ID = "auto_refresh"
        const val NOTIFICATION_ID = 9001
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo("Refreshing prices..."))

        val prefs = appContext.getSharedPreferences(
            SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
        )

        try {
            val allItems = itemRepository.getAllItems().first()
            if (allItems.isEmpty()) return Result.success()

            var successCount = 0
            var failCount = 0

            for (item in allItems) {
                try {
                    val quote = stockPriceService.fetchQuote(item.ticker)
                    val resolvedName = quote.shortName ?: item.name
                    val newValue = quote.price * item.quantity
                    val dayChange = (quote.price - quote.previousClose) * item.quantity

                    itemRepository.upsertItem(
                        item.copy(
                            name = resolvedName,
                            currentPrice = quote.price,
                            value = newValue,
                            dayGainLoss = dayChange
                        )
                    )
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    AppLog.log("Auto-refresh ${item.ticker} failed: ${e.message}")
                }
            }

            val now = LocalDateTime.now()
            prefs.edit().putLong(
                "last_refreshed_at",
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ).apply()

            if (prefs.getBoolean(SettingsViewModel.KEY_AUTO_UPDATE_CHANGE_HISTORY, false)) {
                recordChangeHistory()
            }

            AppLog.log("Auto-refresh complete: $successCount ok" +
                    if (failCount > 0) ", $failCount failed" else "")

            showCompletionNotification(successCount, failCount)
        } catch (e: Exception) {
            AppLog.log("Auto-refresh failed: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    private suspend fun recordChangeHistory() {
        try {
            val allItems = itemRepository.getAllItems().first()
            val etfValue = allItems.filter { it.type == InvestmentType.ETF }.sumOf { it.value }
            val stockValue = allItems.filter { it.type == InvestmentType.Stock }.sumOf { it.value }
            val totalValue = allItems.sumOf { it.value }
            val dailyChangeEtf = allItems.filter { it.type == InvestmentType.ETF }.sumOf { it.dayGainLoss }
            val dailyChangeStock = allItems.filter { it.type == InvestmentType.Stock }.sumOf { it.dayGainLoss }
            val dailyChangeTotal = allItems.sumOf { it.dayGainLoss }
            val today = LocalDate.now()

            val existing = changeHistoryRepository.getRecordByDate(today)
            changeHistoryRepository.upsertRecord(
                ChangeHistoryEntity(
                    id = existing?.id ?: 0,
                    date = today,
                    etfValue = etfValue,
                    stockValue = stockValue,
                    totalValue = totalValue,
                    dailyChangeEtf = dailyChangeEtf,
                    dailyChangeStock = dailyChangeStock,
                    dailyChangeTotal = dailyChangeTotal
                )
            )
        } catch (e: Exception) {
            AppLog.log("Auto-refresh change history failed: ${e.message}")
        }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Invest Help")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(success: Int, failed: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createNotificationChannel()

        val text = "Refreshed $success tickers" + if (failed > 0) ", $failed failed" else ""
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Auto Refresh Complete")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto Refresh",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background price refresh notifications"
        }
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
