package com.monkey.babyshield.presentations.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.monkey.babyshield.R
import com.monkey.babyshield.presentations.theme.BlockedStateRed
import com.monkey.babyshield.presentations.theme.FloatingButtonGreen
import kotlin.math.roundToInt

@Composable
fun FloatingButton(
    isBlocked: Boolean,
    edgeMargin: Int,
    screenWidth: Int,
    screenHeight: Int,
    onDragEnd: (Int, Int) -> Unit,
    onToggleBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 48.dp
    var offsetX by remember { mutableStateOf(screenWidth - buttonSize.value.toInt() - edgeMargin) }
    var offsetY by remember { mutableStateOf(100) }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        // Calculate new position based on closest edge
                        val newX: Int
                        val newY: Int

                        // Find closest horizontal edge
                        if (offsetX < screenWidth / 2) {
                            // Closer to left edge
                            newX = edgeMargin
                        } else {
                            // Closer to right edge
                            newX = screenWidth - buttonSize.value.toInt() - edgeMargin
                        }

                        // Find closest vertical edge
                        if (offsetY < screenHeight / 2) {
                            // Closer to top edge
                            newY = edgeMargin
                        } else {
                            // Closer to bottom edge
                            newY = screenHeight - buttonSize.value.toInt() - edgeMargin
                        }

                        offsetX = newX
                        offsetY = newY
                        onDragEnd(newX, newY)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x.roundToInt()
                        offsetY += dragAmount.y.roundToInt()

                        // Make sure button stays on screen
                        offsetX = offsetX.coerceIn(0, screenWidth - buttonSize.value.toInt())
                        offsetY = offsetY.coerceIn(0, screenHeight - buttonSize.value.toInt())
                    }
                )
            }
    ) {
        IconButton(
            onClick = {
                if (!isDragging) {
                    onToggleBlock()
                }
            },
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(
                    if (isBlocked) BlockedStateRed else FloatingButtonGreen
                )
        ) {
            Icon(
                painter = painterResource(
                    id = if (isBlocked) R.drawable.ic_lock else R.drawable.ic_unlock
                ),
                contentDescription = "Toggle Block",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}