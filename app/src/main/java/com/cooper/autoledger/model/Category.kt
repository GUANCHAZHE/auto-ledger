package com.cooper.autoledger.model

enum class Category(val displayName: String, val type: TransactionType) {
    DINING("餐饮美食", TransactionType.EXPENSE),
    TRANSPORT("交通出行", TransactionType.EXPENSE),
    SHOPPING("购物消费", TransactionType.EXPENSE),
    ENTERTAINMENT("休闲娱乐", TransactionType.EXPENSE),
    GROCERY("日用生鲜", TransactionType.EXPENSE),
    HOUSING("居家生活", TransactionType.EXPENSE),
    MEDICAL("医疗健康", TransactionType.EXPENSE),
    TRANSFER("转账红包", TransactionType.EXPENSE),
    OTHER_EXPENSE("其他支出", TransactionType.EXPENSE),
    SALARY("工资收入", TransactionType.INCOME),
    REFUND("退款红包", TransactionType.INCOME),
    OTHER_INCOME("其他收入", TransactionType.INCOME)
}
