package com.smileandpay.gateway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.smileandpay.gateway.intent.IntentPaymentHandler
import com.smileandpay.gateway.model.PaymentResult
import com.smileandpay.gateway.nepting.OrchestratorState
import com.smileandpay.gateway.ui.ConfigScreen
import com.smileandpay.gateway.ui.PaymentScreen
import com.smileandpay.gateway.ui.PinEntryView
import com.smileandpay.gateway.viewmodel.GatewayViewModel
import com.smileandpay.gateway.viewmodel.GatewayViewModelFactory

class GatewayActivity : ComponentActivity() {

    private val viewModel: GatewayViewModel by viewModels {
        GatewayViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val paymentRequest = IntentPaymentHandler.parseIncomingIntent(intent ?: Intent())

        if (action == IntentPaymentHandler.ACTION_CONFIG) {
            showConfigScreen()
            return
        }

        if (paymentRequest == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (!viewModel.hasCredentials()) {
            showConfigScreen()
            return
        }

        viewModel.startPayment(paymentRequest)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.orchestratorState.collectAsState()
                    val statusMessage by viewModel.statusMessage.collectAsState()

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state is OrchestratorState.ProcessingPin) {
                            PinEntryView(onPinEntered = { viewModel.onPinEntered(it) })
                        }
                        PaymentScreen(
                            amount = paymentRequest.amount,
                            state = state,
                            statusMessage = statusMessage,
                            onCancel = { viewModel.cancel() }
                        )
                    }
                }
            }
        }

        viewModel.result.observe(this) { result ->
            finishWithResult(result)
        }
    }

    private fun finishWithResult(result: PaymentResult) {
        val resultCode = if (result.status == "SUCCESS") Activity.RESULT_OK else Activity.RESULT_CANCELED
        setResult(resultCode, IntentPaymentHandler.buildResultIntent(result))
        finish()
    }

    private fun showConfigScreen() {
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(
                        onSave = { login, password, clientId ->
                            viewModel.saveCredentials(login, password, clientId)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
