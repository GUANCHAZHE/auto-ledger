package com.cooper.autoledger.model

enum class PaymentChannel(val displayName: String) {
    WECHAT("微信支付"),
    ALIPAY("支付宝"),
    UNIONPAY("云闪付/银行"),
    MANUAL("手动记账"),
    OTHER("其他渠道")
}
