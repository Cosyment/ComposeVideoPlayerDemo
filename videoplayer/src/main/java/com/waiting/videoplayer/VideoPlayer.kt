package com.waiting.videoplayer

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Author: HeChao
 * Date: 2022/1/28 10:50
 * Description:
 */

@Composable
fun rememberVideoPlayerController(): AbstractVideoPlayerController {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaPlayer: XMediaPlayer by remember {
        mutableStateOf(VlcMediaPlayer(context))
    }

    return rememberSaveable(
        context, coroutineScope,
        saver = object : Saver<VideoPlayerController, Any> {
            override fun restore(value: Any): VideoPlayerController = VideoPlayerController(mediaPlayer)
            override fun SaverScope.save(value: VideoPlayerController): Any {
                return value.getUrl
            }
        },
        init = {
            VideoPlayerController(mediaPlayer)
        })
}

@Composable
fun rememberUiControllerState(controller: AbstractVideoPlayerController): PlaybackStateDelegate {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return rememberSaveable(context, coroutineScope, saver = PlaybackStateDelegate.Saver) {
        val delegateState = PlaybackStateDelegate(PlayerState(uiState = UiState(
            showTopBar = true,
            showBottomBar = true,
        ), currentItem = controller.currentItem,
            items = controller.items,
            currentVolume = getCurrentVolume(context),
            currentBrightness = getCurrentBrightness(context as Activity)
        ))

        controller.setEventListener {

            when (it) {
                PlayerEvent.IDLE -> {
                    delegateState.showLoadingOverlay(true)
                }
                PlayerEvent.Buffering -> {
                    delegateState.showLoadingOverlay(true)
                    delegateState.totalDuration.takeIf { it <= 0 }?.let {
                        delegateState.updateTotalDuration(total = controller.length)
                    }
                }
                PlayerEvent.Playing -> {
                    delegateState.isPlaying = true
                }
                is PlayerEvent.Changed -> {
                    delegateState.showLoadingOverlay(false)
                    val time = it.value
                    if (delegateState.historyDuration < time) {
                        delegateState.updateDuration(time)
                        if (!delegateState.isDragging) {
                            delegateState.updateDraggingProgress(time)
                        }
                    }
                }
                PlayerEvent.Paused -> {
                    delegateState.showLoadingOverlay(false)
                }
                PlayerEvent.Stopped -> {
//                    delegateState.showLoading(false)
                }
                PlayerEvent.Error -> {
                    delegateState.showErrorOverlay(true)
                }
                PlayerEvent.Completed -> {
                    delegateState.showCompleteOverlay(true)
                }
                else -> {}
            }
        }
        delegateState
    }
}

@Composable
fun VideoPlayer(
    controller: AbstractVideoPlayerController,
    modifier: Modifier = Modifier,
    state: PlaybackStateDelegate = rememberUiControllerState(controller),
    content: (@Composable BoxScope.(PlaybackStateDelegate) -> Unit)? = null,
) {
    require(controller is VideoPlayerController) {
        "Use [rememberVideoPlayerController] to create an instance of [VideoPlayerController]"
    }

    CompositionLocalProvider(
        LocalVideoPlayerController provides controller,
    ) {
        Box(
            modifier = modifier
                .background(color = Color.Black)
                .fillMaxSize()
        ) {
            AndroidView(factory = { context ->
                controller.generateVideoView(context)
            }, update = {
                controller.attachView(it)
            })

            if (content != null) {
                content(state)
            } else {
                StandardControllerLayer(state = state, modifier = modifier,
                    topBarOverlay = {
                        TopBarOverlay(modifier = Modifier.align(Alignment.TopCenter), it) {
                            TopBarExtensionOverlay(state = it)
                        }
                    },
                    bottomBarOverlay = { BottomBarOverlay(modifier = Modifier.align(Alignment.BottomCenter), it) },
                    gestureOverlay = { GestureOverlay(it) },
                    stateOverlay = { StateOverlay(modifier = Modifier.align(Alignment.Center), state = it) },
                    extensionOverlay = { ExtensionOverlay(it) })
            }
        }
    }
}

private fun getCurrentVolume(context: Context): Float {
    val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
}

private fun getCurrentBrightness(activity: Activity): Float {
    val attributes = activity.window.attributes
    return if (attributes.screenBrightness != -1f)
        attributes.screenBrightness
    else {
        val contentResolver = activity.application.contentResolver
        if (Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        ) {
            0.5f
        } else Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            .toFloat() / 255
    }
}



