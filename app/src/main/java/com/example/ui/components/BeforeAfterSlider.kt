package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBg
import com.example.ui.theme.PrimaryNeon

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    initialSplit: Float = 0.5f,
    showLabels: Boolean = true
) {
    var splitRatio by remember { mutableStateOf(initialSplit) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedSplit by animateFloatAsState(
        targetValue = splitRatio,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "splitAnimation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1AFFFFFF))
            .border(
                1.dp,
                BorderGlass,
                RoundedCornerShape(24.dp)
            )
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (containerSize.width > 0) {
                        val newRatio = (change.position.x / containerSize.width).coerceIn(0.05f, 0.95f)
                        splitRatio = newRatio
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (containerSize.width > 0) {
                        splitRatio = (offset.x / containerSize.width).coerceIn(0.05f, 0.95f)
                    }
                }
            }
            .testTag("before_after_slider_box"),
        contentAlignment = Alignment.Center
    ) {
        if (beforeBitmap != null && afterBitmap != null) {
            val beforeImageBmp = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
            val afterImageBmp = remember(afterBitmap) { afterBitmap.asImageBitmap() }

            // Custom canvas rendering for seamless clipRect split
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val splitX = canvasW * animatedSplit

                // 1. Draw Before Image on the left half
                clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                    drawImage(
                        image = beforeImageBmp,
                        dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                        srcSize = IntSize(beforeImageBmp.width, beforeImageBmp.height)
                    )
                }

                // 2. Draw After Image on the right half
                clipRect(left = splitX, top = 0f, right = canvasW, bottom = canvasH) {
                    drawImage(
                        image = afterImageBmp,
                        dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                        srcSize = IntSize(afterImageBmp.width, afterImageBmp.height)
                    )
                }

                // 3. Divider Line
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, canvasH),
                    strokeWidth = 4.dp.toPx()
                )
            }

            // Draggable Central Knob
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                val knobOffset = if (containerSize.width > 0) (containerSize.width * animatedSplit) - 20.dp.value else 0f

                Box(
                    modifier = Modifier
                        .offset(x = knobOffset.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(
                            1.5.dp,
                            Color.White,
                            CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Geser untuk membandingkan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Labels for Asli vs AI
            if (showLabels) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC0D0D0F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "👈 Foto Asli",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xD94F46E5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Tukar Wajah AI 👉",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        } else if (afterBitmap != null) {
            Image(
                bitmap = afterBitmap.asImageBitmap(),
                contentDescription = "Hasil Face Swap",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (beforeBitmap != null) {
            Image(
                bitmap = beforeBitmap.asImageBitmap(),
                contentDescription = "Foto Target",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih foto target & sumber untuk memulai",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}
