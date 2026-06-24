package com.nepting.common.client.model

data class TransactionResponse(
    val amount: Long,
    val type: String,
    val status: TransactionStatus,
    val ticketClient: String = "",
    val ticketMerchant: String = "",
    val tip: Long = 0L,
    val cardToken: String = "",
    val authCode: String = ""
)

enum class TransactionStatus { SUCCESS, REFUSED, CANCELLED, ERROR }
