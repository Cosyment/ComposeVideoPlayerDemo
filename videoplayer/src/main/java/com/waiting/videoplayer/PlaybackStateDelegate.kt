package com.waiting.videoplayer

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Author: HeChao
 * Date: 2022/6/8 14:07
 * Description:
 */
@Stable
class PlaybackStateDelegate(private val initialState: PlayerState) {

    private val stateFlow = MutableStateFlow(initialState)

    companion object {
        val Saver: Saver<PlaybackStateDelegate, *> = Saver(
            save = { it.initialState },
            restore = {
                PlaybackStateDelegate(it)
            }
        )
    }

    val currentIndex: Int
        get() {
            return stateFlow.value.currentItem?.let { currentItem -> stateFlow.value.items?.indexOfFirst { it.uri.path.equals(currentItem.uri.path) } }
                ?: 0
        }
    val currentDuration: Long
        get() {
            return stateFlow.value.currentDuration
        }
    val historyDuration: Long
        get() {
            return stateFlow.value.historyDuration
        }
    val totalDuration: Long
        get() {
            return stateFlow.value.totalDuration
        }

    val currentItem: MediaItem?
        get() {
            return stateFlow.value.items?.getOrNull(currentIndex) ?: stateFlow.value.currentItem
        }
    var isPlaying: Boolean
        get() {
            return stateFlow.value.isPlaying
        }
        set(value) {
            stateFlow.set {
                copy(
                    isPlaying = value,
                    uiState = uiState.copy(showErrorOverlay = false, showCompleteOverlay = false, enableGesture = true)
                )
            }
        }
    var isDragging: Boolean
        get() {
            return stateFlow.value.isDragging
        }
        set(value) {
            stateFlow.set { copy(isDragging = value) }
        }

    val isError: Boolean get() = stateFlow.value.uiState.showErrorOverlay

    val isCompleted: Boolean get() = stateFlow.value.uiState.showCompleteOverlay

    val isPipMode: Boolean get() = stateFlow.value.uiState.isPictureInPictureMode

    var isAutoEnterPipMode: Boolean
        get() = stateFlow.value.autoEnterPictureInPicture
        set(value) {
            stateFlow.set { copy(autoEnterPictureInPicture = value) }
        }

    var isAutoPlayNext: Boolean
        get() = stateFlow.value.autoPlayNext
        set(value) {
            stateFlow.set { copy(autoPlayNext = value) }
        }

    var isContinuation: Boolean
        get() = stateFlow.value.continuation
        set(value) {
            stateFlow.set { copy(continuation = value) }
        }

