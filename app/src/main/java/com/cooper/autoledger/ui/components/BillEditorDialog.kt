package com.cooper.autoledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import com.cooper.autoledger.ui.theme.ExpenseRose
import com.cooper.autoledger.ui.theme.IncomeEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillEditorDialog(
    initialRecord: BillRecord? = null,
    onDismiss: () -> Unit,
    onSave: (
        amountCents: Long,
        type: TransactionType,
        category: Category,
        channel: PaymentChannel,
        merchant: String,
        note: String
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val isEditMode = initialRecord != null

    var selectedType by remember(initialRecord) {
        mutableStateOf(initialRecord?.type ?: TransactionType.EXPENSE)
    }
    var amountInput by remember(initialRecord) {
        mutableStateOf(
            initialRecord?.let { CurrencyUtils.formatYuan(it.amountCents) } ?: ""
        )
    }
    var selectedCategory by remember(initialRecord) {
        mutableStateOf(initialRecord?.category ?: Category.DINING)
    }
    var selectedChannel by remember(initialRecord) {
        mutableStateOf(initialRecord?.channel ?: PaymentChannel.MANUAL)
    }
    var merchantInput by remember(initialRecord) {
        mutableStateOf(initialRecord?.merchant ?: "")
    }
    var noteInput by remember(initialRecord) {
        mutableStateOf(initialRecord?.note ?: "")
    }
    var errorHint by remember { mutableStateOf<String?>(null) }
    var confirmDeleteArmed by remember { mutableStateOf(false) }

    val availableCategories = remember(selectedType) {
        Category.entries.filter { it.type == selectedType }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "编辑账单明细" else "记一笔账单",
                    style = MaterialTheme.typography.titleLarge
                )
                if (initialRecord?.autoCaptured == true) {
                    AutoCapturedBadge()
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expense / Income Toggle Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionTypeSegment(
                        label = "支出",
                        selected = selectedType == TransactionType.EXPENSE,
                        activeColor = ExpenseRose,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            if (selectedCategory.type != TransactionType.EXPENSE) {
                                selectedCategory = Category.DINING
                            }
                        }
                    )
                    TransactionTypeSegment(
                        label = "收入",
                        selected = selectedType == TransactionType.INCOME,
                        activeColor = IncomeEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedType = TransactionType.INCOME
                            if (selectedCategory.type != TransactionType.INCOME) {
                                selectedCategory = Category.SALARY
                            }
                        }
                    )
                }

                // Amount Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        errorHint = null
                    },
                    label = { Text("金额 (元)") },
                    placeholder = { Text("0.00") },
                    prefix = {
                        Text(
                            text = "¥ ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    isError = errorHint != null,
                    supportingText = errorHint?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Text(
                    text = "账单分类",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { category ->
                        val isSelected = category == selectedCategory
                        val accent = category.accentColor()
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                            },
                            label = { Text(category.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Payment Channel Chips
                Text(
                    text = "支付渠道",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentChannel.entries.forEach { channel ->
                        FilterChip(
                            selected = channel == selectedChannel,
                            onClick = { selectedChannel = channel },
                            label = { Text(channel.displayName) }
                        )
                    }
                }

                // Merchant & Note
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    label = { Text("商户 / 交易对象") },
                    placeholder = { Text("例如：瑞幸咖啡、美团外卖、地铁出行") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("备注说明 (选填)") },
                    placeholder = { Text("补充消费说明或标签") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditMode && initialRecord != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (confirmDeleteArmed) {
                                    onDelete(initialRecord.id)
                                } else {
                                    confirmDeleteArmed = true
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (confirmDeleteArmed) "再次点击确认删除该笔账单" else "删除该笔账单")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = CurrencyUtils.parseYuanToCents(amountInput.trim())
                    if (cents == null || cents <= 0L) {
                        errorHint = "请输入大于 0 的有效金额，例如 18.50"
                        return@Button
                    }
                    val cleanMerchant = merchantInput.trim().ifEmpty { selectedCategory.displayName }
                    onSave(
                        cents,
                        selectedType,
                        selectedCategory,
                        selectedChannel,
                        cleanMerchant,
                        noteInput.trim()
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TransactionTypeSegment(
    label: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
