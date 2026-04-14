package com.hadat.aiyoga.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

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

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        init(context)

        MediaManager.get().upload(imageUri)
            .unsigned("yoga_ai_data")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload started")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as String
                    Log.d("Cloudinary", "Upload success: $url")
                    onSuccess(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload error: ${error?.description}")
                    onError(error?.description ?: "Upload failed")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}