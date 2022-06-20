package com.waiting.videoplayer

import org.videolan.libvlc.MediaPlayer

/**
 * Author: HeChao
 * Date: 2022/3/18 9:45
 * Description:
 */
sealed class VideoScale(val value: MediaPlayer.ScaleType, val description: String) {
    object SURFACE_BEST_FIT : VideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT, "最佳")
    object SURFACE_FIT_SCREEN : VideoScale(MediaPlayer.ScaleType.SURFACE_FIT_SCREEN, "拉伸")
    object SURFACE_FILL : VideoScale(MediaPlayer.ScaleType.SURFACE_FILL, "铺满")
    object SURFACE_16_9 : VideoScale(MediaPlayer.ScaleType.SURFACE_16_9, "16:9")

    //    object SURFACE_16_10 : VideoScale(MediaPlayer.ScaleType.SURFACE_16_10, "16:10")
    object SURFACE_4_3 : VideoScale(MediaPlayer.ScaleType.SURFACE_4_3, "4:3")

    //    object SURFACE_221_1 : VideoScale(MediaPlayer.ScaleType.SURFACE_221_1, "2.21:1")
//    object SURFACE_235_1 : VideoScale(MediaPlayer.ScaleType.SURFACE_235_1, "2.35:1")
//    object SURFACE_5_4 : VideoScale(MediaPlayer.ScaleType.SURFACE_5_4, "5:4")
    object SURFACE_ORIGINAL : VideoScale(MediaPlayer.ScaleType.SURFACE_ORIGINAL, "原始")
}

sealed class VideoRate(val value: Float, val description: String) {
    object RATE_0_5 : VideoRate(0.5F, "0.5X")
    object RATE_1_0 : VideoRate(1.0F, "正常")
    object RATE_1_5 : VideoRate(1.5F, "1.5X")
    object RATE_1_75 : VideoRate(1.75F, "1.75X")
    object RATE_2_0 : VideoRate(2.0F, "2.0X")
}