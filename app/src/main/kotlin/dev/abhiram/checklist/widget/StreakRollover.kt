package dev.abhiram.checklist.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

const val STREAK_ROLLOVER_WORK = "streak-widget-rollover"

fun millisUntilNextMidnight(now: ZonedDateTime): Long {
    val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, next).toMillis()
}

fun scheduleStreakRollover(context: Context) {
    val request = OneTimeWorkRequestBuilder<StreakRolloverWorker>()
        .setInitialDelay(
            millisUntilNextMidnight(ZonedDateTime.now()),
            TimeUnit.MILLISECONDS,
        )
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        STREAK_ROLLOVER_WORK,
        ExistingWorkPolicy.REPLACE,
        request,
    )
}

class StreakRolloverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            updateStreakWidgets(applicationContext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to refresh streak widgets", error)
        } finally {
            try {
                scheduleStreakRollover(applicationContext)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to schedule the next streak rollover", error)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "StreakRollover"
    }
}
