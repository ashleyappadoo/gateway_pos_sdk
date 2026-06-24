package com.smileandpay.gateway.model

data class PaymentResult(
    val status: String,
    val amount: Long = 0L,
    val ticketClient: String = "",
    val ticketMerchant: String = "",
    val cardToken: String = "",
    val authCode: String = "",
    val errorMessage: String = ""
)
