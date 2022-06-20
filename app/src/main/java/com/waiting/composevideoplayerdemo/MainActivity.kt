package com.waiting.composevideoplayerdemo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.waiting.composevideoplayerdemo.ui.theme.ComposeVideoPlayerDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeVideoPlayerDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    Greeting("")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    val context = LocalContext.current
    Text(text = "播放测试", textAlign = TextAlign.Center, modifier = Modifier
        .padding(50.dp)
        .fillMaxWidth()
        .clickable {
            PlayerActivity.start(context, Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"))
        })
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeVideoPlayerDemoTheme {
        Greeting("Android")
    }
}