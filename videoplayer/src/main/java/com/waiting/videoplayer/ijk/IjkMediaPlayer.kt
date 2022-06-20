package com.waiting.videoplayer.ijk

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.RequiresApi
import com.waiting.videoplayer.MediaItem
import com.waiting.videoplayer.PlayerEvent
import com.waiting.videoplayer.VideoScale
import com.waiting.videoplayer.XMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Author: HeChao
 * Date: 2022/5/31 11:38
 * Description:
 */
internal class IjkMediaPlayer internal constructor(val context: Context) : XMediaPlayer() {

    private val ijkMediaPlayer = IjkMediaPlayer()
    override val isPlaying: Boolean
        get() = ijkMediaPlayer.isPlaying
    override var time: Long
        get() = ijkMediaPlayer.duration
        set(value) {
            ijkMediaPlayer.seekTo(value)
        }
    override val length: Long
        get() = ijkMediaPlayer.fileSize
    override var rate: Float
        get() = ijkMediaPlayer.getSpeed(0.0F)
        set(value) {
            ijkMediaPlayer.setSpeed(value)
        }
    override var videoScale: VideoScale
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun getVideoView(context: Context): View {
        val textureView = TextureView(context)
        return textureView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun attachView(view: View) {
        (view as TextureView).let {
            it.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    ijkMediaPlayer.setSurface(Surface(p0))
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    return p0.isReleased
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                }
            }
        }
    }

    override fun setSource(mediaItem: MediaItem) {
//        ijkMediaPlayer.setDataSource(context, mediaItem.uri)
        ijkMediaPlayer.setDataSource(
            context,
            Uri.parse("https://sf1-hscdn-tos.pstatp.com/obj/media-fe/xgplayer_doc_video/flv/xgplayer-demo-360p.flv")
        )
        ijkMediaPlayer.prepareAsync()
    }

    override fun play() {
        ijkMediaPlayer.start()
    }

    override fun play(uri: Uri) {
    }

    override fun play(path: String) {
    }

    override fun setEventListener(event: (PlayerEvent) -> Unit) {
        ijkMediaPlayer.setOnPreparedListener {
            event.invoke(PlayerEvent.Prepare)
        }
        ijkMediaPlayer.setOnBufferingUpdateListener { _, i -> event.invoke(PlayerEvent.Changed(i.toLong())) }
        ijkMediaPlayer.setOnCompletionListener {
            event.invoke(PlayerEvent.Completed)
        }
        ijkMediaPlayer.setOnErrorListener { iMediaPlayer, i, i2 ->
            event.invoke(PlayerEvent.Error)
            true
        }
    }

    override fun replay() {
    }

    override fun pause() {
        ijkMediaPlayer.pause()
    }

    override fun resume() {
        ijkMediaPlayer.start()
    }

    override fun stop() {
        ijkMediaPlayer.stop()
    }

    override fun detachViews() {
    }

    override fun release() {
        ijkMediaPlayer.release()
    }
}