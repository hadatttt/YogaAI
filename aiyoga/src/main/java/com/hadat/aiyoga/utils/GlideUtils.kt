package com.hadat.aiyoga.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.hadat.aiyoga.R // Đảm bảo đúng package R của dự án bạn
import java.io.File

/**
 * Load ảnh từ URL mạng thông thường (Dùng cho Yoga-82 Dataset)
 */

fun ImageView.loadGif(gifResource: Int) {
    Glide.with(this)
        .asGif()
        .load(gifResource)
        .into(this)
}
fun ImageView.loadImageContent(path: String){
    val uri = Uri.parse(path)
    val requestBuilder = Glide.with(this).asDrawable().sizeMultiplier(0.1f)
    Glide.with(this)
        .load(uri)
        .placeholder(hoang.dqm.codebase.R.drawable.bg_rectangle_gray_radius_24)
        .thumbnail(requestBuilder)
        .into(this)
}
fun ImageView.loadImage(path: String) {
    val requestBuilder: RequestBuilder<Drawable> =
        Glide.with(this).asDrawable().sizeMultiplier(0.1f)
    Glide.with(this).load(File(path)).placeholder(hoang.dqm.codebase.R.drawable.bg_rectangle_gray_radius_24)
        .thumbnail(requestBuilder).into(this)
}

fun ImageView.loadImageWithUri(uri: Uri) {
    val requestBuilder: RequestBuilder<Drawable> =
        Glide.with(this).asDrawable().sizeMultiplier(0.1f)
    Glide.with(this).load(uri).thumbnail(requestBuilder).into(this)
}
fun ImageView.loadImageFromNetwork(url: String) {
    if (url.isEmpty()) return

    // Tạo thumbnail (0.1x kích thước) để người dùng thấy ảnh mờ trước khi ảnh chính tải xong
    val thumbnailRequest = Glide.with(this)
        .asDrawable()
        .sizeMultiplier(0.1f)

    Glide.with(this)
        .load(url) // Truyền trực tiếp URL String
        .placeholder(hoang.dqm.codebase.R.drawable.bg_rectangle_gray_radius_24) // Ảnh hiện khi đang load
        .error(hoang.dqm.codebase.R.drawable.bg_rectangle_gray_radius_24) // Ảnh hiện khi load lỗi
        .thumbnail(thumbnailRequest)
        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache cả ảnh gốc và ảnh đã resize
        .transition(DrawableTransitionOptions.withCrossFade()) // Hiệu ứng mượt mà
        .into(this)
}
fun ImageView.loadImage(drawable: Int?) {
    val requestBuilder: RequestBuilder<Drawable> =
        Glide.with(this).asDrawable().sizeMultiplier(0.1f)
    Glide.with(this).load(drawable).thumbnail(requestBuilder).into(this)
}

fun ImageView.loadImageBitmap(bitmap: Bitmap?, onResourceReady: (Bitmap) -> Unit) {
    val mBitmap = bitmap ?: return
    Glide.with(this).asBitmap().load(mBitmap).listener(object : RequestListener<Bitmap> {
        override fun onLoadFailed(
            e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>, isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onResourceReady(
            resource: Bitmap,
            model: Any,
            target: com.bumptech.glide.request.target.Target<Bitmap>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            onResourceReady.invoke(resource)
            return false
        }

    }).into(this)
}






