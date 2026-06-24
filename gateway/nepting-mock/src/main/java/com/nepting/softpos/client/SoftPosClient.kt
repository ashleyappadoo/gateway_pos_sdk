package com.nepting.softpos.client

import android.app.Activity
import android.content.Context
import android.view.View
import com.nepting.common.client.callback.UICallback
import com.nepting.common.client.callback.UIRequest
import com.nepting.common.client.model.LoginRequest
import com.nepting.common.client.model.TransactionRequest
import com.nepting.common.client.model.TransactionResponse
import com.nepting.common.client.model.TransactionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoftPosClient(
    private val uiCallback: UICallback,
    private val logger: Any?,
    private val debugEnabled: Boolean,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var transactionCallback: UIRequestCallback? = null
    private var transactionCancelled = false

    fun setContainer(layoutId: Int, view: View?) {}

    fun setActivity(activity: Activity) {}

    fun start() {}

    fun softposLogin(request: LoginRequest, callback: UIRequestCallback) {
        scope.launch {
            delay(1000)
            callback.onSuccess(null)
        }
    }

    fun softposStartTransaction(request: TransactionRequest, callback: UIRequestCallback) {
        transactionCallback = callback
        transactionCancelled = false
        scope.launch {
            uiCallback.postUIRequest(
                UIRequest(UIRequest.ActionType.NFC_LEDS, "Lecteur NFC activé")
            )
            delay(1000)
            if (transactionCancelled) return@launch
            uiCallback.postUIRequest(
                UIRequest(UIRequest.ActionType.MESSAGE, "PRÉSENTEZ VOTRE CARTE")
            )
            delay(2000)
            if (transactionCancelled) return@launch
            uiCallback.postUIRequest(
                UIRequest(UIRequest.ActionType.PIN_ENTRY, "Saisie PIN")
            )
            delay(1000)
            if (transactionCancelled) return@launch
            val response = TransactionResponse(
                amount = request.amount,
                type = request.type,
                status = TransactionStatus.SUCCESS,
                ticketClient = buildTicket("CLIENT", request),
                ticketMerchant = buildTicket("MARCHAND", request),
                authCode = "AUTH${System.currentTimeMillis() % 100000}",
                cardToken = "TOKEN${System.currentTimeMillis() % 1000000}"
            )
            callback.onSuccess(response)
        }
    }

    fun softposStopTransaction() {
        transactionCancelled = true
        transactionCallback?.onCancelled()
        transactionCallback = null
    }

    fun logoff() {}

    fun interrupt() {
        transactionCancelled = true
        transactionCallback?.onCancelled()
        transactionCallback = null
    }

    fun abort() {
        transactionCancelled = true
        transactionCallback?.onCancelled()
        transactionCallback = null
    }

    private fun buildTicket(type: String, request: TransactionRequest): String {
        val euros = request.amount / 100
        val cents = request.amount % 100
        return """
            *** TICKET $type ***
            Montant : $euros,${cents.toString().padStart(2, '0')} EUR
            Type    : ${request.type}
            Ref     : ${request.orderId}
            Statut  : APPROUVÉ
            *********************
        """.trimIndent()
    }
}
