package com.smileandpay.gateway.intent

import android.content.Intent
import com.smileandpay.gateway.model.PaymentRequest
import com.smileandpay.gateway.model.PaymentResult

object IntentPaymentHandler {

    const val GATEWAY_ACTION = "com.smileandpay.gateway.ACTION_PAYMENT"
    const val ACTION_CONFIG = "com.smileandpay.gateway.ACTION_CONFIG"

    const val EXTRA_AMOUNT = "amount"
    const val EXTRA_CURRENCY = "currency"
    const val EXTRA_TXN_TYPE = "txn_type"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_DESCRIPTION = "description"

    const val RESULT_EXTRA_STATUS = "status"
    const val RESULT_EXTRA_AMOUNT = "amount"
    const val RESULT_EXTRA_TICKET_CLIENT = "ticket_client"
    const val RESULT_EXTRA_TICKET_MERCHANT = "ticket_merchant"
    const val RESULT_EXTRA_CARD_TOKEN = "card_token"
    const val RESULT_EXTRA_AUTH_CODE = "auth_code"
    const val RESULT_EXTRA_ERROR_MSG = "error_message"

    fun parseIncomingIntent(intent: Intent): PaymentRequest? {
        val amount = intent.getLongExtra(EXTRA_AMOUNT, -1L)
        if (amount < 0) return null
        return PaymentRequest(
            amount = amount,
            currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "EUR",
            txnType = intent.getStringExtra(EXTRA_TXN_TYPE) ?: "DEBIT",
            orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: "",
            description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        )
    }

    fun buildResultIntent(result: PaymentResult): Intent =
        Intent().apply {
            putExtra(RESULT_EXTRA_STATUS, result.status)
            putExtra(RESULT_EXTRA_AMOUNT, result.amount)
            putExtra(RESULT_EXTRA_TICKET_CLIENT, result.ticketClient)
            putExtra(RESULT_EXTRA_TICKET_MERCHANT, result.ticketMerchant)
            putExtra(RESULT_EXTRA_CARD_TOKEN, result.cardToken)
            putExtra(RESULT_EXTRA_AUTH_CODE, result.authCode)
            if (result.errorMessage.isNotEmpty()) {
                putExtra(RESULT_EXTRA_ERROR_MSG, result.errorMessage)
            }
        }
}
