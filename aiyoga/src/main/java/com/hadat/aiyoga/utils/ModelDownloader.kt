package com.hadat.aiyoga.utils

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelDownloader {
    private const val BASE_URL = "https://raw.githubusercontent.com/hadatttt/DataYoga/main/"
    val YOGA_MODELS = listOf(
        "yoga_model.tflite",
        "pose_landmarker_lite.task"
    )
    fun downloadAllModels(
        context: Context,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        var downloadedCount = 0
        var hasError = false

        YOGA_MODELS.forEach { fileName ->
            downloadFile(context, fileName,
                onProgress = {  },
                onComplete = { file ->
                    if (file == null) hasError = true
                    downloadedCount++

                    if (downloadedCount == YOGA_MODELS.size) {
                        onComplete(!hasError)
                    }
                }
            )
        }
    }

    private fun downloadFile(context: Context, fileName: String, onProgress: (Int) -> Unit, onComplete: (File?) -> Unit) {
        val targetFile = File(context.filesDir, fileName)
        Log.d("ModelDownloader", "Bắt đầu kiểm tra file: $fileName")

        if (targetFile.exists() && targetFile.length() > 0) {
            Log.d("ModelDownloader", "File $fileName đã tồn tại (Size: ${targetFile.length()})")
            onProgress(100)
            onComplete(targetFile)
            return
        }

        val url = BASE_URL + fileName
        Log.d("ModelDownloader", "Đang tải từ URL: $url")

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ModelDownloader", "Tải file $fileName THẤT BẠI: ${e.message}")
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("ModelDownloader", "Server báo lỗi (${response.code}) khi tải $fileName")
                    onComplete(null)
                    return
                }
                Log.d("ModelDownloader", "Server OK, bắt đầu ghi file $fileName...")
                val body = response.body ?: run {
                    Log.e("ModelDownloader", "Body rỗng"); onComplete(null); return
                }

                try {
                    val totalBytes = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(targetFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val p = ((totalRead * 100) / totalBytes).toInt()
                            onProgress(p)
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    Log.d("ModelDownloader", "Tải file $fileName THÀNH CÔNG!")
                    onComplete(targetFile)
                } catch (e: Exception) {
                    Log.e("ModelDownloader", "Lỗi khi ghi file $fileName: ${e.message}")
                    if (targetFile.exists()) targetFile.delete()
                    onComplete(null)
                }
            }
        })
    }
}