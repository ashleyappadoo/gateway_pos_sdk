package com.smileandpay.caissetest.viewmodel

import androidx.lifecycle.ViewModel
import com.smileandpay.caissetest.model.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaisseViewModel : ViewModel() {

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _txnType = MutableStateFlow("DEBIT")
    val txnType: StateFlow<String> = _txnType.asStateFlow()

    private val _orderId = MutableStateFlow("CMD-001")
    val orderId: StateFlow<String> = _orderId.asStateFlow()

    private val _lastResult = MutableStateFlow<PaymentResult?>(null)
    val lastResult: StateFlow<PaymentResult?> = _lastResult.asStateFlow()

    private val _gatewayInstalled = MutableStateFlow(true)
    val gatewayInstalled: StateFlow<Boolean> = _gatewayInstalled.asStateFlow()

    fun onAmountChanged(value: String) { _amount.value = value }
    fun onTxnTypeChanged(value: String) { _txnType.value = value }
    fun onOrderIdChanged(value: String) { _orderId.value = value }
    fun setGatewayInstalled(installed: Boolean) { _gatewayInstalled.value = installed }
    fun setResult(result: PaymentResult) { _lastResult.value = result }

    fun getAmountInCents(): Long {
        val euros = _amount.value.replace(",", ".").toDoubleOrNull() ?: 0.0
        return (euros * 100).toLong()
    }
}
