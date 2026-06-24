package com.smileandpay.gateway.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smileandpay.gateway.auth.CredentialsManager
import com.smileandpay.gateway.model.PaymentRequest
import com.smileandpay.gateway.model.PaymentResult
import com.smileandpay.gateway.nepting.NeptingOrchestrator
import com.smileandpay.gateway.nepting.OrchestratorState
import com.smileandpay.gateway.nepting.UIEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GatewayViewModel(private val context: Context) : ViewModel() {

    private val credentialsManager = CredentialsManager(context)
    private val orchestrator = NeptingOrchestrator(context, credentialsManager)

    val orchestratorState: StateFlow<OrchestratorState> = orchestrator.state

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _result = MutableLiveData<PaymentResult>()
    val result: LiveData<PaymentResult> = _result

    init {
        viewModelScope.launch {
            orchestrator.uiEvents.collect { event ->
                orchestrator.handleUiEvent(event)
                when (event) {
                    is UIEvent.MessageReceived -> _statusMessage.value = event.message
                    is UIEvent.NfcLedsChanged -> _statusMessage.value = "Lecteur NFC actif"
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            orchestrator.state.collect { state ->
                when (state) {
                    is OrchestratorState.Success -> {
                        val r = state.response
                        _result.postValue(
                            PaymentResult(
                                status = r.status.name,
                                amount = r.amount,
                                ticketClient = r.ticketClient,
                                ticketMerchant = r.ticketMerchant,
                                cardToken = r.cardToken,
                                authCode = r.authCode
                            )
                        )
                    }
                    is OrchestratorState.Failed -> {
                        _result.postValue(PaymentResult(status = "ERROR", errorMessage = state.reason))
                    }
                    is OrchestratorState.Cancelled -> {
                        _result.postValue(PaymentResult(status = "CANCELLED"))
                    }
                    else -> {}
                }
            }
        }
    }

    fun hasCredentials(): Boolean = credentialsManager.hasCredentials()

    fun saveCredentials(login: String, password: String, clientId: String) {
        credentialsManager.saveCredentials(login, password, clientId)
    }

    fun startPayment(request: PaymentRequest) {
        orchestrator.startPayment(request)
    }

    fun cancel() {
        orchestrator.cancel()
    }

    fun onPinEntered(pin: String) {
        // Le mock traite le PIN automatiquement
    }
}

class GatewayViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GatewayViewModel(context) as T
    }
}
