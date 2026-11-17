package com.cooper.autoledger.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.autoledger.ui.LedgerUiState
import com.cooper.autoledger.ui.theme.IncomeEmerald
import com.cooper.autoledger.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    uiState: LedgerUiState,
    onToggleAutoCapture: (Boolean) -> Unit,
    onRefreshPermissions: () -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        val msg = if (granted) {
            "记账结果通知权限已开启"
        } else {
            "通知权限未授予，可在系统应用设置中手动开启"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Switch Card
        item(key = "master_switch") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "屏幕自动记账总开关",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "开启后，检测到微信/支付宝/云闪付支付完成页面或支付通知时自动解析入账（90 秒内智能防重）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.autoCaptureEnabled,
                        onCheckedChange = onToggleAutoCapture
                    )
                }
            }
        }

        // Section Header for Permissions
        item(key = "perm_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "系统服务与权限授权",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onRefreshPermissions,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新状态",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 1. Accessibility Service Card
        item(key = "perm_accessibility") {
            PermissionStatusCard(
                title = "1. 无障碍屏幕捕获服务（核心）",
                description = "实时读取微信支付成功页、支付宝账单结果页的金额与收款商户节点。自动跳过本应用与普通聊天页面。",
                isEnabled = uiState.accessibilityEnabled,
                enabledLabel = "运行中",
                disabledLabel = "未开启",
                buttonText = if (uiState.accessibilityEnabled) "前往无障碍设置查看" else "去开启无障碍服务",
                extraTip = "Android 13+ 侧载安装提示：若列表中开关置灰提示「受限制的设置」，请先进入系统「设置 → 应用 → 自动记账 → 右上角 ⋮ → 允许受限制的设置」，再返回开启无障碍。",
                onActionClick = { openAccessibilitySettingsSafe(context) }
            )
        }

        // 2. Notification Listener Service Card
        item(key = "perm_notification_listener") {
            PermissionStatusCard(
                title = "2. 支付通知辅助监听（双通道兜底）",
                description = "监听微信支付凭证、支付宝交易提醒及银行动账通知，熄屏或秒关支付页面时依然不漏记。",
                isEnabled = uiState.notificationEnabled,
                enabledLabel = "已授权",
                disabledLabel = "未授权",
                buttonText = if (uiState.notificationEnabled) "管理通知使用权" else "去授权通知使用权",
                onActionClick = { openNotificationListenerSettingsSafe(context) }
            )
        }

        // 3. Android 13+ POST_NOTIFICATIONS Runtime Permission Card
        item(key = "perm_post_notifications") {
            val isTiramisuOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            PermissionStatusCard(
                title = "3. 自动记账结果通知提醒",
                description = "每当自动捕获一笔账单时弹出通知横幅，并附带「撤销该笔」快捷操作按钮。",
                isEnabled = uiState.postNotificationsGranted,
                enabledLabel = "已允许",
                disabledLabel = "未允许",
                buttonText = when {
                    !isTiramisuOrAbove -> "系统默认已开启 (管理通知)"
                    !uiState.postNotificationsGranted -> "一键申请通知权限"
                    else -> "前往应用通知设置"
                },
                onActionClick = {
                    if (isTiramisuOrAbove && !uiState.postNotificationsGranted) {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettingsSafe(context)
                    }
                }
            )
        }

        // CSV Export & Data Management Card
        item(key = "data_export") {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "账单数据导出",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "累计 ${uiState.totalAllTimeCount} 笔账单",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "将本地 SQLite 数据库中的全部账单导出为标准 UTF-8 CSV 表格文本，可通过微信、邮件、云盘或记事本保存与分析。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onExportCsv { csvContent ->
                                shareCsvBill(context, csvContent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("导出 CSV 账单")
                    }
                }
            }
        }

        // Local Privacy Guarantee Card
        item(key = "privacy_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            .size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "零联网 · 纯本地隐私安全架构",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "本应用未申请 INTERNET 网络权限，所有屏幕节点识别与账单持久化均 100% 在本机内存中与本地 SQLite 数据库完成，绝不上传任何个人消费隐私。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    enabledLabel: String,
    disabledLabel: String,
    buttonText: String,
    extraTip: String? = null,
    onActionClick: () -> Unit
) {
    val badgeColor = if (isEnabled) IncomeEmerald else WarningAmber

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEnabled) enabledLabel else disabledLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (extraTip != null && !isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = extraTip,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEnabled) {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            } else {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonText)
                }
            }
        }
    }
}

private fun openNotificationListenerSettingsSafe(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
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

private fun openAppNotificationSettingsSafe(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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

private fun shareCsvBill(context: Context, csvContent: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "自动记账账单导出.csv")
        putExtra(Intent.EXTRA_TEXT, csvContent)
    }
    runCatching {
        val chooser = Intent.createChooser(sendIntent, "导出 CSV 账单").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }.onFailure {
        Toast.makeText(context, "未找到可用的分享目标应用", Toast.LENGTH_SHORT).show()
    }
}
