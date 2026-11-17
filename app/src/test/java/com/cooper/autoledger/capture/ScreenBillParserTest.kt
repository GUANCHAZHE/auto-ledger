package com.cooper.autoledger.capture

import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenBillParserTest {

    @Test
    fun parseWeChatPaySuccessScreen_extractsAmountMerchantChannelAndDiningCategory() {
        val nodes = listOf(
            "支付成功",
            "瑞幸咖啡(科技园店)",
            "￥18.50",
            "支付方式",
            "招商银行储蓄卡(8812)"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.tencent.mm", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(1850L, parsed.amountCents)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("瑞幸咖啡(科技园店)", parsed.merchant)
        assertEquals(PaymentChannel.WECHAT, parsed.channel)
        assertEquals(Category.DINING, parsed.suggestedCategory)
    }

    @Test
    fun parseWeChatSplitCurrencySymbolNodes_coalescesSymbolAndDigits() {
        val nodes = listOf(
            "微信支付",
            "支付成功",
            "星巴克臻选咖啡",
            "¥",
            "38.00",
            "完成"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.tencent.mm", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(3800L, parsed.amountCents)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("星巴克臻选咖啡", parsed.merchant)
        assertEquals(PaymentChannel.WECHAT, parsed.channel)
        assertEquals(Category.DINING, parsed.suggestedCategory)
    }

    @Test
    fun parseAlipayPaymentSuccessScreen_extractsSignedExpenseAndMerchantLabel() {
        val nodes = listOf(
            "付款成功",
            "-36.80",
            "收款方全称",
            "美团外卖",
            "付款方式",
            "花呗"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.eg.android.AlipayGphone", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(3680L, parsed.amountCents)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("美团外卖", parsed.merchant)
        assertEquals(PaymentChannel.ALIPAY, parsed.channel)
        assertEquals(Category.DINING, parsed.suggestedCategory)
    }

    @Test
    fun parseAlipayBillDetailScreen_withYuebaoAndTransport_extractsCorrectly() {
        val nodes = listOf(
            "账单详情",
            "滴滴出行",
            "-24.50",
            "交易状态",
            "交易成功",
            "付款方式",
            "余额宝",
            "商品说明",
            "快车行程",
            "创建时间",
            "2026-11-17 09:15:32",
            "订单号",
            "202611172200145678901234"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.eg.android.AlipayGphone", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(2450L, parsed.amountCents)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("滴滴出行", parsed.merchant)
        assertEquals(PaymentChannel.ALIPAY, parsed.channel)
        assertEquals(Category.TRANSPORT, parsed.suggestedCategory)
    }

    @Test
    fun parseUnionPayAndMeituanScreens_extractExpenseAccurately() {
        val unionPayNodes = listOf(
            "云闪付",
            "支付成功",
            "盒马鲜生(深圳湾店)",
            "￥126.40",
            "付款方式",
            "工商银行储蓄卡(6688)"
        )
        val unionParsed = ScreenBillParser.parseScreenNodes("com.unionpay", unionPayNodes)
        assertNotNull(unionParsed)
        assertEquals(12640L, unionParsed!!.amountCents)
        assertEquals(PaymentChannel.UNIONPAY, unionParsed.channel)
        assertEquals("盒马鲜生(深圳湾店)", unionParsed.merchant)
        assertEquals(Category.GROCERY, unionParsed.suggestedCategory)

        val meituanNodes = listOf(
            "订单已支付",
            "￥29.90",
            "麦当劳(高新园店)",
            "查看订单"
        )
        val meituanParsed = ScreenBillParser.parseScreenNodes("com.sankuai.meituan", meituanNodes)
        assertNotNull(meituanParsed)
        assertEquals(2990L, meituanParsed!!.amountCents)
        assertEquals(TransactionType.EXPENSE, meituanParsed.type)
        assertEquals("麦当劳(高新园店)", meituanParsed.merchant)
        assertEquals(Category.DINING, meituanParsed.suggestedCategory)
    }

    @Test
    fun parseWeChatRefundScreen_detectsIncomeAndRefundCategory() {
        val nodes = listOf(
            "退款到账",
            "+89.00",
            "京东商城",
            "已退回零钱"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.tencent.mm", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(8900L, parsed.amountCents)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("京东商城", parsed.merchant)
        assertEquals(PaymentChannel.WECHAT, parsed.channel)
        assertEquals(Category.REFUND, parsed.suggestedCategory)
    }

    @Test
    fun parseWeChatQrCollectionScreen_detectsIncomeAndOtherIncomeCategory() {
        val nodes = listOf(
            "二维码收款到账",
            "￥15.00",
            "收款账户",
            "零钱",
            "已存入零钱"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.tencent.mm", nodes)

        assertNotNull(parsed)
        parsed!!
        assertEquals(1500L, parsed.amountCents)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(PaymentChannel.WECHAT, parsed.channel)
        assertEquals(Category.OTHER_INCOME, parsed.suggestedCategory)
    }

    @Test
    fun parseWeChatOfficialNotifications_parsesVoucherAndCollection() {
        val voucherParsed = ScreenBillParser.parseNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "微信支付凭证\n付款金额：￥42.00\n收款方：全家便利店"
        )
        assertNotNull(voucherParsed)
        assertEquals(4200L, voucherParsed!!.amountCents)
        assertEquals(TransactionType.EXPENSE, voucherParsed.type)
        assertEquals("全家便利店", voucherParsed.merchant)
        assertEquals(Category.GROCERY, voucherParsed.suggestedCategory)

        val collectionParsed = ScreenBillParser.parseNotification(
            packageName = "com.tencent.mm",
            title = "微信收款助手",
            content = "微信支付收款26.50元"
        )
        assertNotNull(collectionParsed)
        assertEquals(2650L, collectionParsed!!.amountCents)
        assertEquals(TransactionType.INCOME, collectionParsed.type)
        assertEquals(PaymentChannel.WECHAT, collectionParsed.channel)
    }

    @Test
    fun parseBankNotificationsWithTrailingBalance_ignoresAccountBalance() {
        val debitParsed = ScreenBillParser.parseNotification(
            packageName = "cmb.pb",
            title = "招商银行",
            content = "您账户8812于11月17日消费人民币128.00元，商户：沃尔玛，余额5432.10元。"
        )
        assertNotNull(debitParsed)
        assertEquals(12800L, debitParsed!!.amountCents)
        assertEquals(TransactionType.EXPENSE, debitParsed.type)
        assertEquals("沃尔玛", debitParsed.merchant)
        assertEquals(PaymentChannel.UNIONPAY, debitParsed.channel)
        assertEquals(Category.GROCERY, debitParsed.suggestedCategory)

        val salaryParsed = ScreenBillParser.parseNotification(
            packageName = "cmb.pb",
            title = "招商银行",
            content = "您账户8812于11月17日代发工资入账人民币15,200.00元，余额20,632.10元。"
        )
        assertNotNull(salaryParsed)
        assertEquals(1_520_000L, salaryParsed!!.amountCents)
        assertEquals(TransactionType.INCOME, salaryParsed.type)
        assertEquals(Category.SALARY, salaryParsed.suggestedCategory)
    }

    @Test
    fun rejectOrdinaryWeChatChatMessagesWithNumbers_returnsNull() {
        val chatWithInputBar = listOf(
            "张三",
            "今天午饭花了 25.00 元",
            "发消息"
        )
        assertNull(ScreenBillParser.parseScreenNodes("com.tencent.mm", chatWithInputBar))

        val casualGroupChat = listOf(
            "周末约饭群(8)",
            "李四",
            "这家火锅人均 ￥118.00 怎么样？",
            "王五",
            "可以啊今晚 18:30 走起"
        )
        assertNull(ScreenBillParser.parseScreenNodes("com.tencent.mm", casualGroupChat))
    }

    @Test
    fun rejectWeChatFriendChatNotificationEvenWithPaymentKeywords_returnsNull() {
        val friendChatNotif = ScreenBillParser.parseNotification(
            packageName = "com.tencent.mm",
            title = "张三",
            content = "我已经支付成功了，花了￥88.00"
        )
        assertNull(friendChatNotif)
    }

    @Test
    fun rejectWalletAndYuebaoBalanceOverviewScreens_returnsNull() {
        val wechatWalletTab = listOf(
            "服务",
            "钱包",
            "零钱",
            "¥520.00",
            "零钱通",
            "七日年化 1.65%",
            "银行卡"
        )
        assertNull(ScreenBillParser.parseScreenNodes("com.tencent.mm", wechatWalletTab))

        val alipayWealthTab = listOf(
            "首页",
            "理财",
            "余额宝",
            "总资产(元)",
            "12,800.00",
            "昨日收益",
            "+0.56"
        )
        assertNull(ScreenBillParser.parseScreenNodes("com.eg.android.AlipayGphone", alipayWealthTab))
    }

    @Test
    fun rejectPrePaymentCashierModalBeforeCompletion_returnsNull() {
        val cashierModal = listOf(
            "确认付款",
            "￥18.50",
            "瑞幸咖啡",
            "支付方式",
            "零钱",
            "请输入支付密码"
        )
        assertNull(ScreenBillParser.parseScreenNodes("com.tencent.mm", cashierModal))
    }

    @Test
    fun preferActualPaidAmountOverDiscountLineOnSuccessScreen() {
        val nodes = listOf(
            "支付成功",
            "肯德基宅急送",
            "实付金额",
            "￥29.50",
            "优惠立减",
            "-￥6.00",
            "支付方式",
            "零钱通"
        )

        val parsed = ScreenBillParser.parseScreenNodes("com.tencent.mm", nodes)

        assertNotNull(parsed)
        assertEquals(2950L, parsed!!.amountCents)
        assertEquals("肯德基宅急送", parsed.merchant)
        assertEquals(Category.DINING, parsed.suggestedCategory)
    }

    @Test
    fun categoryClassifier_classifiesAllMajorExpenseAndIncomeCategoriesAccurately() {
        assertEquals(
            Category.DINING,
            CategoryClassifier.classify("霸王茶姬", "", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.TRANSPORT,
            CategoryClassifier.classify("深圳地铁12306出行", "", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.GROCERY,
            CategoryClassifier.classify("叮咚买菜", "", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.SHOPPING,
            CategoryClassifier.classify("拼多多旗舰店", "", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.ENTERTAINMENT,
            CategoryClassifier.classify("淘票票电影", "Steam 游戏会员", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.HOUSING,
            CategoryClassifier.classify("南方电网电费", "中国移动话费", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.MEDICAL,
            CategoryClassifier.classify("京东大药房", "感冒药", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.TRANSFER,
            CategoryClassifier.classify("转账给李雷", "", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.OTHER_EXPENSE,
            CategoryClassifier.classify("未知个体工商户", "支付成功 | 红包抵扣", TransactionType.EXPENSE)
        )
        assertEquals(
            Category.SALARY,
            CategoryClassifier.classify("科技公司", "11月代发工资", TransactionType.INCOME)
        )
        assertEquals(
            Category.REFUND,
            CategoryClassifier.classify("淘宝网", "订单退货退款", TransactionType.INCOME)
        )
        assertEquals(
            Category.OTHER_INCOME,
            CategoryClassifier.classify("二维码收款", "收款到账", TransactionType.INCOME)
        )
    }

    @Test
    fun captureNotifierBannerFormat_matchesExpectedChineseSummary() {
        val expenseBill = BillRecord(
            id = 1L,
            amountCents = 1850L,
            type = TransactionType.EXPENSE,
            category = Category.DINING,
            merchant = "瑞幸咖啡",
            channel = PaymentChannel.WECHAT
        )
        assertEquals(
            "自动记账成功：-¥18.50 · 餐饮美食 (瑞幸咖啡)",
            CaptureNotifier.formatCaptureBannerText(expenseBill)
        )

        val incomeBill = BillRecord(
            id = 2L,
            amountCents = 8900L,
            type = TransactionType.INCOME,
            category = Category.REFUND,
            merchant = "京东商城",
            channel = PaymentChannel.WECHAT
        )
        assertEquals(
            "自动记账成功：+¥89.00 · 退款红包 (京东商城)",
            CaptureNotifier.formatCaptureBannerText(incomeBill)
        )
    }
}
