package com.hadat.aiyoga.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelDownloader {
    private const val BASE_URL = "https://raw.githubusercontent.com/hadatttt/DataYoga/main/"
    val YOGA_MODELS = listOf(
        "yoga_model.tflite",
        "pose_landmarker_heavy.task"
    )

    // Biến để kiểm tra xem có đang tải dở không, tránh tải trùng
    private val downloadingFiles = mutableSetOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun downloadAllModels(
        context: Context,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        var downloadedCount = 0
        var hasError = false

        val allExist = YOGA_MODELS.all { File(context.filesDir, it).exists() }
        if (allExist) {
            onComplete(true)
            return
        }

        YOGA_MODELS.forEach { fileName ->
            downloadFile(context, fileName,
                onProgress = { progress ->
                    mainHandler.post { onProgress(progress) }
                },
                onComplete = { file ->
                    synchronized(this) {
                        if (file == null) hasError = true
                        downloadedCount++
                        if (downloadedCount == YOGA_MODELS.size) {
                            mainHandler.post { onComplete(!hasError) }
                        }
                    }
                }
            )
        }
    }

    private fun downloadFile(context: Context, fileName: String, onProgress: (Int) -> Unit, onComplete: (File?) -> Unit) {
        val targetFile = File(context.filesDir, fileName)

        if (targetFile.exists() && targetFile.length() > 0) {
            onComplete(targetFile)
            return
        }

        synchronized(downloadingFiles) {
            if (downloadingFiles.contains(fileName)) return
            downloadingFiles.add(fileName)
        }

        val url = BASE_URL + fileName
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                synchronized(downloadingFiles) { downloadingFiles.remove(fileName) }
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    synchronized(downloadingFiles) { downloadingFiles.remove(fileName) }
                    onComplete(null)
                    return
                }

                try {
                    val totalBytes = body.contentLength()
                    val inputStream = body.byteStream()
                    val tempFile = File(context.filesDir, "$fileName.tmp")
                    val outputStream = FileOutputStream(tempFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(((totalRead * 100) / totalBytes).toInt())
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    tempFile.renameTo(targetFile)
                    synchronized(downloadingFiles) { downloadingFiles.remove(fileName) }
                    onComplete(targetFile)
                } catch (e: Exception) {
                    if (targetFile.exists()) targetFile.delete()
                    synchronized(downloadingFiles) { downloadingFiles.remove(fileName) }
                    onComplete(null)
                }
            }
        })
    }
}