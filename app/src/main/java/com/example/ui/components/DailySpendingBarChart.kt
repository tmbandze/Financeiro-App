package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.DailySpending
import com.example.utils.Formatters

@Composable
fun DailySpendingBarChart(
    dailySpending: List<DailySpending>,
    dailyAverage: Double,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedDaySpending by remember { mutableStateOf<DailySpending?>(null) }
    val animationProgress = remember { Animatable(0f) }

    val maxDailyAmount = remember(dailySpending) {
        (dailySpending.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(100.0)
    }

    LaunchedEffect(dailySpending) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_spending_chart_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Evolução Diária de Gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Média diária: ${Formatters.formatCurrency(dailyAverage)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedDaySpending != null) {
                    Text(
                        text = "Dia ${selectedDaySpending!!.day}: ${Formatters.formatCurrency(selectedDaySpending!!.amount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Bar Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val highlightColor = MaterialTheme.colorScheme.tertiary
            val emptyBarColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val avgLineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(bottom = 24.dp)
                ) {
                    val barWidth = 14.dp
                    val barSpacing = 8.dp
                    val chartHeight = 130.dp

                    Canvas(
                        modifier = Modifier
                            .width((barWidth + barSpacing) * dailySpending.size + 16.dp)
                            .height(chartHeight)
                            .pointerInput(dailySpending) {
                                detectTapGestures { offset ->
                                    val stepPx = (barWidth + barSpacing).toPx()
                                    val index = (offset.x / stepPx).toInt()
                                    if (index in dailySpending.indices) {
                                        selectedDaySpending = dailySpending[index]
                                    }
                                }
                            }
                    ) {
                        val barWidthPx = barWidth.toPx()
                        val barSpacingPx = barSpacing.toPx()
                        val availableHeight = size.height

                        // Draw average dashed line
                        if (dailyAverage > 0 && maxDailyAmount > 0) {
                            val avgY = availableHeight - ((dailyAverage / maxDailyAmount) * availableHeight).toFloat()
                            drawLine(
                                color = avgLineColor,
                                start = Offset(0f, avgY),
                                end = Offset(size.width, avgY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Draw Bars
                        dailySpending.forEachIndexed { index, item ->
                            val left = index * (barWidthPx + barSpacingPx) + 8.dp.toPx()
                            val barHeightRatio = if (maxDailyAmount > 0) {
                                ((item.amount / maxDailyAmount) * animationProgress.value).toFloat()
                            } else 0f
                            val barHeightPx = (barHeightRatio * availableHeight).coerceAtLeast(if (item.amount > 0) 8.dp.toPx() else 4.dp.toPx())
                            val top = availableHeight - barHeightPx

                            val isSelected = selectedDaySpending?.day == item.day
                            val barColor = when {
                                isSelected -> highlightColor
                                item.amount >= dailyAverage * 1.5 && item.amount > 0 -> highlightColor
                                item.amount > 0 -> primaryColor
                                else -> emptyBarColor
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidthPx, barHeightPx),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Days labels row below
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .horizontalScroll(scrollState)
                ) {
                    val barWidth = 14.dp
                    val barSpacing = 8.dp
                    dailySpending.forEach { item ->
                        Box(
                            modifier = Modifier.width(barWidth + barSpacing),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.day % 5 == 1 || item.day == dailySpending.size) {
                                Text(
                                    text = item.day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Toque em uma barra para detalhes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(avgLineColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Média",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
