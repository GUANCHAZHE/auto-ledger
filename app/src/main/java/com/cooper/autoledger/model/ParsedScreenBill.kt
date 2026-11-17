package com.cooper.autoledger.model

data class ParsedScreenBill(
    val amountCents: Long,
    val type: TransactionType,
    val merchant: String,
    val channel: PaymentChannel,
    val suggestedCategory: Category,
    val sourcePkg: String,
    val rawTextSummary: String,
    val confidence: Float = 1.0f
)
