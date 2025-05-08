package com.monkey.babyshield.presentations.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal fun DefaultWheelMinutesTimePicker(
    modifier: Modifier = Modifier,
    startTime: Int = 0,
    minTime: Int = 0,
    maxTime: Int = Int.MAX_VALUE,
    size: DpSize = DpSize(128.dp, 128.dp),
    rowCount: Int = 3,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onSelectedMinutes: (Int) -> Unit = {}
) {
    val minutes = (minTime..maxTime).map {
        Minute(
            text = it.toString().padStart(2, '0'),
            value = it,
            index = it - minTime
        )
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (selectorProperties.enabled().value) {
            Surface(
                modifier = Modifier.size(size.width, size.height / rowCount),
                shape = selectorProperties.shape().value,
                color = selectorProperties.color().value,
                border = selectorProperties.border().value
            ) {}
        }
        Row{
            //Minute
            val texts = minutes.map { it.text }
            WheelPicker(
                modifier = modifier,
                startIndex = minutes.find { it.value == startTime }?.index ?: 0,
                size = size,
                count = texts.size,
                rowCount = rowCount,
                selectorProperties = selectorProperties,
                onScrollFinished = { snappedIndex ->
                    onSelectedMinutes(minutes[snappedIndex].value)
                    return@WheelPicker snappedIndex
                }
            ) { index ->
                Text(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    text = texts[index],
                    style = textStyle,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

private data class Minute(
    val text: String,
    val value: Int,
    val index: Int
)