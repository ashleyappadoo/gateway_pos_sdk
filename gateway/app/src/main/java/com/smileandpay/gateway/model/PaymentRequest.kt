package com.smileandpay.gateway.model

data class PaymentRequest(
    val amount: Long,
    val currency: String = "EUR",
    val txnType: String = "DEBIT",
    val orderId: String = "",
    val description: String = ""
)
