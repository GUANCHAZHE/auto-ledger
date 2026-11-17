package com.cooper.autoledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.ui.theme.AlipayBlue
import com.cooper.autoledger.ui.theme.IncomeEmerald
import com.cooper.autoledger.ui.theme.ManualSlate
import com.cooper.autoledger.ui.theme.OtherPurple
import com.cooper.autoledger.ui.theme.UnionPayRed
import com.cooper.autoledger.ui.theme.WeChatGreen

fun Category.accentColor(): Color = when (this) {
    Category.DINING -> Color(0xFFF97316)        // Warm Orange
    Category.TRANSPORT -> Color(0xFF0284C7)     // Sky Blue
    Category.SHOPPING -> Color(0xFFEC4899)      // Rose Pink
    Category.ENTERTAINMENT -> Color(0xFF8B5CF6) // Vivid Violet
    Category.GROCERY -> Color(0xFF10B981)       // Fresh Emerald
    Category.HOUSING -> Color(0xFF0D9488)       // Deep Teal
    Category.MEDICAL -> Color(0xFFE11D48)       // Ruby Red
    Category.TRANSFER -> Color(0xFFF59E0B)      // Amber Gold
    Category.OTHER_EXPENSE -> Color(0xFF64748B) // Cool Slate
    Category.SALARY -> Color(0xFF059669)        // Rich Emerald
    Category.REFUND -> Color(0xFF0EA5E9)        // Cyan Blue
    Category.OTHER_INCOME -> Color(0xFF14B8A6)  // Mint Teal
}

fun Category.symbolGlyph(): String = when (this) {
    Category.DINING -> "餐"
    Category.TRANSPORT -> "行"
    Category.SHOPPING -> "购"
    Category.ENTERTAINMENT -> "娱"
    Category.GROCERY -> "鲜"
    Category.HOUSING -> "居"
    Category.MEDICAL -> "医"
    Category.TRANSFER -> "转"
    Category.OTHER_EXPENSE -> "支"
    Category.SALARY -> "薪"
    Category.REFUND -> "退"
    Category.OTHER_INCOME -> "收"
}

fun PaymentChannel.brandColor(): Color = when (this) {
    PaymentChannel.WECHAT -> WeChatGreen
    PaymentChannel.ALIPAY -> AlipayBlue
    PaymentChannel.UNIONPAY -> UnionPayRed
    PaymentChannel.MANUAL -> ManualSlate
    PaymentChannel.OTHER -> OtherPurple
}

@Composable
fun CategoryAvatar(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    fontSize: TextUnit = 16.sp
) {
    val accent = category.accentColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(width = 1.dp, color = accent.copy(alpha = 0.28f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.symbolGlyph(),
            color = accent,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PaymentChannelBadge(
    channel: PaymentChannel,
    modifier: Modifier = Modifier
) {
    val color = channel.brandColor()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = channel.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AutoCapturedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(IncomeEmerald.copy(alpha = 0.13f))
            .border(
                width = 0.75.dp,
                color = IncomeEmerald.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "屏幕自动",
            style = MaterialTheme.typography.labelSmall,
            color = IncomeEmerald,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Custom crisp vector icons for the 4 bottom navigation tabs so offline builds never depend on
 * `material-icons-extended`.
 */
object NavTabIcons {
    val Ledger: ImageVector by lazy {
        ImageVector.Builder(
            name = "LedgerTab",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()
                moveTo(9f, 9f)
                lineTo(15f, 9f)
                moveTo(9f, 13f)
                lineTo(15f, 13f)
                moveTo(9f, 17f)
                lineTo(12.5f, 17f)
            }
        }.build()
    }

    val Stats: ImageVector by lazy {
        ImageVector.Builder(
            name = "StatsTab",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                lineTo(20f, 20f)
                moveTo(7f, 20f)
                lineTo(7f, 13f)
                moveTo(12f, 20f)
                lineTo(12f, 7f)
                moveTo(17f, 20f)
                lineTo(17f, 10f)
            }
        }.build()
    }

    val Simulator: ImageVector by lazy {
        ImageVector.Builder(
            name = "SimulatorTab",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Viewfinder corners + center scan line
                moveTo(4f, 8f)
                lineTo(4f, 5f)
                lineTo(7f, 5f)
                moveTo(17f, 5f)
                lineTo(20f, 5f)
                lineTo(20f, 8f)
                moveTo(4f, 16f)
                lineTo(4f, 19f)
                lineTo(7f, 19f)
                moveTo(17f, 19f)
                lineTo(20f, 19f)
                lineTo(20f, 16f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "SettingsTab",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(9f, 5f)
                lineTo(9f, 9f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(15f, 10f)
                lineTo(15f, 14f)
                moveTo(4f, 17f)
                lineTo(20f, 17f)
                moveTo(10f, 15f)
                lineTo(10f, 19f)
            }
        }.build()
    }
}
