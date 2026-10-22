package com.metroos.launcher.shell

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.data.RecentApp
import com.metroos.launcher.data.RecentsStore
import com.metroos.launcher.util.Springs
import com.metroos.launcher.util.velocityDamping
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * MeeGo task switcher: horizontal card deck. Drag a card up/down to
 * dismiss it with spring physics, tap to resume, swipe between cards.
 */
@Composable
fun TaskSwitcherScreen(
    onDismiss: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var recents by remember { mutableStateOf(RecentsStore.all()) }

    DisposableEffect(Unit) {
        val listener = { version++; recents = RecentsStore.all() }
        RecentsStore.addListener(listener)
        onDispose { RecentsStore.removeListener(listener) }
    }
    @Suppress("UNUSED_EXPRESSION")
    version

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE05070E))
            .padding(top = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(6.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Task switcher",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier
                    .clickable(onClick = onHome)
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        if (recents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No recent apps.\nLaunch something from the home screen.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(recents, key = { it.id }) { app ->
                    RecentCard(
                        app = app,
                        onDismiss = { RecentsStore.remove(app.id) },
                        onResume = {
                            app.launchIntent?.let { intent ->
                                runCatching {
                                    context.startActivity(
                                        intent.addFlags(
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        )
                                    )
                                }
                            }
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    app: RecentApp,
    onDismiss: () -> Unit,
    onResume: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .padding(vertical = 24.dp)
            .graphicsLayer { translationY = offsetY.value }
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(400.dp)
                .background(
                    Color.White.copy(alpha = if (dismissed) 0.03f else 0.14f),
                    RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onResume)
                .pointerInput(Unit) {
                    // Vertical fling dismiss with velocity-tuned spring settle.
                    var acc = 0f
                    detectVerticalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragCancel = { acc = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            acc += dragAmount
                            change.consume()
                            scope.launch { offsetY.snapTo(offsetY.value + dragAmount) }
                        },
                        onDragEnd = {
                            if (abs(acc) > 180f) {
                                dismissed = true
                                scope.launch {
                                    offsetY.animateTo(
                                        -1400f,
                                        spring(
                                            dampingRatio = velocityDamping(abs(acc)),
                                            stiffness = 260f
                                        )
                                    )
                                    onDismiss()
                                }
                            } else {
                                scope.launch { offsetY.animateTo(0f, Springs.fast()) }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.label.take(1),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(app.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(30.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}
