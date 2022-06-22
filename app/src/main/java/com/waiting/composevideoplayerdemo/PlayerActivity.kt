package com.waiting.composevideoplayerdemo

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.waiting.videoplayer.*


/**
 * Author: HeChao
 * Date: 2022/6/20 16:35
 * Description:
 */
class PlayerActivity : ComponentActivity() {
    companion object {
        fun start(context: Context, uri: Uri) {
            context.startActivity(
                Intent(
                    context,
                    PlayerActivity::class.java
                ).putExtra("uri", uri)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } catch (e: NoSuchFieldError) {
            e.printStackTrace()
        }

        val uri = intent.getParcelableExtra<Uri>("uri")

        setContent {
            ProvideWindowInsets {
                rememberSystemUiController().setStatusBarColor(Color.Black)

                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    uri?.let { VideoComponent(it) }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        stateDelegate?.enterPipMode(isInPictureInPictureMode)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        stateDelegate?.apply {
            //Android 12以下自动进入画中画
            if (this.isAutoEnterPipMode && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                enterPipMode(true)
                enterPictureInPictureMode()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            controller?.hide(WindowInsetsCompat.Type.navigationBars())
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            controller?.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
private var stateDelegate by mutableStateOf<PlaybackStateDelegate?>(null)

@Composable
fun VideoComponent(uri: Uri) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    Box(modifier = Modifier
        .height(if (isPortrait) 250.dp else Resources.getSystem().displayMetrics.heightPixels.dp)
        .background(Color.Black)
    ) {
        val controller = rememberVideoPlayerController()
        stateDelegate = rememberUiControllerState(controller = controller).apply {
            VideoPlayer(controller = controller,
                state = this.apply {
                    isAutoEnterPipMode =true
                })
        }

        LaunchedEffect(key1 = controller, uri, block = {
            MediaItem("测试", uri).apply {
                controller.currentItem = this
                controller.play(this.uri)
            }
        })
    }
}