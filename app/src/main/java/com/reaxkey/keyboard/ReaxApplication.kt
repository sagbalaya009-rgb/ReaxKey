package com.reaxkey.keyboard

import android.app.Application
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import com.reaxkey.data.*
import com.reaxkey.model.*

class ReaxApplication:Application(),ReaxHost,SingletonImageLoader.Factory {
    private val database by lazy { ReaxDatabase.open(this) }
    override val repository:MediaRepository by lazy { LocalRepository(database) }
    override fun newImageLoader(context:Context)=ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizeBytes(24L*1024*1024).build() }
        .components { if(Build.VERSION.SDK_INT>=28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory()) }
        .build()
}
