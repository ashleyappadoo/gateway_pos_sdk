package com.smileandpay.caissetest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smileandpay.caissetest.viewmodel.CaisseViewModel

@Composable
fun MainScreen(viewModel: CaisseViewModel, onPay: () -> Unit) {
    val amount by viewModel.amount.collectAsState()
    val txnType by viewModel.txnType.collectAsState()
    val orderId by viewModel.orderId.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val gatewayInstalled by viewModel.gatewayInstalled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Caisse Test", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (!gatewayInstalled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚠ Gateway Smile&Pay non installée",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = amount,
            onValueChange = viewModel::onAmountChanged,
            label = { Text("Montant (€)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("€") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Type de transaction", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DEBIT", "CREDIT", "VOID").forEach { type ->
                FilterChip(
                    selected = txnType == type,
                    onClick = { viewModel.onTxnTypeChanged(type) },
                    label = { Text(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = orderId,
            onValueChange = viewModel::onOrderIdChanged,
            label = { Text("Référence commande") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPay,
            enabled = gatewayInstalled && amount.isNotBlank() && amount.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Payer via Gateway  →")
        }

        lastResult?.let { result ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (result.status) {
                        "SUCCESS" -> Color(0xFFE8F5E9)
                        "CANCELLED" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Résultat : ${result.status}",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (result.status) {
                            "SUCCESS" -> Color(0xFF2E7D32)
                            "CANCELLED" -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        }
                    )
                    if (result.status == "SUCCESS") {
                        val euros = result.amount / 100
                        val cents = (result.amount % 100).toString().padStart(2, '0')
                        Text("Montant : $euros,$cents €")
                        if (result.authCode.isNotEmpty()) Text("Auth : ${result.authCode}")
                        if (result.ticketClient.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ticket client :", style = MaterialTheme.typography.labelMedium)
                            Text(result.ticketClient, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (result.errorMessage.isNotEmpty()) {
                        Text("Erreur : ${result.errorMessage}", color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
}
