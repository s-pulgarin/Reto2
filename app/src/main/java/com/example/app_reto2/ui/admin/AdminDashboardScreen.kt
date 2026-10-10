package com.example.app_reto2.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

private val ApprovedColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val pairLabel = "${state.currencyPair.take(3)} / ${state.currencyPair.drop(3)}"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Auditoría") },
                actions = {
                    Column(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = pairLabel, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = state.livePrice?.let { "%.4f".format(it) } ?: "--",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoadingTransactions -> CenteredBox(padding) {
                CircularProgressIndicator()
            }
            state.transactions.isEmpty() -> CenteredBox(padding) {
                Text("No hay transacciones registradas.")
            }
            else -> {
                val pending = state.pending
                val history = state.history
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item(key = "header_pending") {
                            SectionTitle("Pendientes (${pending.size})")
                        }
                        items(pending, key = { it.transactionId }) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                canAudit = state.livePrice != null,
                                isAuditing = transaction.transactionId in state.auditingIds,
                                onAudit = { viewModel.validateAndAudit(transaction) }
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        item(key = "header_history") {
                            SectionTitle("Historial (${history.size})")
                        }
                        items(history, key = { it.transactionId }) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                canAudit = false,
                                isAuditing = false,
                                onAudit = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(
    padding: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    canAudit: Boolean,
    isAuditing: Boolean,
    onAudit: () -> Unit
) {
    val statusColor = when (transaction.status) {
        TransactionStatus.APPROVED -> ApprovedColor
        TransactionStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val statusLabel = when (transaction.status) {
        TransactionStatus.APPROVED -> "APROBADA"
        TransactionStatus.REJECTED -> "RECHAZADA"
        else -> "PENDIENTE"
    }
    val typeLabel = if (transaction.type == TransactionType.BUY) "COMPRA" else "VENTA"
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, statusColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$typeLabel · ${transaction.currencyPair}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
            }
            Text("Cantidad: ${"%.4f".format(transaction.amount)}")
            Text("Precio de ejecución: ${"%.4f".format(transaction.executionPrice)}")
            Text(
                text = "Usuario: ${transaction.userId.take(8)}…",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = transaction.timestamp?.let { dateFormat.format(it) } ?: "Sincronizando…",
                style = MaterialTheme.typography.bodySmall
            )

            if (transaction.status == TransactionStatus.PENDING) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAudit,
                    enabled = canAudit && !isAuditing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAuditing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Auditar")
                    }
                }
            }
        }
    }
}