package com.example.app_reto2.ui.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

private val ApprovedColor = Color(0xFF2E7D32)
private val PendingColor = Color(0xFFEF6C00)
private val AmountRegex = Regex("^\\d*\\.?\\d*$")

private fun formatMoney(value: Double): String =
    String.format(Locale.US, "$ %,.2f", value)

private fun formatPrice(value: Double): String {
    val decimals = if (value >= 100) 2 else 5
    return String.format(Locale.US, "$ %,.${decimals}f", value)
}

private fun formatAmount(value: Double): String =
    value.toBigDecimal().stripTrailingZeros().toPlainString()

@Composable
fun UserDashboardScreen(
    viewModel: UserDashboardViewModel,
    onNavigateToKyc: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    UserDashboardContent(
        state = state,
        onRefresh = viewModel::loadPrice,
        onTrade = viewModel::executeTrade,
        onNavigateToKyc = onNavigateToKyc,
        onLogout = onLogout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDashboardContent(
    state: UserDashboardState,
    onRefresh: () -> Unit,
    onTrade: (amount: Double, tradeType: String) -> Unit,
    onNavigateToKyc: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) amountText = ""
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mi cuenta") },
                actions = {
                    TextButton(onClick = onNavigateToKyc) { Text("KYC") }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BalanceCard(balance = state.balance) }

            item {
                MarketCard(
                    pairLabel = "${state.currencyPair.take(3)}/${state.currencyPair.drop(3)}",
                    price = state.price,
                    isLoading = state.isLoadingPrice,
                    statusMessage = state.statusMessage,
                    onRefresh = onRefresh,
                    refreshEnabled = !state.isSavingTrade
                )
            }

            item {
                TradeSection(
                    state = state,
                    amountText = amountText,
                    onAmountChange = { amountText = it },
                    onTrade = onTrade
                )
            }

            item {
                Text(
                    text = "Mis Transacciones",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            when {
                state.isLoadingHistory -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                state.transactions.isEmpty() -> item {
                    Text(
                        text = "Aún no tienes transacciones.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> items(state.transactions, key = { it.transactionId }) { transaction ->
                    TransactionItem(transaction)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Saldo Disponible", style = MaterialTheme.typography.labelLarge)
            if (balance == null) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                Text(
                    text = formatMoney(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MarketCard(
    pairLabel: String,
    price: Double?,
    isLoading: Boolean,
    statusMessage: String?,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Mercado en vivo", style = MaterialTheme.typography.labelLarge)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                Text(
                    text = "$pairLabel - ${price?.let(::formatPrice) ?: "--"}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            statusMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            TextButton(onClick = onRefresh, enabled = refreshEnabled) {
                Text("Reconectar")
            }
        }
    }
}

@Composable
private fun TradeSection(
    state: UserDashboardState,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onTrade: (amount: Double, tradeType: String) -> Unit
) {
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val price = state.price
    val balance = state.balance
    val total = price?.let { amount * it }
    val exceedsBalance = total != null && balance != null && total > balance
    val canTrade = price != null && !state.isLoadingPrice && !state.isSavingTrade

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { if (AmountRegex.matches(it)) onAmountChange(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSavingTrade,
            label = { Text("Cantidad a operar") },
            singleLine = true,
            isError = exceedsBalance,
            supportingText = {
                if (exceedsBalance) {
                    Text("El total supera tu saldo disponible (aplica a compras).")
                } else if (total != null && amount > 0.0) {
                    Text("Total estimado: ${formatMoney(total)}")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        if (state.isSavingTrade) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        state.successMessage?.let {
            Text(text = it, color = ApprovedColor)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onTrade(amount, TransactionType.BUY) },
                enabled = canTrade && !exceedsBalance,
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

@Composable
private fun TransactionItem(transaction: Transaction) {
    val (statusColor, statusIcon, statusLabel) = when (transaction.status) {
        TransactionStatus.APPROVED -> Triple(ApprovedColor, Icons.Default.CheckCircle, "APROBADA")
        TransactionStatus.REJECTED -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Close, "RECHAZADA")
        else -> Triple(PendingColor, Icons.Default.Info, "PENDIENTE")
    }
    val typeLabel = if (transaction.type == TransactionType.BUY) "COMPRA" else "VENTA"
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.5.dp, statusColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusLabel,
                tint = statusColor
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$typeLabel · ${transaction.currencyPair}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${formatAmount(transaction.amount)} @ ${formatPrice(transaction.executionPrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = transaction.timestamp?.let { dateFormat.format(it) } ?: "Sincronizando…",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(transaction.amount * transaction.executionPrice),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}