    @Composable
    fun collect(): State<PlayerState> {
        return stateFlow.collectAsState()
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T> collect(filter: PlayerState.() -> T): State<T> {
        return remember(filter) {
            stateFlow.map { it.filter() }
        }.collectAsState(initial = stateFlow.value.filter())
    }

    fun showLoadingOverlay(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showLoadingOverlay = show, showErrorOverlay = false, showCompleteOverlay = false)) }
    }

    fun updateDuration(duration: Long) {
        stateFlow.set { copy(currentDuration = duration) }
    }

    fun updateTotalDuration(total: Long) {
        stateFlow.set { copy(totalDuration = total) }
    }

    fun updateDraggingProgress(progress: Long) {
        stateFlow.set { copy(draggingProgress = progress) }
    }

    fun updateCurrentItem(item: MediaItem) {
        stateFlow.set {
            copy(currentItem = item,
                currentDuration = 0L,
                totalDuration = 0L,
                draggingProgress = 0L,
                currentIndex = items?.indexOfFirst { it.uri.path.equals(item.uri.path) } ?: 0)
        }
    }

    fun updateCurrentItem(index: Int, currentDuration: Long = 0L, historyDuration: Long = -1L) {
        stateFlow.set {
            copy(
                currentItem = items?.takeIf { it.size > index && index >= 0 }?.get(index),
                currentDuration = currentDuration,
                historyDuration = historyDuration,
                totalDuration = 0L,
                draggingProgress = currentDuration,
                currentIndex = index
            )
        }
    }

    fun showAllControls(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showTopBar = show, showBottomBar = show, showLockButton = show, showPipButton = show)) }
        if (!show) {
            showRateOverlay(false)
            showSerialOverlay(false)
            showScaleOverlay(false)
        }
    }

    fun enterPipMode(enter: Boolean) {
        stateFlow.set {
            copy(
                uiState = uiState.copy(
                    isPictureInPictureMode = enter,
                    showTopBar = false,
                    showBottomBar = false,
                    showPipButton = false,
                    showLockButton = false,
                    showRateOverlay = false,
                    showSerialOverlay = false,
                    showScaleOverlay = false,
                )
            )
        }
    }

    fun showControls(top: Boolean, bottom: Boolean) {
        stateFlow.set {
            copy(
                uiState = uiState.copy(
                    showTopBar = top,
                    showBottomBar = bottom,
                    showLockButton = !uiState.isPortrait && bottom
                )
            )
        }
    }

    private fun showTopBar(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showTopBar = show)) }
    }

    private fun showBottomBar(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showBottomBar = show)) }
    }

    fun isPortrait(portrait: Boolean = true) {
        stateFlow.set {
            copy(
                uiState = uiState.copy(
                    isPortrait = portrait,
                )
            )
        }
    }

    private fun showVolumeIndicator(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showVolumeIndicator = show)) }
    }

    private fun showBrightnessIndicator(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showBrightnessIndicator = show)) }
    }

    fun showRateOverlay(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showRateOverlay = show, showScaleOverlay = false, showSerialOverlay = false)) }
    }

    fun showScaleOverlay(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showScaleOverlay = show, showRateOverlay = false, showSerialOverlay = false)) }
    }

    fun showSerialOverlay(show: Boolean) {
        stateFlow.set {
            copy(
                uiState = uiState.copy(
                    showScaleOverlay = false,
                    showRateOverlay = false,
                    showSerialOverlay = show
                )
            )
        }
    }

    fun showCenterProgressComponent(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showCenterProgress = show)) }
    }

    fun hideGestureComponent() {
        showVolumeIndicator(false)
        showBrightnessIndicator(false)
        showCenterProgressComponent(false)
        showTopBar(false)
        showBottomBar(false)
        showLockButton(false)
        showPipButton(false)
    }

    fun showCompleteOverlay(show: Boolean) {
        stateFlow.set {
            copy(
                isPlaying = false,
                uiState = uiState.copy(showCompleteOverlay = show, enableGesture = false, showLoadingOverlay = false)
            )
        }
    }

    fun showErrorOverlay(show: Boolean) {
        stateFlow.set { copy(isPlaying = false, uiState = uiState.copy(showErrorOverlay = show, showLoadingOverlay = false, enableGesture = false)) }
    }

    fun locked(lock: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(locked = lock)) }
    }

    fun showLockButton(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showLockButton = show)) }
    }

    fun showPipButton(show: Boolean) {
        stateFlow.set { copy(uiState = uiState.copy(showPipButton = show)) }
    }

    fun draggingVolume(volume: Float) {
        stateFlow.set {
            copy(
                currentVolume = volume,
                uiState = uiState.copy(
                    showVolumeIndicator = true,
                    showBrightnessIndicator = false,
                    showCenterProgress = false
                ),
                isDragging = true
            )
        }
    }

    fun draggingBrightness(brightness: Float) {
        stateFlow.set {
            copy(
                currentBrightness = brightness,
                uiState = uiState.copy(
                    showVolumeIndicator = false,
                    showBrightnessIndicator = true,
                    showCenterProgress = false
                ),
                isDragging = true
            )
        }
    }

    fun draggingProgress(progress: Long) {
        stateFlow.set {
            copy(
                draggingProgress = progress,
                historyDuration = 0L,
                uiState = uiState.copy(
                    showVolumeIndicator = false,
                    showBrightnessIndicator = false,
                    showCenterProgress = true
                ), isDragging = true
            )
        }
    }

    fun resetProgress() {
        stateFlow.set { copy(draggingProgress = 0, currentDuration = 0) }
    }

    override fun toString(): String {
        return stateFlow.value.toString()
    }
}

fun <T> MutableStateFlow<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}