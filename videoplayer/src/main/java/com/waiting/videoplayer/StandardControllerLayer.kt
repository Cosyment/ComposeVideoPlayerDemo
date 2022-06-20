package com.waiting.videoplayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.imovie.ui.widget.LottieAnimation
import com.google.accompanist.insets.statusBarsHeight
import com.waiting.utils.BatteryUtil
import com.waiting.utils.NetworkUtil
import com.waiting.videoplayer.widget.DefaultThumb
import com.waiting.videoplayer.widget.DefaultTrack
import com.waiting.videoplayer.widget.HorizontalSlider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.ranges.contains


/**
 * @PackageName : com.github.imovie.ui.widget.player
 * @Author : Waiting
 * @Date :   2022/2/12 17:56
 */
private val handler: Handler = Handler(Looper.getMainLooper())

private enum class DragDirection {
    HORIZONTAL,
    VERTICAL
}

@Composable
internal fun StandardControllerLayer(
    state: PlaybackStateDelegate, modifier: Modifier,
    topBarOverlay: @Composable ((PlaybackStateDelegate) -> Unit)? = null,
    bottomBarOverlay: @Composable ((PlaybackStateDelegate) -> Unit)? = null,
    stateOverlay: @Composable ((PlaybackStateDelegate) -> Unit)? = null,
    gestureOverlay: @Composable ((PlaybackStateDelegate) -> Unit)? = null,
    extensionOverlay: @Composable ((PlaybackStateDelegate) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = LocalVideoPlayerController.current
    val orientation = LocalConfiguration.current.orientation

    LaunchedEffect(key1 = orientation, block = {
        state.isPortrait(orientation == Configuration.ORIENTATION_PORTRAIT)
    })

    Box(modifier = modifier.fillMaxSize()) {
        gestureOverlay?.invoke(state)
        topBarOverlay?.invoke(state)
        bottomBarOverlay?.invoke(state)
        stateOverlay?.invoke(state)
        extensionOverlay?.invoke(state)
    }

    startTimedHideTask(state)
    DisposableEffect(controller, lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                controller.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                controller.resume()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                controller.recycler()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            clearTimedHideTask()
        }
    }
}

private suspend fun PointerInputScope.controllerGestures(
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: ((DragDirection) -> Unit)? = null,
    onDrag: ((DragDirection, Float, Offset) -> Unit)? = null,
) {
    var initOffset = Offset.Zero
    coroutineScope {
        launch { detectTapGestures(onTap = onTap, onDoubleTap = onDoubleTap) }

        launch {
            detectHorizontalDragGestures(onDragStart = {
                initOffset = it
                onDragStart?.invoke(it)
            }, onDragEnd = { onDragEnd?.invoke(DragDirection.HORIZONTAL) }, onHorizontalDrag = { change, dragAmount ->
                if (initOffset.y < 100 || size.height - initOffset.y < 100) {
                    return@detectHorizontalDragGestures
                }
                onDrag?.invoke(DragDirection.HORIZONTAL, dragAmount, change.position)
                change.consumePositionChange()
            })
        }

        launch {
            detectVerticalDragGestures(onDragStart = {
                initOffset = it
                onDragStart?.invoke(it)
            }, onDragEnd = { onDragEnd?.invoke(DragDirection.VERTICAL) }, onVerticalDrag = { change, dragAmount ->
                if (initOffset.y < 100 || size.height - initOffset.y < 100) {
                    return@detectVerticalDragGestures
                }
                onDrag?.invoke(DragDirection.VERTICAL, dragAmount, change.position)
                change.consumePositionChange()
            })
        }
    }
}


