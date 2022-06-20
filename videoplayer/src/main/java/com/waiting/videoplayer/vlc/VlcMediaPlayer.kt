package com.waiting.videoplayer

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.waiting.utils.NetworkUtil
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.RendererItem
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Author: HeChao
 * Date: 2022/5/25 17:17
 * Description:
 */
internal class VlcMediaPlayer internal constructor(val context: Context) : XMediaPlayer() {

    private val mediaPlayer: MediaPlayer = MediaPlayer(LibVLC(context))
    private var displayManager: DisplayManager? = null
    private var duration = -1L
    private var totalDuration = -1L
    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying
    override var time: Long
        get() = duration.takeIf { it > 0 } ?: mediaPlayer.time.also { duration = it }
        set(value) {
            launch {
                mediaPlayer.time = value
            }
        }
    override val length: Long
        get() = totalDuration.takeIf { it > 0 } ?: mediaPlayer.length.also {
            totalDuration = it
        }
    override var rate: Float
        get() = mediaPlayer.rate
        set(value) {
            launch {
                mediaPlayer.rate = value
            }
        }
    override var videoScale: VideoScale
        get() = when (mediaPlayer.videoScale) {
            MediaPlayer.ScaleType.SURFACE_BEST_FIT -> {
                VideoScale.SURFACE_BEST_FIT
            }
            MediaPlayer.ScaleType.SURFACE_FIT_SCREEN -> {
                VideoScale.SURFACE_FIT_SCREEN
            }
            MediaPlayer.ScaleType.SURFACE_FILL -> {
                VideoScale.SURFACE_FILL
            }
            MediaPlayer.ScaleType.SURFACE_16_9 -> {
                VideoScale.SURFACE_16_9
            }
            MediaPlayer.ScaleType.SURFACE_4_3 -> {
                VideoScale.SURFACE_4_3
            }
            else -> VideoScale.SURFACE_BEST_FIT
        }
        set(value) {
            launch {
                when (value) {
                    VideoScale.SURFACE_BEST_FIT -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                    }
                    VideoScale.SURFACE_FIT_SCREEN -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
                    }
                    VideoScale.SURFACE_FILL -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                    }
                    VideoScale.SURFACE_16_9 -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_16_9
                    }
                    VideoScale.SURFACE_4_3 -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_4_3
                    }
                    else -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                    }
                }
            }
        }

    override fun getVideoView(context: Context): View {
        require(context is Activity) { "context must be Activity" }
        return VLCVideoLayout(context)
    }

    override fun attachView(view: View) {
        val rendererItem = MutableLiveData<RendererItem>()
        displayManager =
            DisplayManager(context as Activity, rendererItem, true, false, true)
        mediaPlayer.attachViews(view as VLCVideoLayout, displayManager, true, true)
    }

    override fun setSource(mediaItem: MediaItem) {
        launch {
            mediaPlayer.media = Media(mediaPlayer.libVLC, mediaItem.uri)
        }
    }

    override fun play() {
        launch {
            mediaPlayer.play()
        }
    }

    override fun play(uri: Uri) {
        launch {
            mediaPlayer.play(uri)
        }
    }

    override fun play(path: String) {
        launch {
            mediaPlayer.play(path)
        }
    }

    override fun replay() {
        launch {
            mediaPlayer.media?.uri?.apply { play(this) }
        }
    }

    override fun setEventListener(event: (PlayerEvent) -> Unit) {
        mediaPlayer.setEventListener { listener ->
            when (listener.type) {
                MediaPlayer.Event.MediaChanged, MediaPlayer.Event.Opening -> {
                    duration = -1L
                    totalDuration = -1L
                    event.invoke(PlayerEvent.Prepare)
                }
                MediaPlayer.Event.Buffering -> {
                    event.invoke(PlayerEvent.Buffering)
                }
                MediaPlayer.Event.Playing -> {
                    event.invoke(PlayerEvent.Playing)
                }
                MediaPlayer.Event.Paused -> {
                    event.invoke(PlayerEvent.Paused)
                }
                MediaPlayer.Event.Stopped -> {
                    event.invoke(PlayerEvent.Stopped)
                }
                MediaPlayer.Event.TimeChanged -> {
                    event.invoke(PlayerEvent.Changed(listener.timeChanged))
                }
                MediaPlayer.Event.EncounteredError -> {
                    event.invoke(PlayerEvent.Error)
                }
                MediaPlayer.Event.EndReached -> {
                    if (NetworkUtil.isNetworkConnected(context)) {
                        event.invoke(PlayerEvent.Completed)
                    }
                }
            }
            mediaPlayer.media?.uri?.apply {
                if (this.scheme?.equals("http") == true || this.scheme?.equals("https") == true) {
                    handleConnectionStatus(context)
                }
            }
        }
    }

    override fun pause() {
        launch {
            mediaPlayer.pause()
        }
    }

    override fun resume() {
        launch {
            mediaPlayer.play()
        }
    }

    override fun stop() {
        launch {
            mediaPlayer.media?.setEventListener(null)
            mediaPlayer.media?.release()
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
        }
    }

    override fun detachViews() {
        mediaPlayer.detachViews()
    }

    override fun release() {
        displayManager?.release()
        displayManager = null
        mediaPlayer.detachViews()
        launch {
            if (!mediaPlayer.isReleased) {
//                mediaPlayer.release()  // FIXME: will throw  VLCObject (org.videolan.libvlc.Media) finalized but not natively released (1 refs)
            }
        }
    }

    private fun launch(delay: Long = 0L, block: suspend CoroutineScope.() -> Unit) {
        MainScope().launch(CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e("TAG", "PlayerController RuntimeException coroutineContext $coroutineContext ${throwable.printStackTrace()}")
            throwable.printStackTrace()
        } + Dispatchers.IO) {
            if (delay > 0) {
                delay(delay)
            }
            block()
            cancel()
        }
    }

    private fun handleConnectionStatus(context: Context) {
        if (!NetworkUtil.isNetworkConnected(context)) {
            if (mediaPlayer.isPlaying) {
                launch { mediaPlayer.stop() }
            }
        }
    }
}