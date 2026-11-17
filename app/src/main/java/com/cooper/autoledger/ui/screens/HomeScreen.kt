package com.cooper.autoledger.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import com.cooper.autoledger.ui.DailyBillGroup
import com.cooper.autoledger.ui.LedgerUiState
import com.cooper.autoledger.ui.components.AutoCapturedBadge
import com.cooper.autoledger.ui.components.BillEditorDialog
import com.cooper.autoledger.ui.components.CategoryAvatar
import com.cooper.autoledger.ui.components.PaymentChannelBadge
import com.cooper.autoledger.ui.theme.ActiveTealBg
import com.cooper.autoledger.ui.theme.ActiveTealBorder
import com.cooper.autoledger.ui.theme.ExpenseRose
import com.cooper.autoledger.ui.theme.HeroSlateStart
import com.cooper.autoledger.ui.theme.HeroTealEnd
import com.cooper.autoledger.ui.theme.IncomeEmerald
import com.cooper.autoledger.ui.theme.WarningAmber
import com.cooper.autoledger.ui.theme.WarningDarkText
import com.cooper.autoledger.ui.theme.WarningSoftBg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HomeScreen(
    uiState: LedgerUiState,
    showAddDialog: Boolean,
    onRequestAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onEnableAutoCapture: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onAddManualBill: (
        amountCents: Long,
        type: TransactionType,
        category: Category,
        channel: PaymentChannel,
        merchant: String,
        note: String
    ) -> Unit,
    onUpdateBill: (
        existing: BillRecord,
        amountCents: Long,
        type: TransactionType,
        category: Category,
        channel: PaymentChannel,
        merchant: String,
        note: String
    ) -> Unit,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingRecord by remember { mutableStateOf<BillRecord?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hero_summary") {
            MonthlyHeroSummaryCard(
                monthLabel = uiState.monthLabel,
                isCurrentMonth = uiState.isCurrentMonth,
                expenseCents = uiState.monthlyExpenseCents,
                incomeCents = uiState.monthlyIncomeCents,
                balanceCents = uiState.monthlyBalanceCents,
                autoCapturedCount = uiState.autoCapturedCount,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onJumpToCurrentMonth = onJumpToCurrentMonth
            )
        }

        item(key = "auto_capture_banner") {
            AutoCaptureStatusBanner(
                autoCaptureEnabled = uiState.autoCaptureEnabled,
                accessibilityEnabled = uiState.accessibilityEnabled,
                notificationEnabled = uiState.notificationEnabled,
                onOpenAccessibilitySettings = { openAccessibilitySettingsSafe(context) },
                onEnableMasterSwitch = onEnableAutoCapture,
                onOpenSimulator = onNavigateToSimulator
            )
        }

        if (uiState.dailyGroups.isEmpty()) {
            item(key = "empty_ledger") {
                EmptyLedgerCard(
                    monthLabel = uiState.monthLabel,
                    onOpenSimulator = onNavigateToSimulator,
                    onOpenAddDialog = onRequestAddDialog
                )
            }
        } else {
            uiState.dailyGroups.forEach { group ->
                item(key = "day_${group.date}") {
                    DailyGroupCard(
                        group = group,
                        onClickRecord = { record -> editingRecord = record }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        BillEditorDialog(
            initialRecord = null,
            onDismiss = onDismissAddDialog,
            onSave = { cents, type, category, channel, merchant, note ->
                onAddManualBill(cents, type, category, channel, merchant, note)
                onDismissAddDialog()
            }
        )
    }

    editingRecord?.let { target ->
        BillEditorDialog(
            initialRecord = target,
            onDismiss = { editingRecord = null },
            onSave = { cents, type, category, channel, merchant, note ->
                onUpdateBill(target, cents, type, category, channel, merchant, note)
                editingRecord = null
            },
            onDelete = { id ->
                onDeleteBill(id)
                editingRecord = null
            }
        )
    }
}

@Composable
private fun MonthlyHeroSummaryCard(
    monthLabel: String,
    isCurrentMonth: Boolean,
    expenseCents: Long,
    incomeCents: Long,
    balanceCents: Long,
    autoCapturedCount: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(HeroSlateStart, HeroTealEnd)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Top bar: Month switcher + Auto-capture count pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 2.dp)
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = !isCurrentMonth, onClick = onJumpToCurrentMonth)
                            .padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2DD4BF).copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "自动入账 $autoCapturedCount 笔",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF99F6E4),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "本月支出 (元)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "¥${CurrencyUtils.formatYuan(expenseCents)}",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "本月收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "¥${CurrencyUtils.formatYuan(incomeCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "本月结余",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val balancePrefix = if (balanceCents < 0L) "-¥" else "¥"
                    val absBalance = kotlin.math.abs(balanceCents)
                    Text(
                        text = "$balancePrefix${CurrencyUtils.formatYuan(absBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (balanceCents < 0L) Color(0xFFFDA4AF) else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoCaptureStatusBanner(
    autoCaptureEnabled: Boolean,
    accessibilityEnabled: Boolean,
    notificationEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onEnableMasterSwitch: () -> Unit,
    onOpenSimulator: () -> Unit
) {
    val isActive = autoCaptureEnabled && accessibilityEnabled

    val containerColor = if (isActive) ActiveTealBg else WarningSoftBg
    val borderColor = if (isActive) ActiveTealBorder else WarningAmber.copy(alpha = 0.45f)
    val accentColor = if (isActive) IncomeEmerald else WarningAmber
    val titleColor = if (isActive) Color(0xFF065F46) else WarningDarkText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    !autoCaptureEnabled -> "屏幕自动记账总开关已暂停"
                    accessibilityEnabled -> "无障碍屏幕自动记账运行中"
                    else -> "无障碍屏幕识别尚未开启"
                },
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                fontWeight = FontWeight.Bold
            )
            val subText = when {
                !autoCaptureEnabled -> "恢复开启后可在微信/支付宝支付成功页实时自动入账"
                accessibilityEnabled && notificationEnabled -> "屏幕节点捕获 + 支付通知双通道守护已就绪"
                accessibilityEnabled -> "微信/支付宝支付结果页将自动提取金额与商户入账"
                else -> "一键授权无障碍服务，支付完成即刻自动识别屏幕账单"
            }
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.82f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            !autoCaptureEnabled -> {
                Button(
                    onClick = onEnableMasterSwitch,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("开启", style = MaterialTheme.typography.labelMedium)
                }
            }
            !accessibilityEnabled -> {
                Button(
                    onClick = onOpenAccessibilitySettings,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("去授权", style = MaterialTheme.typography.labelMedium)
                }
            }
            else -> {
                TextButton(
                    onClick = onOpenSimulator,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "测试台",
                        style = MaterialTheme.typography.labelLarge,
                        color = IncomeEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyGroupCard(
    group: DailyBillGroup,
    onClickRecord: (BillRecord) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Day Header & Subtotals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.dateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (group.dayExpenseCents > 0L) {
                        Text(
                            text = "支 ¥${CurrencyUtils.formatYuan(group.dayExpenseCents)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (group.dayIncomeCents > 0L) {
                        Text(
                            text = "收 ¥${CurrencyUtils.formatYuan(group.dayIncomeCents)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IncomeEmerald
                        )
                    }
                }
            }

            group.records.forEachIndexed { index, record ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 14.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                }
                BillRecordRow(
                    record = record,
                    onClick = { onClickRecord(record) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillRecordRow(
    record: BillRecord,
    onClick: () -> Unit
) {
    val timeStr = remember(record.timestampMs) {
        Instant.ofEpochMilli(record.timestampMs)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(TIME_FORMATTER)
    }

    val isExpense = record.type == TransactionType.EXPENSE
    val amountText = (if (isExpense) "-¥" else "+¥") + CurrencyUtils.formatYuan(record.amountCents)
    val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else IncomeEmerald

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(category = record.category, size = 42.dp)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.merchant.ifBlank { record.category.displayName },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$timeStr · ${record.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PaymentChannelBadge(channel = record.channel)
                if (record.autoCaptured) {
                    AutoCapturedBadge()
                }
            }
            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun EmptyLedgerCard(
    monthLabel: String,
    onOpenSimulator: () -> Unit,
    onOpenAddDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¥",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "${monthLabel}暂无账单记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "无需真实付款，可立即前往「识别测试」一键体验微信/支付宝屏幕节点自动识别入账，或手动记一笔。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenSimulator) {
                    Text("体验屏幕识别测试")
                }
                OutlinedButton(onClick = onOpenAddDialog) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("手动记一笔")
                }
            }
        }
    }
}

fun openAccessibilitySettingsSafe(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
