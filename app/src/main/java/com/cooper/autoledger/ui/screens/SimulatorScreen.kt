package com.cooper.autoledger.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.ParsedScreenBill
import com.cooper.autoledger.model.TransactionType
import com.cooper.autoledger.ui.SimulatorPreset
import com.cooper.autoledger.ui.SimulatorUiState
import com.cooper.autoledger.ui.components.CategoryAvatar
import com.cooper.autoledger.ui.components.PaymentChannelBadge
import com.cooper.autoledger.ui.theme.ActiveTealBg
import com.cooper.autoledger.ui.theme.ActiveTealBorder
import com.cooper.autoledger.ui.theme.ExpenseRose
import com.cooper.autoledger.ui.theme.IncomeEmerald
import com.cooper.autoledger.ui.theme.WarningAmber
import com.cooper.autoledger.ui.theme.WarningDarkText
import com.cooper.autoledger.ui.theme.WarningSoftBg

@Composable
fun SimulatorScreen(
    state: SimulatorUiState,
    presets: List<SimulatorPreset>,
    onSelectPreset: (Int) -> Unit,
    onUpdatePackage: (String) -> Unit,
    onUpdateNodesText: (String) -> Unit,
    onParseScreen: () -> Unit,
    onSimulateAutoInsert: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Intro Header Card
        item(key = "sim_intro") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "屏幕无障碍节点识别测试台",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "无需真实扫码付款！选择下方预设场景或自由修改节点文本，即可实时验证 ScreenBillParser 的金额提取、商户分类、聊天防误触拦截及 90 秒去重入账全链路。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4 One-Tap Presets
        item(key = "sim_presets") {
            PresetSelectorCard(
                presets = presets,
                selectedIndex = state.selectedPresetIndex,
                onSelectPreset = onSelectPreset
            )
        }

        // Editable Package & Multi-line Node Tree Input + Action Buttons
        item(key = "sim_editor") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "模拟前台窗口节点树",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.packageName,
                        onValueChange = onUpdatePackage,
                        label = { Text("前台应用包名 (packageName)") },
                        placeholder = { Text("com.tencent.mm / com.eg.android.AlipayGphone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.nodeLinesText,
                        onValueChange = onUpdateNodesText,
                        label = { Text("无障碍屏幕文本节点 (每行一个节点文本)") },
                        placeholder = { Text("支付成功\n瑞幸咖啡\n￥18.50") },
                        minLines = 5,
                        maxLines = 9,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onParseScreen,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("解析屏幕文本")
                        }

                        Button(
                            onClick = onSimulateAutoInsert,
                            enabled = state.parsedPreview != null || !state.hasEvaluated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("模拟自动入账")
                        }
                    }
                }
            }
        }

        // Feedback Banner after Simulate Insert
        state.feedbackMessage?.let { message ->
            item(key = "sim_feedback") {
                val isWarn = state.feedbackIsWarning
                val bg = if (isWarn) WarningSoftBg else ActiveTealBg
                val border = if (isWarn) WarningAmber.copy(alpha = 0.45f) else ActiveTealBorder
                val textColor = if (isWarn) WarningDarkText else Color(0xFF065F46)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isWarn) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isWarn) WarningAmber else IncomeEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isWarn) {
                        TextButton(onClick = onNavigateToHome) {
                            Text("看账本", color = IncomeEmerald)
                        }
                    }
                }
            }
        }

        // Live ParsedScreenBill Preview Card
        if (state.hasEvaluated) {
            item(key = "sim_preview") {
                ParsedResultPreviewCard(parsed = state.parsedPreview)
            }
        }
    }
}

@Composable
private fun PresetSelectorCard(
    presets: List<SimulatorPreset>,
    selectedIndex: Int?,
    onSelectPreset: (Int) -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "一键填充真实屏幕场景预设",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            presets.chunked(2).forEachIndexed { rowIdx, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEachIndexed { colIdx, preset ->
                        val actualIndex = rowIdx * 2 + colIdx
                        val selected = selectedIndex == actualIndex
                        val borderColor = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }
                        val bgColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPreset(actualIndex) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = preset.subtitle,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedResultPreviewCard(parsed: ParsedScreenBill?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (parsed == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "防误触安全拦截 · 未识别为支付凭据",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "解析返回值：null\n原因：当前屏幕缺少「支付成功 / 付款成功 / 退款到账」等有效交易完成锚点（或属于普通微信聊天对话/首页余额），系统安全忽略，绝不误记账！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val isExpense = parsed.type == TransactionType.EXPENSE
            val sign = if (isExpense) "-¥" else "+¥"
            val amountColor = if (isExpense) ExpenseRose else IncomeEmerald
            val confidencePct = (parsed.confidence * 100f).toInt().coerceIn(1, 100)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实时解析结果预览 (ParsedScreenBill)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(IncomeEmerald.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "置信度 $confidencePct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryAvatar(category = parsed.suggestedCategory, size = 46.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = parsed.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PaymentChannelBadge(channel = parsed.channel)
                            Text(
                                text = "建议分类：${parsed.suggestedCategory.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "$sign${CurrencyUtils.formatYuan(parsed.amountCents)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "来源包名：${parsed.sourcePkg}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "摘要文本：${parsed.rawTextSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
