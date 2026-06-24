package com.smileandpay.gateway.nepting

import android.content.Context
import com.nepting.common.client.model.TransactionRequest
import com.nepting.common.client.model.TransactionResponse
import com.nepting.softpos.client.SoftPosHelper
import com.nepting.softpos.client.UIRequestCallback
import com.smileandpay.gateway.auth.CredentialsManager
import com.smileandpay.gateway.model.PaymentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class OrchestratorState {
    object Idle : OrchestratorState()
    object LoggingIn : OrchestratorState()
    object WaitingForCard : OrchestratorState()
    object ProcessingPin : OrchestratorState()
    object Processing : OrchestratorState()
    data class Success(val response: TransactionResponse) : OrchestratorState()
    data class Failed(val reason: String) : OrchestratorState()
    object Cancelled : OrchestratorState()
}

class TransactionCancelledException : Exception("Transaction annulée")

class NeptingOrchestrator(
    private val context: Context,
    private val credentialsManager: CredentialsManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow<OrchestratorState>(OrchestratorState.Idle)
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()

    private val uiCallbackHandler = UICallbackHandler()
    val uiEvents: SharedFlow<UIEvent> = uiCallbackHandler.events

    private var softPosClient: SoftPosClient? = null
    private var isSoftPosLoginCalledOnce = false

    fun startPayment(request: PaymentRequest) {
        scope.launch {
            try {
                ensureClient()
                if (!isSoftPosLoginCalledOnce) {
                    _state.value = OrchestratorState.LoggingIn
                    login()
                    isSoftPosLoginCalledOnce = true
                }
                _state.value = OrchestratorState.WaitingForCard
                val txnRequest = TransactionRequest(
                    amount = request.amount,
                    currency = request.currency,
                    type = request.txnType,
                    orderId = request.orderId,
                    description = request.description
                )
                val response = startTransaction(txnRequest)
                _state.value = OrchestratorState.Success(response)
            } catch (e: TransactionCancelledException) {
                _state.value = OrchestratorState.Cancelled
            } catch (e: Exception) {
                _state.value = OrchestratorState.Failed(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun cancel() {
        softPosClient?.abort()
        _state.value = OrchestratorState.Cancelled
    }

    fun reset() {
        _state.value = OrchestratorState.Idle
    }

    private fun ensureClient() {
        if (softPosClient == null) {
            softPosClient = SoftPosHelper.createClient(
                context = context,
                uiCallback = uiCallbackHandler,
                debugEnabled = true
            )
            softPosClient?.start()
        }
    }

    private suspend fun login() = suspendCoroutine { cont ->
        val loginRequest = credentialsManager.getLoginRequest()
        softPosClient?.softposLogin(loginRequest, object : UIRequestCallback {
            override fun onSuccess(response: TransactionResponse?) = cont.resume(Unit)
            override fun onFailure(reason: String) = cont.resumeWithException(Exception(reason))
            override fun onCancelled() = cont.resumeWithException(TransactionCancelledException())
        })
    }

    private suspend fun startTransaction(request: TransactionRequest): TransactionResponse =
        suspendCoroutine { cont ->
            softPosClient?.softposStartTransaction(request, object : UIRequestCallback {
                override fun onSuccess(response: TransactionResponse?) {
                    if (response != null) cont.resume(response)
                    else cont.resumeWithException(Exception("Réponse vide"))
                }

                override fun onFailure(reason: String) =
                    cont.resumeWithException(Exception(reason))

                override fun onCancelled() =
                    cont.resumeWithException(TransactionCancelledException())
            })
        }

    fun handleUiEvent(event: UIEvent) {
        when (event) {
            is UIEvent.PinEntryRequested -> _state.value = OrchestratorState.ProcessingPin
            else -> {}
        }
    }
}
