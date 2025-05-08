package com.monkey.babyshield.presentations.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.monkey.babyshield.R
import kotlinx.coroutines.launch

@Composable
fun DraggableFloatingIcon(
    iconSize: Dp = 50.dp,
    edgeOffset: Dp = 100.dp,
    iconContent: @Composable () -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthPx = remember { screenWidthDp.value }
    val screenHeightPx = remember { screenHeightDp.value }
    val iconSizePx = remember { iconSize.value }
    val edgeOffsetPx = remember { edgeOffset.value }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf((screenHeightPx - iconSizePx) / 2) }

    val animatedOffsetX = remember { Animatable(offsetX) }
    val animatedOffsetY = remember { Animatable(offsetY) }

    val coroutineScope = rememberCoroutineScope()

    val animationSpec: AnimationSpec<Float> = SpringSpec(dampingRatio = 0.7f, stiffness = 200f)

    Canvas(
        modifier = Modifier
            .size(iconSize)
            .offset {
                Offset(animatedOffsetX.value, animatedOffsetY.value).round()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        // handle drag start if needed
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        // keep the icon within screen bounds during drag
                        offsetX = offsetX.coerceIn(0f, screenWidthPx - iconSizePx)
                        offsetY = offsetY.coerceIn(0f, screenHeightPx - iconSizePx)

                        coroutineScope.launch {
                            animatedOffsetX.snapTo(offsetX)
                            animatedOffsetY.snapTo(offsetY)
                        }
                    },
                    onDragEnd = {
                        val closestX = if (offsetX < screenWidthPx / 2) {
                            edgeOffsetPx
                        } else {
                            screenWidthPx - iconSizePx - edgeOffsetPx
                        }
                        val closestY = offsetY.coerceIn(edgeOffsetPx,
                            screenHeightPx - iconSizePx - edgeOffsetPx
                        )
                        coroutineScope.launch {
                            animatedOffsetX.animateTo(closestX, animationSpec = animationSpec)
                            animatedOffsetY.animateTo(closestY, animationSpec = animationSpec)
                            offsetX = animatedOffsetX.value
                            offsetY = animatedOffsetY.value
                        }
                    }
                )
            }
    ) {
        //iconContent
    }
}

@Composable
fun MyFloatingIconContent(isLocked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(if (isLocked) R.drawable.ic_lock else R.drawable.ic_unlock), contentDescription = "Lock Icon")
    }
}