package com.waiting.videoplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Author: HeChao
 * Date: 2022/1/28 11:29
 * Description:
 */
sealed class PlayerEvent(val state: Int, val description: String) {
    object IDLE : PlayerEvent(0, "空闲")
    object Prepare : PlayerEvent(1, "准备播放")
    object Buffering : PlayerEvent(2, "正在缓冲")
    object Playing : PlayerEvent(3, "播放中")
    data class Changed(val value: Long) : PlayerEvent(4, "播放中")
    object Paused : PlayerEvent(5, "已暂停")
    object Stopped : PlayerEvent(6, "停止")
    object Error : PlayerEvent(7, "错误")
    object Completed : PlayerEvent(8, "播放完成")
}

@Parcelize
data class PlayerState(
    val isPlaying: Boolean = false,
    val isDragging: Boolean = false,
    val draggingProgress: Long = 0L,
    val currentDuration: Long = 0L,
    val historyDuration: Long = 0L,
    val totalDuration: Long = 0L,
    val currentIndex: Int = 0,
    val currentItem: MediaItem? = null,
    val items: List<MediaItem>? = null,
    val currentVolume: Float = 0.0F,
    val currentBrightness: Float = 0.0F,
    val rate: Float = 1.0F,
    val autoEnterPictureInPicture: Boolean = false,
    val autoPlayNext: Boolean = true,
    val continuation: Boolean = true,
    val uiState: UiState = UiState(),
) : Parcelable

@Parcelize
data class UiState(
    val isPortrait: Boolean = true,
    val locked: Boolean = false,
    val enableGesture: Boolean = true,
    val isPictureInPictureMode: Boolean = false,
    val showLoadingOverlay: Boolean = false,
    val showTopBar: Boolean = false,
    val showBottomBar: Boolean = false,
    val showCenterProgress: Boolean = false,
    val showLockButton: Boolean = false,
    val showPipButton: Boolean = true,
    val showRateOverlay: Boolean = false,
    val showScaleOverlay: Boolean = false,
    val showSerialOverlay: Boolean = false,
    val showVolumeIndicator: Boolean = false,
    val showBrightnessIndicator: Boolean = false,
    val showCompleteOverlay: Boolean = false,
    val showErrorOverlay: Boolean = false,
) : Parcelable

@Parcelize
data class MediaItem(val title: String, val uri: Uri) : Parcelable


