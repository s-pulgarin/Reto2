package com.example.app_reto2.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_reto2.data.model.TransactionType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
private val AmountRegex = Regex("^\\d*\\.?\\d*$")

@Composable
fun UserDashboardScreen(
    viewModel: UserDashboardViewModel,
    onNavigateToKyc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    UserDashboardContent(
        state = state,
        onRefresh = viewModel::loadPrice,
        onTrade = viewModel::executeTrade,
        onNavigateToKyc = onNavigateToKyc,
        modifier = modifier
    )
}

@Composable
private fun UserDashboardContent(
    state: UserDashboardState,
    onRefresh: () -> Unit,
    onTrade: (amount: Double, tradeType: String) -> Unit,
    onNavigateToKyc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) amountText = ""
    }

    val canTrade = state.price != null && !state.isLoadingPrice && !state.isSavingTrade
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val pairLabel = "${state.currencyPair.take(3)} / ${state.currencyPair.drop(3)}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mercado de divisas",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedButton(
            onClick = onNavigateToKyc,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verificar identidad (KYC)")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = pairLabel, style = MaterialTheme.typography.titleMedium)
                if (state.isLoadingPrice) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                } else {
                    Text(
                        text = state.price?.let { "%.5f".format(it) } ?: "--",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onRefresh, enabled = !state.isSavingTrade) {
                    Text("Reconectar")
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { if (AmountRegex.matches(it)) amountText = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSavingTrade,
            label = { Text("Cantidad a operar") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        if (state.isSavingTrade) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        state.successMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onTrade(amount, TransactionType.BUY) },
                enabled = canTrade,
                modifier = Modifier.weight(1f)
            ) {
                Text("COMPRAR")
            }
            Button(
                onClick = { onTrade(amount, TransactionType.SELL) },
                enabled = canTrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("VENDER")
            }
        }
    }
}