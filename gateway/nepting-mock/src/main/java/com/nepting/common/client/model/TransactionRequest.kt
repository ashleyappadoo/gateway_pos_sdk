package com.nepting.common.client.model

data class TransactionRequest(
    val amount: Long,
    val currency: String = "EUR",
    val type: String = "DEBIT",
    val orderId: String = "",
    val description: String = ""
)
