package com.metroos.launcher.apps

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Browser module: full HTML5/JavaScript WebView with an auto-hiding URL
 * bar. A single tap on the page toggles the chrome; video sites such as
 * YouTube's mobile site play inline with DOM storage and media playback
 * without user-gesture restrictions enabled.
 */
@Composable
fun BrowserApp() {
    val context = LocalContext.current
    var url by remember { mutableStateOf("https://www.youtube.com") }
    var input by remember { mutableStateOf(url) }
    var barVisible by remember { mutableStateOf(true) }

    val webView = remember {
        @SuppressLint("SetJavaScriptEnabled")
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            setOnTouchListener { v, event ->
                // Single tap on the page toggles the auto-hiding URL bar.
                GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapUp(e: android.view.MotionEvent): Boolean {
                            barVisible = !barVisible
                            return false
                        }
                    }
                ).onTouchEvent(event)
                v.performClick()
                false
            }
        }
    }

    DisposableEffect(Unit) {
        webView.onResume()
        onDispose {
            webView.onPause()
            webView.destroy()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF205070E))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .clickable { if (webView.canGoBack()) webView.goBack() }
                        .size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = Color.White,
                    modifier = Modifier
                        .clickable { if (webView.canGoForward()) webView.goForward() }
                        .size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Go",
                    tint = Color.White,
                    modifier = Modifier
                        .clickable {
                            val target = normalizeUrl(input)
                            url = target
                            webView.loadUrl(target)
                        }
                        .size(22.dp)
                )
            }
        }

        AndroidView(
            factory = { webView },
            update = { view ->
                // Only reload when the target actually differs from the live page.
                if (view.url != normalizeUrl(url)) {
                    view.loadUrl(normalizeUrl(url))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

private fun normalizeUrl(raw: String): String = when {
    raw.isBlank() -> "https://www.youtube.com"
    raw.startsWith("http://") || raw.startsWith("https://") -> raw
    else -> "https://$raw"
}
