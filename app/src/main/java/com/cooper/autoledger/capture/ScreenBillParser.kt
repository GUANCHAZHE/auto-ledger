package com.cooper.autoledger.capture

import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.ParsedScreenBill
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object ScreenBillParser {

    private const val OWN_PACKAGE = "com.cooper.autoledger"
    private const val WECHAT_PACKAGE = "com.tencent.mm"
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    private const val UNIONPAY_PACKAGE = "com.unionpay"
    private const val MEITUAN_PACKAGE = "com.sankuai.meituan"
    private const val JD_PACKAGE = "com.jingdong.app.mall"

    private const val MAX_VALID_AMOUNT_CENTS = 100_000_000L // 1,000,000.00 CNY

    private val BANK_PACKAGE_TOKENS = listOf(
        "unionpay", "bank", "cmb", "icbc", "ccb", "boc", "abc",
        "psbc", "spdb", "cib", "citic", "cmbc", "pingan", "ceb", "gdb"
    )

    private val EXPENSE_COMPLETION_ANCHORS = listOf(
        "支付成功", "付款成功", "已支付", "交易成功", "订单已支付", "成功支付",
        "成功付款", "支付完成", "付款完成", "扣款成功", "自动扣款成功", "消费成功",
        "转账成功", "已转账", "缴费成功", "充值成功"
    )

    private val INCOME_COMPLETION_ANCHORS = listOf(
        "收款到账", "二维码收款到账", "收钱到账", "微信支付收款", "支付宝成功收款",
        "成功收款", "已收款", "已收钱", "退款到账", "退款成功", "已退款",
        "已退回零钱", "退回零钱", "已退回", "已存入零钱", "已存入余额",
        "转账到账", "入账成功", "工资入账", "代发工资"
    )

    private val INLINE_BANK_ANCHORS = listOf(
        "消费人民币", "入账人民币", "代发工资"
    )

    private val BILL_DETAIL_HEADERS = setOf(
        "账单详情", "交易详情", "收支详情", "当前状态", "交易状态"
    )

    private val STRUCTURAL_LABELS = setOf(
        "账单详情", "交易详情", "收支详情", "当前状态", "交易状态",
        "支付方式", "付款方式", "收款方", "收款方全称", "商户全称",
        "商户名称", "商家名称", "商品说明", "交易时间", "支付时间",
        "创建时间", "交易单号", "商户单号", "订单号", "退款方式",
        "到账账户", "收款账户", "转账说明", "交易对方"
    )

    private val FUNDING_CONTEXT_TOKENS = listOf(
        "零钱通", "微信零钱", "花呗", "余额宝", "储蓄卡", "信用卡", "借记卡", "云闪付"
    )

    private val CHAT_UI_LABELS = setOf(
        "发消息", "按住 说话", "切换到按住说话", "切换到键盘", "聊天信息",
        "更多功能按钮，已折叠", "更多功能按钮,已折叠", "发送"
    )

    private val PRE_PAYMENT_BLOCKERS = listOf(
        "请输入支付密码", "输入支付密码", "立即支付", "立即付款",
        "确认支付", "确认付款", "去支付", "去付款", "提交订单",
        "待支付", "等待买家付款", "等待付款"
    )

    private val BALANCE_SCREEN_BLOCKERS = listOf(
        "总资产", "昨日收益", "七日年化", "万份收益", "账户余额",
        "零钱余额", "我的零钱", "可提现金额"
    )

    private val NOTIFICATION_SPAM_BLOCKERS = listOf(
        "验证码", "最高可借", "可借额度", "授信额度", "免费领",
        "即将到期", "优惠券到期", "体验金", "还款提醒", "账单日"
    )

    private val PRIMARY_MERCHANT_LABELS = setOf(
        "收款方", "收款方全称", "商户全称", "商户名称", "商家名称",
        "交易对方", "对方账户", "退款方", "付款给", "商户", "商家"
    )

    private val SECONDARY_ITEM_LABELS = setOf(
        "商品说明", "商品名称", "商品", "消费名称", "订单信息", "转账说明"
    )

    private val PAID_AMOUNT_LABELS = setOf(
        "实付", "实付金额", "支付金额", "付款金额", "退款金额",
        "到账金额", "收款金额", "交易金额", "订单金额"
    )

    private val NON_PAID_AMOUNT_TOKENS = listOf(
        "优惠", "立减", "抵扣", "满减", "折扣", "代金券", "优惠券",
        "原价", "商品总价", "余额", "可用额度", "总资产", "手续费", "服务费", "奖励金"
    )

    private val IGNORED_MERCHANT_EXACT = setOf(
        "微信支付", "支付宝", "云闪付", "零钱", "微信零钱", "零钱通",
        "花呗", "余额宝", "白条", "京东白条", "美团月付", "极速付",
        "银行卡", "免密支付", "完成", "确定", "返回", "返回商家",
        "关闭", "查看订单", "查看详情", "查看账单", "更多", "我知道了",
        "账单服务", "开发票", "对此订单有疑惑", "定位到此交易", "投诉",
        "收款账户", "到账账户", "退款成功", "支付成功", "付款成功",
        "已支付", "交易成功", "订单已支付", "成功支付", "收款到账",
        "二维码收款到账", "已收款", "退款到账", "已退回零钱", "已存入零钱",
        "交易提醒", "服务通知", "动账通知", "账单通知", "支付通知", "消息通知"
    ) + STRUCTURAL_LABELS + PAID_AMOUNT_LABELS

    private val IGNORED_MERCHANT_SUBSTRINGS = listOf(
        "优惠券", "领券", "积分", "摇一摇", "奖励金", "免单", "广告",
        "推荐", "立即领取", "本服务由", "客服", "有疑问", "尾号", "点击查看"
    )

    private val BANK_FUNDING_REGEX = Regex(
        "^.{0,16}(储蓄卡|信用卡|借记卡|银行卡)(\\s*[\\(（]\\d{3,6}[\\)）])?$"
    )
    private val PURE_NUMBER_FRAGMENT_REGEX = Regex("^\\d{1,7}(?:,\\d{3})*(?:\\.\\d{1,2})?$")
    private val STANDALONE_AMOUNT_REGEX = Regex(
        "^([+-])?\\s*([¥￥])?\\s*(\\d{1,7}(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(元)?$"
    )
    private val INLINE_PREFIX_AMOUNT_REGEX = Regex(
        "(?:([+-])\\s*)?(?:[¥￥]|人民币|RMB)\\s*(\\d{1,7}(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*元?"
    )
    private val INLINE_YUAN_AMOUNT_REGEX = Regex(
        "(?:([+-])\\s*)?(\\d{1,7}(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*元"
    )
    private val TRAILING_BALANCE_CLAUSE_REGEX = Regex(
        "(账户余额|可用余额|当前余额|余额|可用额度|剩余额度)[：:\\s]*[¥￥]?\\s*\\d[\\d,]*(?:\\.\\d{1,2})?\\s*元?"
    )
    private val DATE_OR_TIME_REGEX = Regex(
        "^(\\d{4}[-/年]\\d{1,2}[-/月].*|\\d{1,2}:\\d{2}(:\\d{2})?|\\d{1,2}月\\d{1,2}日.*)$"
    )
    private val ORDER_ID_REGEX = Regex("^[0-9a-zA-Z_-]{10,}$")

    private data class AmountCandidate(
        val cents: Long,
        val sign: Int, // -1 expense, +1 income, 0 unsigned
        val nodeIndex: Int,
        val score: Int
    )

    fun parseScreenNodes(packageName: String, nodeTexts: List<String>): ParsedScreenBill? {
        val pkg = packageName.trim()
        val cleaned = nodeTexts
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return null

        // Never parse our own notification text or simulator labels.
        if (cleaned.any { it.startsWith("自动记账成功：") || it == "屏幕识别测试台" || it == "模拟自动入账" }) {
            return null
        }

        val nodes = coalesceCurrencyNodes(cleaned)
        if (!hasValidScreenPaymentContext(nodes)) {
            return null
        }

        val bestAmount = selectBestAmount(nodes) ?: return null
        val channel = detectChannel(pkg, nodes)
        val type = detectTransactionType(nodes, bestAmount.sign)
        val merchant = extractMerchantFromNodes(pkg, nodes, bestAmount.nodeIndex, type, channel)
        val rawSummary = nodes.take(12).joinToString(" | ").take(200)
        val suggestedCategory = CategoryClassifier.classify(merchant, rawSummary, type)
        val confidence = if (nodes.any { isStrongCompletionNode(it) }) 0.95f else 0.85f

        return ParsedScreenBill(
            amountCents = bestAmount.cents,
            type = type,
            merchant = merchant,
            channel = channel,
            suggestedCategory = suggestedCategory,
            sourcePkg = pkg,
            rawTextSummary = rawSummary,
            confidence = confidence
        )
    }

    fun parseNotification(packageName: String, title: String, content: String): ParsedScreenBill? {
        val pkg = packageName.trim()
        if (pkg == OWN_PACKAGE) return null

        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        if (cleanTitle.isEmpty() && cleanContent.isEmpty()) return null

        val combined = listOf(cleanTitle, cleanContent)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        if (combined.contains("自动记账成功：")) return null
        if (NOTIFICATION_SPAM_BLOCKERS.any { combined.contains(it) }) return null

        // Anti-false-positive for WeChat: reject ordinary friend/group chat notifications!
        if (pkg == WECHAT_PACKAGE && !isTrustedWeChatPayNotification(cleanTitle, cleanContent)) {
            return null
        }

        if (!hasValidNotificationPaymentContext(cleanTitle, cleanContent)) {
            return null
        }

        // Strip trailing balance clauses (e.g. "余额1200.00元") so we never capture account balance as spend!
        val sanitizedLines = combined
            .lines()
            .map { it.replace(TRAILING_BALANCE_CLAUSE_REGEX, "").trim() }
            .filter { it.isNotEmpty() }

        val nodes = coalesceCurrencyNodes(sanitizedLines)
        val bestAmount = selectBestAmount(nodes) ?: return null
        val channel = detectChannel(pkg, nodes)
        val type = detectNotificationTransactionType(combined, bestAmount.sign)
        val merchant = extractMerchantFromNotification(pkg, cleanTitle, cleanContent, nodes, bestAmount.nodeIndex, type, channel)
        val rawSummary = combined.replace('\n', ' ').take(200)
        val suggestedCategory = CategoryClassifier.classify(merchant, rawSummary, type)

        return ParsedScreenBill(
            amountCents = bestAmount.cents,
            type = type,
            merchant = merchant,
            channel = channel,
            suggestedCategory = suggestedCategory,
            sourcePkg = pkg,
            rawTextSummary = rawSummary,
            confidence = 0.90f
        )
    }

    private fun coalesceCurrencyNodes(rawNodes: List<String>): List<String> {
        val result = ArrayList<String>(rawNodes.size)
        var i = 0
        while (i < rawNodes.size) {
            val cur = rawNodes[i]
            val next = rawNodes.getOrNull(i + 1)
            val next2 = rawNodes.getOrNull(i + 2)

            if ((cur == "-" || cur == "+") && (next == "¥" || next == "￥") && next2 != null && PURE_NUMBER_FRAGMENT_REGEX.matches(next2)) {
                result.add("$cur$next$next2")
                i += 3
                continue
            }
            if ((cur == "¥" || cur == "￥" || cur == "-¥" || cur == "+¥" || cur == "-￥" || cur == "+￥" || cur == "-" || cur == "+") &&
                next != null && PURE_NUMBER_FRAGMENT_REGEX.matches(next)
            ) {
                result.add("$cur$next")
                i += 2
                continue
            }
            result.add(cur)
            i++
        }
        return result
    }

    private fun hasValidScreenPaymentContext(nodes: List<String>): Boolean {
        val hasStrongCompletion = nodes.any { isStrongCompletionNode(it) }
        val hasBillDetailHeader = nodes.any { it in BILL_DETAIL_HEADERS }
        val structuralCount = nodes.count { node ->
            node in STRUCTURAL_LABELS || STRUCTURAL_LABELS.any { label ->
                node.startsWith("$label：") || node.startsWith("$label:")
            }
        }
        val hasFundingContext = nodes.any { node ->
            FUNDING_CONTEXT_TOKENS.any { token -> node.contains(token) }
        }

        // Reject active chat windows unless structural bill labels are clearly present.
        if (nodes.any { it in CHAT_UI_LABELS } && structuralCount == 0) {
            return false
        }

        // Reject pre-payment cashier modals before actual payment completion.
        if (!hasStrongCompletion && nodes.any { node -> PRE_PAYMENT_BLOCKERS.any { node.contains(it) } }) {
            return false
        }

        // Reject home/wealth/wallet balance overview screens.
        if (!hasStrongCompletion && !hasBillDetailHeader &&
            nodes.any { node -> BALANCE_SCREEN_BLOCKERS.any { node.contains(it) } }
        ) {
            return false
        }

        if (hasStrongCompletion) return true
        if (hasBillDetailHeader && (structuralCount >= 2 || hasFundingContext)) return true
        if (structuralCount >= 2 && hasFundingContext) return true

        return false
    }

    private fun isStrongCompletionNode(node: String): Boolean {
        if (node.length > 36) return false
        if (node.contains("?") || node.contains("？") || node.contains("吗")) return false
        if (EXPENSE_COMPLETION_ANCHORS.any { node.contains(it) }) return true
        if (INCOME_COMPLETION_ANCHORS.any { node.contains(it) }) return true
        if (INLINE_BANK_ANCHORS.any { node.contains(it) }) return true
        return false
    }

    private fun isTrustedWeChatPayNotification(title: String, content: String): Boolean {
        val trustedTitles = listOf("微信支付", "微信收款", "服务通知", "收款通知", "退款")
        if (trustedTitles.any { title.contains(it) }) return true
        val trustedContentAnchors = listOf(
            "微信支付凭证", "微信支付收款", "二维码收款到账",
            "付款金额：", "付款金额:", "收款金额：", "退款金额："
        )
        return trustedContentAnchors.any { content.contains(it) }
    }

    private fun hasValidNotificationPaymentContext(title: String, content: String): Boolean {
        val combined = "$title\n$content"
        val notificationAnchors = EXPENSE_COMPLETION_ANCHORS + INCOME_COMPLETION_ANCHORS + listOf(
            "微信支付凭证", "交易提醒", "付款金额", "消费人民币", "消费",
            "支出", "扣款", "自动扣款", "入账", "存入", "成功向"
        )
        return notificationAnchors.any { combined.contains(it) }
    }

    private fun selectBestAmount(nodes: List<String>): AmountCandidate? {
        val candidates = ArrayList<AmountCandidate>()

        for ((index, rawNode) in nodes.withIndex()) {
            val prevNode = nodes.getOrNull(index - 1).orEmpty()
            val sanitizedForPenalty = (prevNode + " " + rawNode).replace("余额宝", "")
            val hasDiscountOrBalancePenalty = NON_PAID_AMOUNT_TOKENS.any { sanitizedForPenalty.contains(it) }
            val hasPaidBonus = prevNode in PAID_AMOUNT_LABELS ||
                PAID_AMOUNT_LABELS.any { rawNode.startsWith(it) } ||
                isStrongCompletionNode(prevNode)

            // 1. Check standalone currency/signed node first.
            val standaloneMatch = STANDALONE_AMOUNT_REGEX.matchEntire(rawNode)
            if (standaloneMatch != null) {
                val signStr = standaloneMatch.groupValues[1]
                val symbolStr = standaloneMatch.groupValues[2]
                val numStr = standaloneMatch.groupValues[3]
                val yuanStr = standaloneMatch.groupValues[4]

                val hasExplicitMarker = signStr.isNotEmpty() || symbolStr.isNotEmpty() || yuanStr.isNotEmpty()
                val hasDecimal = numStr.contains('.')

                // Reject bare integers like card tail "8812" or "12306" when no currency marker exists.
                if (hasExplicitMarker || hasDecimal) {
                    val cents = parseCentsSafe(numStr)
                    if (cents != null && cents in 1L..MAX_VALID_AMOUNT_CENTS) {
                        val sign = when (signStr) {
                            "-" -> -1
                            "+" -> 1
                            else -> 0
                        }
                        var score = if (symbolStr.isNotEmpty() || signStr.isNotEmpty()) 85 else 65
                        if (hasPaidBonus) score += 30
                        if (hasDiscountOrBalancePenalty) score -= 65
                        candidates.add(AmountCandidate(cents, sign, index, score))
                    }
                }
                continue
            }

            // 2. Check inline currency patterns inside sentence/key-value nodes.
            val nodeWithoutBalance = rawNode.replace(TRAILING_BALANCE_CLAUSE_REGEX, "")
            var matchedInline = false

            for (m in INLINE_PREFIX_AMOUNT_REGEX.findAll(nodeWithoutBalance)) {
                val signStr = m.groupValues[1]
                val numStr = m.groupValues[2]
                val cents = parseCentsSafe(numStr)
                if (cents != null && cents in 1L..MAX_VALID_AMOUNT_CENTS) {
                    val sign = when (signStr) {
                        "-" -> -1
                        "+" -> 1
                        else -> 0
                    }
                    var score = 70
                    if (hasPaidBonus || isTransactionActionSentence(nodeWithoutBalance)) score += 25
                    if (hasDiscountOrBalancePenalty) score -= 65
                    candidates.add(AmountCandidate(cents, sign, index, score))
                    matchedInline = true
                }
            }

            if (!matchedInline) {
                for (m in INLINE_YUAN_AMOUNT_REGEX.findAll(nodeWithoutBalance)) {
                    val signStr = m.groupValues[1]
                    val numStr = m.groupValues[2]
                    val cents = parseCentsSafe(numStr)
                    if (cents != null && cents in 1L..MAX_VALID_AMOUNT_CENTS) {
                        val sign = when (signStr) {
                            "-" -> -1
                            "+" -> 1
                            else -> 0
                        }
                        var score = 60
                        if (hasPaidBonus || isTransactionActionSentence(nodeWithoutBalance)) score += 25
                        if (hasDiscountOrBalancePenalty) score -= 65
                        candidates.add(AmountCandidate(cents, sign, index, score))
                    }
                }
            }
        }

        return candidates
            .filter { it.score > 0 }
            .maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenBy { -it.nodeIndex })
    }

    private fun isTransactionActionSentence(text: String): Boolean {
        val actionTokens = listOf(
            "实付", "付款金额", "支付金额", "退款金额", "收款金额",
            "消费", "支出", "成功支付", "成功付款", "已支付", "收款", "退款", "入账"
        )
        return actionTokens.any { text.contains(it) }
    }

    private fun parseCentsSafe(rawNumber: String): Long? {
        val normalized = rawNumber.replace(",", "").trim()
        if (normalized.isEmpty()) return null
        CurrencyUtils.parseYuanToCents(normalized)?.let { parsed ->
            if (parsed > 0L) return parsed
        }
        return try {
            BigDecimal(normalized)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
                .takeIf { it > 0L }
        } catch (_: Exception) {
            null
        }
    }

    private fun detectChannel(pkg: String, nodes: List<String>): PaymentChannel {
        val lowerPkg = pkg.lowercase(Locale.ROOT)
        if (pkg == WECHAT_PACKAGE) return PaymentChannel.WECHAT
        if (pkg == ALIPAY_PACKAGE) return PaymentChannel.ALIPAY
        if (pkg == UNIONPAY_PACKAGE || BANK_PACKAGE_TOKENS.any { lowerPkg.contains(it) }) {
            return PaymentChannel.UNIONPAY
        }

        val combined = nodes.joinToString(" ")
        return when {
            combined.contains("微信支付") || combined.contains("微信零钱") || combined.contains("零钱通") ->
                PaymentChannel.WECHAT
            combined.contains("支付宝") || combined.contains("花呗") || combined.contains("余额宝") ->
                PaymentChannel.ALIPAY
            combined.contains("云闪付") || combined.contains("银联") ||
                combined.contains("储蓄卡") || combined.contains("信用卡") ||
                combined.contains("借记卡") || combined.contains("银行") ->
                PaymentChannel.UNIONPAY
            else -> PaymentChannel.OTHER
        }
    }

    private fun detectTransactionType(nodes: List<String>, amountSign: Int): TransactionType {
        // Explicit income completion anchors take priority (note: never match "收款方" as income!).
        val hasIncomeAnchor = nodes.any { node ->
            INCOME_COMPLETION_ANCHORS.any { anchor -> node.contains(anchor) }
        }
        val hasExpenseAnchor = nodes.any { node ->
            EXPENSE_COMPLETION_ANCHORS.any { anchor -> node.contains(anchor) } ||
                node.contains("消费人民币")
        }

        if (hasIncomeAnchor && !hasExpenseAnchor) return TransactionType.INCOME
        if (amountSign > 0 && !hasExpenseAnchor) return TransactionType.INCOME
        if (amountSign < 0 && !hasIncomeAnchor) return TransactionType.EXPENSE
        if (hasIncomeAnchor) return TransactionType.INCOME

        return TransactionType.EXPENSE
    }

    private fun detectNotificationTransactionType(combined: String, amountSign: Int): TransactionType {
        val sanitized = combined
            .replace("收款方全称", "")
            .replace("收款方", "")
            .replace("收款人", "")

        val hasIncomeSignal = INCOME_COMPLETION_ANCHORS.any { sanitized.contains(it) } ||
            sanitized.contains("退款") ||
            sanitized.contains("入账") ||
            sanitized.contains("存入")
        val hasExpenseSignal = EXPENSE_COMPLETION_ANCHORS.any { sanitized.contains(it) } ||
            sanitized.contains("消费") ||
            sanitized.contains("支出") ||
            sanitized.contains("扣款") ||
            sanitized.contains("成功向")

        if (hasIncomeSignal && !hasExpenseSignal) return TransactionType.INCOME
        if (amountSign > 0 && !hasExpenseSignal) return TransactionType.INCOME
        if (amountSign < 0 && !hasIncomeSignal) return TransactionType.EXPENSE
        if (hasIncomeSignal) return TransactionType.INCOME

        return TransactionType.EXPENSE
    }

    private fun extractMerchantFromNodes(
        pkg: String,
        nodes: List<String>,
        amountNodeIndex: Int,
        type: TransactionType,
        channel: PaymentChannel
    ): String {
        // 1. Explicit primary merchant key-value labels.
        findKeyValueMerchant(nodes, PRIMARY_MERCHANT_LABELS)?.let { return it }

        // 2. Inline patterns like "向 肯德基 付款" or "付款给 张三".
        for (node in nodes) {
            Regex("^(?:向|给)\\s*(.+?)\\s*(?:付款|支付|转账)$").find(node)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
            Regex("^付款给[：:\\s]*(.+)$").find(node)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        // 3. Prominent merchant line near payment anchor or amount node.
        val anchorIndex = nodes.indexOfFirst { isStrongCompletionNode(it) }.takeIf { it >= 0 } ?: 0
        var bestCandidate: String? = null
        var bestScore = Int.MIN_VALUE

        for ((index, node) in nodes.withIndex()) {
            if (index == amountNodeIndex) continue
            if (isIgnoredForMerchant(node)) continue

            var score = 20
            val distAmount = kotlin.math.abs(index - amountNodeIndex)
            val distAnchor = kotlin.math.abs(index - anchorIndex)
            if (distAmount <= 2) score += 30 else if (distAmount <= 4) score += 15
            if (distAnchor <= 2) score += 25
            if (node.length in 2..26) score += 20
            if (index < 6) score += 10

            if (score > bestScore) {
                bestScore = score
                bestCandidate = node
            }
        }

        if (bestCandidate != null) {
            return bestCandidate
        }

        // 4. Secondary item description label fallback.
        findKeyValueMerchant(nodes, SECONDARY_ITEM_LABELS)?.let { return it }

        val isRefund = nodes.any { it.contains("退款") || it.contains("退回") }
        return fallbackMerchant(pkg, type, channel, isRefund)
    }

    private fun extractMerchantFromNotification(
        pkg: String,
        title: String,
        content: String,
        nodes: List<String>,
        amountNodeIndex: Int,
        type: TransactionType,
        channel: PaymentChannel
    ): String {
        val combined = "$title\n$content"

        val regexPatterns = listOf(
            Regex("(?:收款方全称|收款方|商户全称|商户名称|商家名称|商户|商家)[：:]\\s*([^\\n，,。；;\\[\\]]{2,30})"),
            Regex("(?:在|向)\\s*([^\\n，,。；;：:]{2,24}?)\\s*(?:成功支付|成功付款|支付成功|消费|支付|付款)"),
            Regex("(?:支出|消费)[\\(（]([^\\)）]{2,24})[\\)）]"),
            Regex("([^\\n，,。：:]{2,24}?)已退款")
        )

        for (pattern in regexPatterns) {
            val match = pattern.find(combined)
            val candidate = match?.groupValues?.getOrNull(1)?.trim()
            if (!candidate.isNullOrEmpty() && !isIgnoredForMerchant(candidate)) {
                return candidate
            }
        }

        if (combined.contains("代发工资") || combined.contains("工资入账")) {
            return if (title.isNotEmpty() && title != "服务通知") "${title}代发工资" else "工资入账"
        }

        val fromNodes = extractMerchantFromNodes(pkg, nodes, amountNodeIndex, type, channel)
        if (fromNodes.isNotEmpty() && fromNodes != title) {
            return fromNodes
        }

        val isRefund = combined.contains("退款") || combined.contains("退回")
        return fallbackMerchant(pkg, type, channel, isRefund)
    }

    private fun findKeyValueMerchant(nodes: List<String>, labels: Set<String>): String? {
        for ((index, node) in nodes.withIndex()) {
            for (label in labels) {
                if (node == label) {
                    val value = nodes.getOrNull(index + 1)?.trim()
                    if (!value.isNullOrEmpty() && !isIgnoredForMerchant(value)) {
                        return value
                    }
                } else if (node.startsWith("$label：") || node.startsWith("$label:")) {
                    val value = node.removePrefix("$label：").removePrefix("$label:").trim()
                    if (value.isNotEmpty() && !isIgnoredForMerchant(value)) {
                        return value
                    }
                }
            }
        }
        return null
    }

    private fun isIgnoredForMerchant(node: String): Boolean {
        val trimmed = node.trim()
        if (trimmed.length < 2 || trimmed.length > 40) return true
        if (trimmed in IGNORED_MERCHANT_EXACT) return true
        if (isStrongCompletionNode(trimmed)) return true
        if (STANDALONE_AMOUNT_REGEX.matches(trimmed)) return true
        if (BANK_FUNDING_REGEX.matches(trimmed)) return true
        if (DATE_OR_TIME_REGEX.matches(trimmed)) return true
        if (ORDER_ID_REGEX.matches(trimmed) && trimmed.any { it.isDigit() }) return true
        if (trimmed.all { !it.isLetter() && it.code < 0x4E00 }) return true
        if (IGNORED_MERCHANT_SUBSTRINGS.any { trimmed.contains(it) }) return true
        if (STRUCTURAL_LABELS.any { trimmed.startsWith("$it：") || trimmed.startsWith("$it:") }) return true
        if (PAID_AMOUNT_LABELS.any { trimmed.startsWith("$it：") || trimmed.startsWith("$it:") }) return true
        return false
    }

    private fun fallbackMerchant(
        pkg: String,
        type: TransactionType,
        channel: PaymentChannel,
        isRefund: Boolean = false
    ): String {
        if (type == TransactionType.INCOME) {
            if (isRefund) {
                return when (channel) {
                    PaymentChannel.WECHAT -> "微信退款"
                    PaymentChannel.ALIPAY -> "支付宝退款"
                    PaymentChannel.UNIONPAY -> "银行退款"
                    else -> "退款入账"
                }
            }
            return when (channel) {
                PaymentChannel.WECHAT -> "微信收款"
                PaymentChannel.ALIPAY -> "支付宝收款"
                PaymentChannel.UNIONPAY -> "银行入账"
                else -> "收款入账"
            }
        }
        return when (pkg) {
            MEITUAN_PACKAGE -> "美团消费"
            JD_PACKAGE -> "京东商城"
            else -> when (channel) {
                PaymentChannel.WECHAT -> "微信支付商户"
                PaymentChannel.ALIPAY -> "支付宝商户"
                PaymentChannel.UNIONPAY -> "云闪付/银行消费"
                else -> "商户消费"
            }
        }
    }
}
