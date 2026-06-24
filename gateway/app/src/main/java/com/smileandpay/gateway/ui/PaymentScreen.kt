package com.smileandpay.gateway.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smileandpay.gateway.R
import com.smileandpay.gateway.nepting.OrchestratorState

@Composable
fun PaymentScreen(
    amount: Long,
    state: OrchestratorState,
    statusMessage: String,
    onCancel: () -> Unit
) {
    val euros = amount / 100
    val cents = (amount % 100).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$euros,$cents €",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        NfcIcon(
            active = state is OrchestratorState.WaitingForCard ||
                    state is OrchestratorState.ProcessingPin
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when (state) {
                is OrchestratorState.LoggingIn -> "Connexion au serveur..."
                is OrchestratorState.WaitingForCard -> statusMessage.ifEmpty { "Présentez votre carte" }
                is OrchestratorState.ProcessingPin -> "Saisie du code PIN"
                is OrchestratorState.Processing -> "Traitement en cours..."
                is OrchestratorState.Success -> "Paiement accepté ✓"
                is OrchestratorState.Failed -> "Échec : ${state.reason}"
                is OrchestratorState.Cancelled -> "Transaction annulée"
                else -> "Initialisation..."
            },
            fontSize = 20.sp,
            color = when (state) {
                is OrchestratorState.Success -> Color(0xFF2E7D32)
                is OrchestratorState.Failed -> MaterialTheme.colorScheme.error
                is OrchestratorState.Cancelled -> Color(0xFFF57C00)
                else -> MaterialTheme.colorScheme.onBackground
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (state !is OrchestratorState.Success &&
            state !is OrchestratorState.Failed &&
            state !is OrchestratorState.Cancelled
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Annuler")
            }
        }
    }
}

@Composable
fun NfcIcon(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_alpha"
    )
    Icon(
        painter = painterResource(id = R.drawable.ic_nfc),
        contentDescription = "NFC",
        modifier = Modifier
            .size(80.dp)
            .alpha(if (active) alpha else 0.3f),
        tint = if (active) MaterialTheme.colorScheme.primary else Color.Gray
    )
}
