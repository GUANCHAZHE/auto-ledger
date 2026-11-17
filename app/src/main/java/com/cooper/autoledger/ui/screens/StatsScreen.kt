package com.cooper.autoledger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.TransactionType
import com.cooper.autoledger.ui.CategoryStat
import com.cooper.autoledger.ui.DailyTrendPoint
import com.cooper.autoledger.ui.LedgerUiState
import com.cooper.autoledger.ui.components.CategoryAvatar
import com.cooper.autoledger.ui.components.accentColor
import com.cooper.autoledger.ui.theme.ExpenseRose
import com.cooper.autoledger.ui.theme.IncomeEmerald
import java.util.Locale
import kotlin.math.max

@Composable
fun StatsScreen(
    uiState: LedgerUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val isExpense = activeType == TransactionType.EXPENSE

    val categoryStats = if (isExpense) {
        uiState.expenseCategoryStats
    } else {
        uiState.incomeCategoryStats
    }
    val totalCents = if (isExpense) {
        uiState.monthlyExpenseCents
    } else {
        uiState.monthlyIncomeCents
    }
    val trendPoints = if (isExpense) {
        uiState.last7DaysExpenseTrend
    } else {
        uiState.last7DaysIncomeTrend
    }
    val totalTxCount = categoryStats.sumOf { it.count }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month + Expense/Income Switcher Header
        item(key = "stats_header") {
            StatsControlHeader(
                monthLabel = uiState.monthLabel,
                isCurrentMonth = uiState.isCurrentMonth,
                activeType = activeType,
                onSelectType = { activeType = it },
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onJumpToCurrentMonth = onJumpToCurrentMonth
            )
        }

        // Donut Chart Card
        item(key = "donut_chart") {
            CategoryDonutCard(
                isExpense = isExpense,
                totalCents = totalCents,
                totalTxCount = totalTxCount,
                stats = categoryStats
            )
        }

        // 7-Day Trend Bar Chart Card
        item(key = "trend_bar_chart") {
            SevenDayTrendCard(
                isExpense = isExpense,
                points = trendPoints
            )
        }

        // Ranked Category Breakdown List
        item(key = "ranked_categories") {
            CategoryRankingCard(
                isExpense = isExpense,
                stats = categoryStats
            )
        }
    }
}

@Composable
private fun StatsControlHeader(
    monthLabel: String,
    isCurrentMonth: Boolean,
    activeType: TransactionType,
    onSelectType: (TransactionType) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月"
                )
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = !isCurrentMonth, onClick = onJumpToCurrentMonth)
                    .padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月"
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TypePillButton(
                label = "支出构成",
                selected = activeType == TransactionType.EXPENSE,
                activeColor = ExpenseRose,
                onClick = { onSelectType(TransactionType.EXPENSE) }
            )
            TypePillButton(
                label = "收入构成",
                selected = activeType == TransactionType.INCOME,
                activeColor = IncomeEmerald,
                onClick = { onSelectType(TransactionType.INCOME) }
            )
        }
    }
}

@Composable
private fun TypePillButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDonutCard(
    isExpense: Boolean,
    totalCents: Long,
    totalTxCount: Int,
    stats: List<CategoryStat>
) {
    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpense) "分类支出占比" else "分类收入占比",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 $totalTxCount 笔",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Native Compose Canvas Donut Chart
            Box(
                modifier = Modifier.size(204.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 28.dp.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    if (stats.isEmpty() || totalCents <= 0L) {
                        drawArc(
                            color = emptyRingColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx)
                        )
                    } else if (stats.size == 1) {
                        drawArc(
                            color = stats.first().category.accentColor(),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                        )
                    } else {
                        val gapDegrees = 2.4f
                        val totalGap = gapDegrees * stats.size
                        val drawableDegrees = (360f - totalGap).coerceAtLeast(180f)
                        var currentStart = -90f

                        stats.forEach { item ->
                            val sweep = max(2f, item.ratio * drawableDegrees)
                            drawArc(
                                color = item.category.accentColor(),
                                startAngle = currentStart,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                            )
                            currentStart += sweep + gapDegrees
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isExpense) "总支出" else "总收入",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "¥${CurrencyUtils.formatYuan(totalCents)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.take(6).forEach { stat ->
                        val pct = String.format(Locale.US, "%.1f%%", stat.ratio * 100f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(stat.category.accentColor())
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${stat.category.displayName} $pct",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SevenDayTrendCard(
    isExpense: Boolean,
    points: List<DailyTrendPoint>
) {
    val barColor = if (isExpense) MaterialTheme.colorScheme.primary else IncomeEmerald
    val highlightColor = if (isExpense) ExpenseRose else IncomeEmerald
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val emptySlotColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)

    val maxAmount = points.maxOfOrNull { it.amountCents } ?: 0L
    val sevenDayTotal = points.sumOf { it.amountCents }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpense) "近 7 日支出趋势" else "近 7 日收入趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "合计 ¥${CurrencyUtils.formatYuan(sevenDayTotal)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount value labels above bars
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { pt ->
                        Text(
                            text = if (pt.amountCents > 0L) {
                                formatCompactYuan(pt.amountCents)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pt.amountCents == maxAmount && maxAmount > 0L) {
                                highlightColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Native Compose Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                if (points.isEmpty()) return@Canvas

                val count = points.size
                val slotWidth = size.width / count.toFloat()
                val barWidth = (slotWidth * 0.46f).coerceAtMost(28.dp.toPx())
                val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                // Subtle horizontal baseline & mid-line
                for (i in 0..2) {
                    val y = size.height * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                points.forEachIndexed { index, pt ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    // Background track
                    drawRoundRect(
                        color = emptySlotColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = corner
                    )

                    if (pt.amountCents > 0L && maxAmount > 0L) {
                        val ratio = (pt.amountCents.toFloat() / maxAmount.toFloat())
                            .coerceIn(0.08f, 1f)
                        val barHeight = size.height * ratio
                        val top = size.height - barHeight
                        val fill = if (pt.amountCents == maxAmount) highlightColor else barColor
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Date Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { pt ->
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (pt.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (pt.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRankingCard(
    isExpense: Boolean,
    stats: List<CategoryStat>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = if (isExpense) "分类支出排行" else "分类收入排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "当前月份暂无${if (isExpense) "支出" else "收入"}记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                stats.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                    CategoryRankRow(stat = item)
                }
            }
        }
    }
}

@Composable
private fun CategoryRankRow(stat: CategoryStat) {
    val accent = stat.category.accentColor()
    val pctText = String.format(Locale.US, "%.1f%%", stat.ratio * 100f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(category = stat.category, size = 38.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stat.category.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${stat.count}笔 · $pctText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "¥${CurrencyUtils.formatYuan(stat.amountCents)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.ratio.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
            }
        }
    }
}

private fun formatCompactYuan(amountCents: Long): String {
    val yuan = amountCents / 100.0
    return when {
        yuan >= 1000.0 -> String.format(Locale.US, "%.1fk", yuan / 1000.0)
        amountCents % 100L == 0L -> (amountCents / 100L).toString()
        else -> String.format(Locale.US, "%.1f", yuan)
    }
}
