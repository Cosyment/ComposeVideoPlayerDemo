package com.github.imovie.ui.widget

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * Copyright (C), 2020-2022, 中传互动（湖北）信息技术有限公司
 * Author: HeChao
 * Date: 2022/1/28 11:39
 * Description:
 */
@Composable
fun LottieAnimation(
    @RawRes rawRes: Int,
    url: String? = null,
    modifier: Modifier = Modifier.width(100.dp).height(100.dp)
) {
    if (rawRes <= 0 && url?.isEmpty() == true) {
        return
    }
    val composition by rememberLottieComposition(
        spec = if (rawRes > 0) LottieCompositionSpec.RawRes(
            rawRes
        ) else LottieCompositionSpec.Url(url = url ?: "")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = modifier,
            contentScale= ContentScale.Crop
        )
    }
}