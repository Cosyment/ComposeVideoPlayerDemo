package com.waiting.videoplayer

import android.net.Uri

/**
 * Author: HeChao
 * Date: 2022/6/14 12:09
 * Description:
 */
interface UriInterceptor {

    fun parseUri(uri: Uri): Uri
}