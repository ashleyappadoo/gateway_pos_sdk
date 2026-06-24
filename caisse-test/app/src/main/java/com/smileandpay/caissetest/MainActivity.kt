package com.smileandpay.caissetest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smileandpay.caissetest.ui.MainScreen
import com.smileandpay.caissetest.viewmodel.CaisseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CaisseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setGatewayInstalled(GatewayClient.isGatewayInstalled(this))

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onPay = ::launchPayment
                    )
                }
            }
        }
    }

    private fun launchPayment() {
        val orderId = viewModel.orderId.value.ifEmpty { "CMD-${System.currentTimeMillis()}" }
        GatewayClient.launchPayment(
            activity = this,
            amountCents = viewModel.getAmountInCents(),
            txnType = viewModel.txnType.value,
            orderId = orderId
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GatewayClient.REQUEST_CODE_PAYMENT) {
            val result = GatewayClient.parseResult(data)
            viewModel.setResult(result)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setGatewayInstalled(GatewayClient.isGatewayInstalled(this))
    }
}
