package com.waiting.videoplayer

import android.content.Context
import android.net.Uri
import android.view.View

/**
 * Author: HeChao
 * Date: 2022/5/25 17:15
 * Description:
 */
internal abstract class XMediaPlayer {

    abstract val isPlaying: Boolean
    abstract var time: Long
    abstract val length: Long
    abstract var rate: Float
    abstract var videoScale: VideoScale
    abstract fun  getVideoView(context: Context): View
    abstract fun  attachView(view: View)
    abstract fun setSource(mediaItem: MediaItem)
    abstract fun play()
    abstract fun play(uri: Uri)
    abstract fun play(path: String)
    abstract fun setEventListener(event: (PlayerEvent) -> Unit)
    abstract fun replay()
    abstract fun pause()
    abstract fun resume()
    abstract fun stop()
    abstract fun detachViews()
    abstract fun release()
}