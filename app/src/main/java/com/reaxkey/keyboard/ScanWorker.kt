package com.reaxkey.keyboard

import android.content.Context
import androidx.work.*
import com.reaxkey.model.ReaxHost
import com.reaxkey.media.MediaAccess

class ScanWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
    override suspend fun doWork():Result {
        val repo=(applicationContext as ReaxHost).repository
        val id=inputData.getString("source") ?: return Result.failure()
        val source=repo.source(id) ?: return Result.failure()
        if(!source.enabled) return Result.success()
        repo.putSource(source.copy(health="SCANNING"))
        MediaAccess(applicationContext,repo).scan(source) { count -> setProgress(workDataOf("indexed" to count)) }
        return Result.success()
    }
    companion object {
        fun enqueue(context:Context,id:String) {
            // A user-requested refresh must recover a job that Android stopped and left queued.
            WorkManager.getInstance(context).enqueueUniqueWork("scan-$id",ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ScanWorker>().setInputData(workDataOf("source" to id)).build())
        }
    }
}
