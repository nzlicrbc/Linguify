package com.example.linguify.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.linguify.R

private val GLIDE_OPTIONS = RequestOptions()
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .centerCrop()
    .priority(Priority.HIGH)
    .dontAnimate()
    .dontTransform()

fun ImageView.loadWithCache(url: String?, placeholder: Int = R.drawable.placeholder_image) {
    if (url.isNullOrEmpty()) {
        setImageResource(placeholder)
        return
    }

    Glide.with(context.applicationContext)
        .load(url)
        .apply(GLIDE_OPTIONS)
        .placeholder(placeholder)
        .error(placeholder)
        .transition(DrawableTransitionOptions.withCrossFade(100))
        .thumbnail(0.1f)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}