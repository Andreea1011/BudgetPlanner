package com.example.budgetplanner.ui2.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun MonthlyTrendBar(
    months: List<MonthPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White,               // <- white bars
    labelColor: Color = Color.White,             // <- month labels white
    labelSizeSp: Int = 14,                       // <- bigger labels
    maxBarHeightDp: Int = 180
) {
    if (months.isEmpty()) return

    val maxVal = max(1.0, months.maxOf { it.value })
    val chartPadding = 16.dp

    BoxWithConstraints(modifier) {
        // available width inside the left/right padding
        val innerWidth = maxWidth - chartPadding * 2
        // each month takes the same horizontal slot
        val step = innerWidth / months.size
        // make bar width proportional to the slot so it stays centered
        val barWidth = min(step * 0.45f, 22.dp)

        Column {
            // chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxBarHeightDp.dp)
                    .padding(horizontal = chartPadding),
                contentAlignment = Alignment.BottomStart
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepPx = size.width / months.size
                    val barW = barWidth.toPx()

                    months.forEachIndexed { i, p ->
                        val hRatio = (p.value / maxVal).toFloat()
                        val barH = size.height * hRatio
                        val left = i * stepPx + (stepPx - barW) / 2f
                        val top = size.height - barH

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // labels: each label box has exactly the same width as its chart slot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = chartPadding),
                horizontalArrangement = Arrangement.Start
            ) {
                months.forEach { p ->
                    Box(
                        modifier = Modifier.width(step),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.label,
                            color = labelColor,
                            fontSize = labelSizeSp.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