//基础功能
@Composable
internal fun TopBarOverlay(modifier: Modifier, state: PlaybackStateDelegate, extensionContent: @Composable ColumnScope.() -> Unit) {
    val uiState by state.collect { uiState }
    val titleBarHeight = if (uiState.isPortrait) 40.dp else 45.dp
    AnimatedVisibility(
        visible = uiState.showTopBar,
        enter = expandVertically(), exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(
            modifier = modifier
                .allowInterceptTouchEvent()
                .background(
                    brush = Brush.verticalGradient(
                        arrayListOf(
                            Color.Black.copy(0.5F),
                            Color.Black.copy(0.4F),
                            Color.Black.copy(0.3F),
                            Color.Black.copy(0.2F),
                            Color.Black.copy(0.1F),
                            Color.Black.copy(0F),
                        )
                    )
                )
        ) {
            Spacer(modifier = Modifier.statusBarsHeight())
            extensionContent()
        }
    }

    AnimatedVisibility(
        visible = if (uiState.isPortrait) true else uiState.showTopBar,
        enter = expandVertically(),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(
            modifier = Modifier
                .allowInterceptTouchEvent()
                .padding(
                    start = if (uiState.isPortrait) 0.dp else 20.dp
                )
        ) {
            Spacer(modifier = Modifier.statusBarsHeight())
            Row(
                modifier = Modifier
                    .height(titleBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                IconButton(onClick = {
                    dispatcher?.onBackPressed()
                }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White.copy(0.8F),
                        modifier = Modifier
                            .size(35.dp)
                            .padding(5.dp)
                    )
                }
            }
        }
    }

    val context = LocalContext.current
    BackHandler(enabled = !uiState.isPortrait, onBack = {
        context.let { it as Activity }.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    })
}

