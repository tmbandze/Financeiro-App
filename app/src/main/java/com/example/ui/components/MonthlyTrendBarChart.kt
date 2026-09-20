package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MonthlyTrend
import com.example.utils.Formatters

@Composable
fun MonthlyTrendBarChart(
    monthlyTrends: List<MonthlyTrend>,
    modifier: Modifier = Modifier,
    onMonthSelected: (String) -> Unit = {}
) {
    val animationProgress = remember { Animatable(0f) }
    val maxSpent = remember(monthlyTrends) {
        (monthlyTrends.maxOfOrNull { it.totalAmount } ?: 1000.0).coerceAtLeast(500.0)
    }

    LaunchedEffect(monthlyTrends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_trend_chart_card"),
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
                        text = "Comparativo Mensal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Histórico dos últimos meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Toque para alternar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val unselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            val barBackground = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrends.forEach { trend ->
                    val isSelected = trend.isSelected

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trend_bar_${trend.monthKey}")
                            .clickable { onMonthSelected(trend.monthKey) }
                            .padding(horizontal = 4.dp)
                    ) {
                        // Amount label above bar
                        if (trend.totalAmount > 0) {
                            Text(
                                text = Formatters.formatCurrency(trend.totalAmount)
                                    .replace("R$", "")
                                    .trim(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bar Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                val barWidth = (size.width * 0.65f).coerceAtMost(32.dp.toPx())
                                val left = (size.width - barWidth) / 2f
                                val totalH = size.height

                                // Draw subtle background track
                                drawRoundRect(
                                    color = barBackground,
                                    topLeft = Offset(left, 0f),
                                    size = Size(barWidth, totalH),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )

                                // Draw filled amount bar
                                val fillHeightRatio = if (maxSpent > 0) {
                                    ((trend.totalAmount / maxSpent) * animationProgress.value).toFloat()
                                } else 0f
                                val fillHeightPx = (fillHeightRatio * totalH).coerceAtLeast(if (trend.totalAmount > 0) 8.dp.toPx() else 0f)
                                val top = totalH - fillHeightPx

                                drawRoundRect(
                                    color = if (isSelected) primaryColor else unselectedColor,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, fillHeightPx),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month label
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) primaryColor else Color.Transparent
                        ) {
                            Text(
                                text = trend.shortName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
