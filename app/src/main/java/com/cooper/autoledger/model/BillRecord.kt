package com.cooper.autoledger.model

data class BillRecord(
    val id: Long = 0L,
    val amountCents: Long,
    val type: TransactionType,
    val category: Category,
    val merchant: String,
    val note: String = "",
    val channel: PaymentChannel,
    val sourcePkg: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val autoCaptured: Boolean = true,
    val rawDigest: String = ""
)
