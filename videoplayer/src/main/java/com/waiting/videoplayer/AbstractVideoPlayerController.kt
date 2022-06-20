package com.waiting.videoplayer

import android.content.Context
import android.net.Uri
import android.view.View

/**
 * Author: HeChao
 * Date: 2022/1/28 10:50
 * Description:
 */
abstract class AbstractVideoPlayerController {

    abstract var currentItem: MediaItem?
    abstract var items: List<MediaItem>?
    abstract val isPlaying: Boolean
    abstract val getUrl: String
    abstract var videoScale: VideoScale
    abstract var time: Long
    abstract val length: Long
    abstract var rate: Float
    abstract fun generateVideoView(context: Context): View
    abstract fun attachView(view: View)
    abstract fun play()
    abstract fun play(path: String)
    abstract fun play(uri: Uri)
    abstract fun addInterceptor(interceptor: UriInterceptor)
    abstract fun replay()
    abstract fun resume()
    abstract fun pause()
    abstract fun setEventListener(event: (PlayerEvent) -> Unit)
    abstract fun stop()
    abstract fun release()
    abstract fun detachViews()
    abstract fun seekTo(position: Long)
    abstract fun recycler()
}