package com.hadat.aiyoga.utils.view

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import kotlin.collections.get

object CloudinaryUtils {

    private var isInit = false

    fun init(context: Context) {
        if (isInit) return

        val config = mapOf(
            "cloud_name" to "dy77vf9zx"
        )

        MediaManager.init(context, config)
        isInit = true
    }

    private fun uriToFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")

            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        init(context)

        val filePath = uriToFile(context, imageUri)

        if (filePath == null) {
            onError("Cannot read image")
            return
        }

        MediaManager.get().upload(filePath)
            .unsigned("yoga_ai_data")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as String
                    onSuccess(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Upload failed")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}