@Composable
internal fun TopBarExtensionOverlay(state: PlaybackStateDelegate) {
    val uiState by state.collect { uiState }
    val titleBarHeight = if (uiState.isPortrait) 40.dp else 45.dp
    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(titleBarHeight)
                .padding(
                    start = if (uiState.isPortrait) 0.dp else 65.dp,
                    end = if (uiState.isPortrait) 0.dp else 10.dp
                )
                .fillMaxWidth()
        ) {
//                    标题
            if (uiState.isPortrait) {
                Spacer(modifier = Modifier.weight(1F))
            } else {
                Text(
                    text = state.collect { currentItem?.title }.value ?: "",
                    color = Color.White.copy(0.8F),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1F)
                )
//                        选集
                if ((state.collect { items }.value?.size ?: 0) > 1) {
                    TextButton(onClick = {
                        state.showSerialOverlay(!uiState.showSerialOverlay)
                        if (uiState.showSerialOverlay) {
                            clearTimedHideTask()
                        }
                    }, modifier = Modifier.offset(x = 10.dp)) {
                        Text(
                            text = "选集",
                            color = Color.White.copy(0.8F),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

//                    倍速
            IconButton(
                onClick = {
                    state.showRateOverlay(!uiState.showRateOverlay)
                    if (uiState.showRateOverlay) {
                        clearTimedHideTask()
                    }
                }, modifier = Modifier
                    .size(35.dp)
                    .padding(5.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_speed),
                    contentDescription = "倍速",
                    tint = Color.White.copy(0.8F),
                )
            }

//                    缩放
            IconButton(
                onClick = {
                    state.showScaleOverlay(!uiState.showScaleOverlay)
                    if (uiState.showScaleOverlay) {
                        clearTimedHideTask()
                    }
                },
                modifier = Modifier
                    .size(35.dp)
                    .padding(5.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_real_size),
                    contentDescription = "缩放",
                    tint = Color.White.copy(0.8F),
                )
            }
            Spacer(modifier = Modifier.width(if (uiState.isPortrait) 10.dp else 20.dp))
        }

//                时间电量
        if (!uiState.isPortrait) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
            ) {

                val context = LocalContext.current
                val batteryBean = remember {
                    mutableStateOf(BatteryUtil.getBattery(context))
                }
                val time = SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date())
                Text(text = time, color = Color.White.copy(0.5F), fontSize = 10.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (batteryBean.value?.battery ?: 0.0) {
                        in 0.0..0.2 -> {
                            R.drawable.ic_battery_20
                        }
                        in 0.21..0.4 -> {
                            R.drawable.ic_battery_40
                        }
                        in 0.41..0.6 -> {
                            R.drawable.ic_battery_60
                        }
                        in 0.61..0.9 -> {
                            R.drawable.ic_battery_80
                        }
                        in 0.91..1.0 -> {
                            R.drawable.ic_battery_100
                        }
                        else -> {
                            0
                        }
                    }
                    if (icon > 0) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null, tint = Color.White.copy(0.5F),
                            modifier = Modifier
                                .height(15.dp)
                                .padding(start = 0.dp)
                        )
                    }
                    if (icon > 0 && batteryBean.value?.isCharging() == true) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_battery_recharge),
                            contentDescription = null, tint = Color.White.copy(0.5F),
                            modifier = Modifier
                                .height(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))

                    if (NetworkUtil.isWifiConnected(context)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wifi),
                            contentDescription = null, tint = Color.White.copy(0.5F),
                            modifier = Modifier
                                .height(13.dp)
                                .padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BottomBarOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val controller = LocalVideoPlayerController.current
    val showBottomBar by state.collect { uiState.showBottomBar }
    val isPortrait by state.collect { uiState.isPortrait }
    val currentDuration by state.collect { currentDuration }
    val totalDuration by state.collect { totalDuration }
    val draggingProgress by state.collect { draggingProgress }
    val isDragging by state.collect { isDragging }

    AnimatedVisibility(
        visible = showBottomBar, modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        val horizontalPadding = if (isPortrait) 0.dp else 10.dp
        Row(
            modifier = modifier
                .background(
                    brush = Brush.verticalGradient(
                        arrayListOf(
                            Color.Black.copy(0F),
                            Color.Black.copy(0.1F),
                            Color.Black.copy(0.2F),
                            Color.Black.copy(0.3F),
                            Color.Black.copy(0.4F),
                            Color.Black.copy(0.5F),
                        )
                    )
                )
                .padding(
                    top = 10.dp,
                    bottom = if (isPortrait) 5.dp else 15.dp,
                    start = horizontalPadding,
                    end = horizontalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatVideoTime(currentDuration),
                color = Color.White.copy(0.5F),
                fontSize = 12.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .padding(start = 10.dp, end = 5.dp, bottom = 2.dp)
                    .sizeIn(minWidth = 30.dp, maxWidth = 80.dp)
            )

            val progressAnimation by animateFloatAsState(
                targetValue = 0F.coerceAtLeast(draggingProgress.toFloat()),
                animationSpec = tween(
                    durationMillis = if (isDragging) 0 else 300,
                    easing = FastOutSlowInEasing
                )
            )

            HorizontalSlider(
                value = progressAnimation,
                onValueChange = {
                    state.isDragging = true
                    if (!showBottomBar) {
                        state.showControls(top = false, bottom = true)
                    }
                    state.draggingProgress(progress = it.toLong())
                },
                thumbSizeInDp = DpSize(10.dp, 10.dp),
                onValueChangeFinished = {
                    state.isDragging = false
                    controller.seekTo(draggingProgress)
                    state.showCenterProgressComponent(false)
                    startTimedHideTask(state)
                },
                valueRange = 0f..0F.coerceAtLeast(totalDuration.toFloat()),
                steps = 0,
                modifier = Modifier
                    .weight(1F)
                    .heightIn(min = 2.dp, max = 2.dp),
                track = { p1, p2, p3, p4, p5 ->
                    DefaultTrack(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        colorTrack = Color.LightGray.copy(0.2F),
                        colorProgress = Color.LightGray.copy(0.7F)
                    )
                },
                thumb = { p1, p2, p3, p4, p5 ->
                    DefaultThumb(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        color = Color.LightGray
                    )
                }
            )
            Text(
                text = formatVideoTime(totalDuration),
                color = Color.White.copy(0.5F),
                fontSize = 12.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .padding(
                        start =
                        5.dp, end = 10.dp, bottom = 2.dp
                    )
                    .sizeIn(minWidth = 30.dp, maxWidth = 80.dp)
            )

            val context = LocalContext.current
            IconButton(
                onClick = {
                    context.let {
                        it as Activity
                    }.let {
                        if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                            state.isPortrait(false)
                            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            state.isPortrait()
                            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                }, modifier = Modifier
                    .padding(end = 10.dp)
                    .size(20.dp)
            ) {
                Icon(
                    painterResource(id = if (isPortrait) R.drawable.ic_fullscreen else R.drawable.ic_smallscreen),
                    contentDescription = null,
                    tint = Color.White.copy(0.5F)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = !showBottomBar, modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        val progressAnimation by animateFloatAsState(
            targetValue = currentDuration.toFloat() / totalDuration.toFloat(),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
        LinearProgressIndicator(
            progress = progressAnimation,
            modifier = modifier
                .fillMaxWidth()
                .height(1.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.LightGray.copy(0.5F),
            backgroundColor = Color.DarkGray.copy(0.5F)
        )
    }
}

//手势功能
@Composable
internal fun GestureOverlay(state: PlaybackStateDelegate) {
    val context = LocalContext.current
    val controller = LocalVideoPlayerController.current
    val uiState by state.collect { uiState }
    val draggingProgress by state.collect { draggingProgress }
    val maxVolume by remember {
        mutableStateOf(getMaxVolume(context))
    }
    val currentVolume by state.collect { currentVolume }
    val currentBrightness by state.collect { currentBrightness }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            controllerGestures(
                onTap = {
                    if (uiState.showErrorOverlay) {
                        return@controllerGestures
                    }
                    if (uiState.locked) {
                        state.showLockButton(!uiState.showLockButton)
                        startTimedHideTask(state)
                        return@controllerGestures
                    }
                    if (uiState.showScaleOverlay || uiState.showRateOverlay || uiState.showSerialOverlay) {
                        state.showRateOverlay(false)
                        state.showScaleOverlay(false)
                        state.showSerialOverlay(false)
                    } else {
                        state.showAllControls(show = !uiState.showBottomBar)
                    }
                    startTimedHideTask(state)
                },
                onDoubleTap = {
                    if (uiState.locked || !uiState.enableGesture) {
                        return@controllerGestures
                    }
                    if (uiState.showErrorOverlay) {
                        return@controllerGestures
                    }
                    togglePlay(mediaPlayer = controller)
                },
                onDragEnd = {
                    if (uiState.locked || !uiState.enableGesture)
                        return@controllerGestures
                    if (it == DragDirection.HORIZONTAL) {
                        controller.seekTo(draggingProgress)
                    }
                    state.hideGestureComponent()
                    state.isDragging = false
                },
                onDrag = { direction, dragAmount, offset ->
                    if (uiState.locked || !uiState.enableGesture)
                        return@controllerGestures
                    if (direction == DragDirection.HORIZONTAL) {
                        val length = controller.length
                        val delta = dragAmount / size.width * length + draggingProgress
                        val time = 0f.coerceAtLeast(delta)
                        state.draggingProgress(progress = min(time.toLong(), length))
                    } else {
                        if (offset.x > size.width / 2) {
                            var delta =
                                -dragAmount * 1.5F / size.height * maxVolume + currentVolume
                            if (delta < 0) delta = 0f
                            if (delta > maxVolume) delta = maxVolume.toFloat()
                            state.draggingVolume(delta)
                        } else {
                            val maxBrightness = 1.0f
                            var delta =
                                -dragAmount * 1.5F / size.height * maxBrightness + currentBrightness
                            if (delta < 0) delta = 0F
                            if (delta > maxBrightness) delta = maxBrightness
                            state.draggingBrightness(delta)
                        }
                    }
                })
        }
    ) {
        CenterProgressOverlay(modifier = Modifier.align(Alignment.Center), state)
        BrightnessOverlay(modifier = Modifier.align(Alignment.TopCenter), state)
        VolumeOverlay(modifier = Modifier.align(Alignment.TopCenter), state)
    }
}

@Composable
internal fun ExtensionOverlay(state: PlaybackStateDelegate) {
    Box(modifier = Modifier.fillMaxSize()) {
        val controller = LocalVideoPlayerController.current
        ScaleControllerOverlay(modifier = Modifier.align(Alignment.CenterEnd), state) {
            controller.videoScale = it
        }
        RateControllerOverlay(modifier = Modifier.align(Alignment.CenterEnd), state) {
            controller.rate = it.value
        }
        SerialControllerOverlay(modifier = Modifier.align(Alignment.CenterEnd), state) {
            state.updateCurrentItem(it)
            controller.items?.get(it)?.apply {
                controller.play(this.uri)
            }
        }
        LockControllerOverlay(modifier = Modifier.align(Alignment.CenterStart), state)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun VolumeOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val context = LocalContext.current
    val showVolumeIndicator by state.collect { uiState.showVolumeIndicator }
    val currentVolume by state.collect { currentVolume }

    val maxVolume by remember {
        mutableStateOf(getMaxVolume(context))
    }

    AnimatedVisibility(
        visible = showVolumeIndicator, modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(modifier = modifier.padding(top = 35.dp)) {
            Spacer(modifier = Modifier.statusBarsHeight())
            Row(
                modifier = modifier
                    .background(Color.Black.copy(0.2f), shape = RoundedCornerShape(50))
                    .width(200.dp)
                    .height(30.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val percent = currentVolume / maxVolume
                val icon = when (percent * 100) {
                    0.0f -> R.drawable.ic_volume_off
                    in 0.001..40.0 -> R.drawable.ic_volume_low
                    in 40.0..75.0 -> R.drawable.ic_volume_medium
                    in 75.0..100.0 -> R.drawable.ic_volume_high
                    else -> 0
                }
                AnimatedContent(targetState = icon, transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 200)) with
                            fadeOut(animationSpec = tween(durationMillis = 300))
                }) { targetState ->
                    if (icon > 0) {
                        Icon(
                            painter = painterResource(id = targetState),
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(25.dp)
                                .padding(end = 5.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = percent,
                    color = Color.LightGray,
                    backgroundColor = Color.LightGray.copy(0.2F),
                    modifier = Modifier
                        .height(1.5.dp)
                        .clip(AbsoluteRoundedCornerShape(50))
                )
            }
        }
    }
    changeVolume(LocalContext.current, currentVolume.toInt())
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun BrightnessOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val showBrightnessIndicator by state.collect { uiState.showBrightnessIndicator }
    val currentBrightness by state.collect { currentBrightness }
    AnimatedVisibility(
        visible = showBrightnessIndicator, modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(modifier = modifier.padding(top = 35.dp)) {
            Spacer(
                modifier = Modifier
                    .statusBarsHeight()
            )
            Row(
                modifier = modifier
                    .background(Color.Black.copy(0.2f), shape = RoundedCornerShape(50))
                    .width(200.dp)
                    .height(30.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (currentBrightness) {
                    in 0.0..0.3 -> R.drawable.ic_brightness_low
                    in 0.31..0.6 -> R.drawable.ic_brightness_medium
                    else -> R.drawable.ic_brightness_high
                }
                AnimatedContent(targetState = icon, transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 200)) with
                            fadeOut(animationSpec = tween(durationMillis = 300))
                }) { targetState ->
                    Icon(
                        painter = painterResource(id = targetState),
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier
                            .size(25.dp)
                            .padding(end = 5.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = currentBrightness,
                    color = Color.LightGray,
                    backgroundColor = Color.LightGray.copy(0.2F),
                    modifier = Modifier
                        .height(1.5.dp)
                        .clip(AbsoluteRoundedCornerShape(50))
                )
            }
        }
    }

    changeBrightness(LocalContext.current, currentBrightness)
}

@Composable
internal fun CenterProgressOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val showCenterProgress by state.collect { uiState.showCenterProgress }
    val draggingProgress by state.collect { draggingProgress }
    val totalDuration by state.collect { totalDuration }

    AnimatedVisibility(
        visible = showCenterProgress, modifier = modifier, enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = modifier
                .background(
                    Color.Black.copy(0.2F),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 15.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatVideoTime(if (draggingProgress < 0) 0 else draggingProgress)} / ${
                    formatVideoTime(totalDuration)
                }", color = Color.LightGray, fontSize = 30.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun CenterLoadingOverlay(modifier: Modifier, state: UiState) {
    AnimatedVisibility(
        visible = state.showLoadingOverlay,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LottieAnimation(
            rawRes = R.raw.video_loading
        )
    }
}

@Composable
internal fun CompleteOverlay(modifier: Modifier, state: UiState, completion: (() -> Unit)? = null) {
    AnimatedVisibility(
        visible = state.showCompleteOverlay,
        modifier = modifier
            .allowInterceptTouchEvent(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TextButton(
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black.copy(0.5F)),
            shape = AbsoluteRoundedCornerShape(50),
            onClick = {
//                重播
                completion?.invoke()
            },
            modifier = Modifier
                .background(Color.Black.copy(0.2F), shape = AbsoluteRoundedCornerShape(50))
                .clip(AbsoluteRoundedCornerShape(50))
        ) {
            Icon(
                painterResource(id = R.drawable.ic_replay),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "重新播放",
                color = Color.LightGray,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}

@Composable
internal fun ErrorOverlay(modifier: Modifier, state: UiState, completion: (() -> Unit)? = null) {
    AnimatedVisibility(
        visible = state.showErrorOverlay,
        modifier = modifier.allowInterceptTouchEvent(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Black.copy(
                        0.5F
                    )
                ),
                    shape = AbsoluteRoundedCornerShape(50),
                    onClick = {
//                        播放
                        completion?.invoke()
                    }) {
                    Text(text = "播放出错，请重试", color = Color.LightGray, fontSize = 16.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(id = R.drawable.ic_tips),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary.copy(0.5F),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "可以尝试切换网络后再重试哦~",
                        color = MaterialTheme.colors.onPrimary.copy(0.5F),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun StateOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val controller = LocalVideoPlayerController.current
    val uiState by state.collect { uiState }
    CenterLoadingOverlay(modifier = modifier, state = uiState)
    CompleteOverlay(modifier = modifier, state = uiState) {
        controller.replay()
        state.resetProgress()
    }
    ErrorOverlay(modifier = modifier, state = uiState) {
        controller.replay()
        state.resetProgress()
    }
}

/**********************************************扩展功能**************************************/
@Composable
private fun LockControllerOverlay(modifier: Modifier, state: PlaybackStateDelegate) {
    val uiState by state.collect { uiState }
    var locked by remember {
        mutableStateOf(uiState.locked)
    }
    AnimatedVisibility(
        visible = uiState.showLockButton && !uiState.isPortrait,
        modifier = modifier
            .padding(if (uiState.isPortrait) 5.dp else 30.dp)
            .allowInterceptTouchEvent(),
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        IconButton(onClick = {
            locked = !locked
            if (locked) {
                state.showLockButton(true)
                state.showAllControls(false)
            } else {
                state.showAllControls(true)
                startTimedHideTask(state)
            }

            state.locked(locked)
        }) {
            Icon(
                painterResource(id = if (locked) R.drawable.ic_lock else R.drawable.ic_unlock),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = Color.White.copy(0.8F)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SerialControllerOverlay(
    modifier: Modifier, state: PlaybackStateDelegate, completion: ((Int) -> Unit)? = null,
) {
    val showSerialContainer by state.collect { uiState.showSerialOverlay }
    val items by state.collect { items }
    val currentIndex by state.collect { currentIndex }

    AnimatedVisibility(
        visible = showSerialContainer, modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(4),
            modifier = Modifier
                .allowInterceptTouchEvent()
                .background(Color.Black.copy(0.6f))
                .width(270.dp)
                .fillMaxHeight()
                .padding(top = 20.dp, bottom = 10.dp),
            content = {
                items(items?.size ?: 0) { index ->
                    val isChecked = currentIndex == index
                    Card(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        contentColor = if (isChecked) Color.DarkGray else Color.Gray,
                        backgroundColor = if (isChecked) Color.White.copy(0.6F) else Color.LightGray.copy(
                            0.3F
                        ),
                        elevation = 0.dp
                    ) {
                        Column(
                            Modifier
                                .height(35.dp)
                                .width(35.dp)
                                .clickable {
                                    state.showSerialOverlay(false)
                                    completion?.invoke(index)
                                    startTimedHideTask(state)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            })
    }
}

@Composable
private fun ScaleControllerOverlay(modifier: Modifier, state: PlaybackStateDelegate, completion: ((VideoScale) -> Unit)? = null) {
    val showScaleContainer by state.collect { uiState.showScaleOverlay }
    val videoScaleList by remember {
        mutableStateOf(listOf(
            VideoScale.SURFACE_BEST_FIT,
            VideoScale.SURFACE_FIT_SCREEN,
            VideoScale.SURFACE_FILL,
            VideoScale.SURFACE_16_9,
            VideoScale.SURFACE_4_3,
            VideoScale.SURFACE_ORIGINAL
        ))
    }
    val checkedIndex = remember {
        mutableStateOf(0)
    }

    AnimatedVisibility(
        visible = showScaleContainer, modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        LazyColumn(
            modifier = Modifier
                .allowInterceptTouchEvent()
                .background(Color.Black.copy(0.5f))
                .width(110.dp)
                .padding(
                    top = if (state.collect { isPlaying }.value) 40.dp else 20.dp,
                    bottom = 20.dp,
                    start = 10.dp,
                    end = 10.dp
                )
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            items(videoScaleList.size) { index ->
                CustomOptionItem(
                    title = videoScaleList[index].description,
                    checkedIndex.value == index
                ) {
                    completion?.invoke(videoScaleList[index])
                    state.showScaleOverlay(false)
                    checkedIndex.value = index
                    startTimedHideTask(state)
                }
            }
        }
    }
}

@Composable
private fun RateControllerOverlay(modifier: Modifier, state: PlaybackStateDelegate, completion: ((VideoRate) -> Unit)? = null) {
    val showRateContainer by state.collect { uiState.showRateOverlay }
    val videoRateList by remember {
        mutableStateOf(listOf(
            VideoRate.RATE_0_5,
            VideoRate.RATE_1_0,
            VideoRate.RATE_1_5,
            VideoRate.RATE_1_75,
            VideoRate.RATE_2_0
        ))
    }
    val checkedIndex = remember {
        mutableStateOf(1)
    }
    AnimatedVisibility(
        visible = showRateContainer, modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        LazyColumn(
            modifier = Modifier
                .allowInterceptTouchEvent()
                .background(Color.Black.copy(0.5f))
                .width(110.dp)
                .padding(
                    top = if (state.collect { uiState.isPortrait }.value) 40.dp else 20.dp,
                    bottom = 20.dp,
                    start = 10.dp,
                    end = 10.dp
                )
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            items(videoRateList.size) { index ->
                CustomOptionItem(
                    videoRateList[index].description,
                    checkedIndex.value == index
                ) {
                    completion?.invoke(videoRateList[index])
                    state.showRateOverlay(false)
                    checkedIndex.value = index
                    startTimedHideTask(state)
                }
            }
        }
    }
}

@Composable
private fun CustomOptionItem(title: String, isChecked: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = { onClick() },
        Modifier
            .height(40.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            color = Color.White.copy(0.5F),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = Color.White.copy(if (isChecked) 0.5f else 0f)
        )
    }
}

/**********************************************扩展功能**************************************/

private fun togglePlay(mediaPlayer: AbstractVideoPlayerController) {
    if (mediaPlayer.isPlaying) {
        mediaPlayer.pause()
    } else {
        mediaPlayer.resume()
    }
}

private fun startTimedHideTask(state: PlaybackStateDelegate) {
    clearTimedHideTask()
    handler.postDelayed({
        state.hideGestureComponent()
    }, 3500)
}

private fun clearTimedHideTask() {
    handler.removeCallbacksAndMessages(null)
}

private fun getMaxVolume(context: Context): Int {
    val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
}

private fun changeVolume(context: Context, volume: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        volume,
        0
    )
}

private fun changeBrightness(context: Context, brightness: Float) {
    val activity = context as Activity
    val attributes = activity.window.attributes
    attributes.screenBrightness = brightness
    activity.window.attributes = attributes
}

private fun formatVideoTime(time: Long): String {
    val totalSecond = time / 1000
    val second = totalSecond % 60
    val minute = (totalSecond / 60) % 60
    val hour = totalSecond / 3600
    val stringBuilder = StringBuilder()
    val formatter = Formatter(stringBuilder, Locale.CHINA)
    return if (hour > 0) {
        formatter.format("%d:%02d:%02d", hour, minute, second).toString()
    } else {
        formatter.format("%02d:%02d", minute, second).toString()
    }
}

//允许子元素拦截父元素touch事件
private fun Modifier.allowInterceptTouchEvent(): Modifier {
    return this.pointerInput(Unit) {
        detectTransformGestures { _, _, _, _ -> }
    }
}
