package com.smileandpay.caissetest

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import com.smileandpay.caissetest.model.PaymentResult

object GatewayClient {

    const val GATEWAY_PACKAGE = "com.smileandpay.gateway"
    const val GATEWAY_ACTION = "com.smileandpay.gateway.ACTION_PAYMENT"

    const val EXTRA_AMOUNT = "amount"
    const val EXTRA_CURRENCY = "currency"
    const val EXTRA_TXN_TYPE = "txn_type"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_DESCRIPTION = "description"

    const val RESULT_STATUS = "status"
    const val RESULT_AMOUNT = "amount"
    const val RESULT_TICKET_CLIENT = "ticket_client"
    const val RESULT_TICKET_MERCHANT = "ticket_merchant"
    const val RESULT_CARD_TOKEN = "card_token"
    const val RESULT_AUTH_CODE = "auth_code"
    const val RESULT_ERROR_MSG = "error_message"

    const val REQUEST_CODE_PAYMENT = 1001

    fun isGatewayInstalled(activity: Activity): Boolean =
        try {
            activity.packageManager.getPackageInfo(GATEWAY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun launchPayment(
        activity: Activity,
        amountCents: Long,
        currency: String = "EUR",
        txnType: String = "DEBIT",
        orderId: String,
        description: String = ""
    ) {
        val intent = Intent(GATEWAY_ACTION).apply {
            setPackage(GATEWAY_PACKAGE)
            putExtra(EXTRA_AMOUNT, amountCents)
            putExtra(EXTRA_CURRENCY, currency)
            putExtra(EXTRA_TXN_TYPE, txnType)
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        @Suppress("DEPRECATION")
        activity.startActivityForResult(intent, REQUEST_CODE_PAYMENT)
    }

    fun parseResult(data: Intent?): PaymentResult {
        if (data == null) return PaymentResult(status = "CANCELLED")
        return PaymentResult(
            status = data.getStringExtra(RESULT_STATUS) ?: "ERROR",
            amount = data.getLongExtra(RESULT_AMOUNT, 0L),
            ticketClient = data.getStringExtra(RESULT_TICKET_CLIENT) ?: "",
            ticketMerchant = data.getStringExtra(RESULT_TICKET_MERCHANT) ?: "",
            cardToken = data.getStringExtra(RESULT_CARD_TOKEN) ?: "",
            authCode = data.getStringExtra(RESULT_AUTH_CODE) ?: "",
            errorMessage = data.getStringExtra(RESULT_ERROR_MSG) ?: ""
        )
    }
}
