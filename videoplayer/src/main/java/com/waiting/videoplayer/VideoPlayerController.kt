package com.waiting.videoplayer

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.compose.runtime.compositionLocalOf

/**
 * Author: HeChao
 * Date: 2022/3/18 9:18
 * Description:
 */
internal val LocalVideoPlayerController = compositionLocalOf<VideoPlayerController> {
    error("VideoPlayerController is not initialized")
}

internal class VideoPlayerController internal constructor(
    private val mediaPlayer: XMediaPlayer,
) : AbstractVideoPlayerController() {

    private var currentMediaItem: MediaItem? = null
    private var mediaItems: List<MediaItem>? = null
    private var interceptor: UriInterceptor? = null
    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying
    override val getUrl: String
        get() = currentItem?.uri.toString()
    override var videoScale: VideoScale
        get() = mediaPlayer.videoScale
        set(value) {
            mediaPlayer.videoScale = value
        }
    override var time: Long
        get() = mediaPlayer.time
        set(value) {
            mediaPlayer.time = value
        }
    override val length: Long
        get() = mediaPlayer.length
    override var rate: Float
        get() = mediaPlayer.rate
        set(value) {
            mediaPlayer.rate = value
        }
    override var currentItem: MediaItem?
        get() = currentMediaItem
        set(value) {
            value?.let {
                currentMediaItem = it
                mediaPlayer.setSource(it)
            }
        }
    override var items: List<MediaItem>?
        get() = mediaItems
        set(value) {
            mediaItems = value
        }

    override fun generateVideoView(context: Context): View {
        return mediaPlayer.getVideoView(context)
    }

    override fun attachView(view: View) {
        mediaPlayer.attachView(view)
    }

    override fun play() {
        mediaPlayer.play()
    }

    override fun play(path: String) {
        mediaPlayer.play(path)
    }

    override fun play(uri: Uri) {
        mediaPlayer.play(interceptor?.parseUri(uri) ?: uri)
    }

    override fun addInterceptor(interceptor: UriInterceptor) {
        this.interceptor = interceptor
    }

    override fun replay() {
        mediaPlayer.replay()
    }

    override fun resume() {
        mediaPlayer.resume()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun setEventListener(event: (PlayerEvent) -> Unit) {
        mediaPlayer.setEventListener(event)
    }

    override fun stop() {
        mediaPlayer.stop()
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun detachViews() {
        mediaPlayer.detachViews()
    }

    override fun seekTo(position: Long) {
        this.time = position
    }

    override fun recycler() {
        stop()
        release()
        currentMediaItem = null
        mediaItems = null
    }
}

