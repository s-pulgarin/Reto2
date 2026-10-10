package com.example.app_reto2.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.repository.FinnhubRepository
import com.example.app_reto2.data.repository.TransactionRepository
import com.example.app_reto2.util.MarketConfig
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class AdminDashboardState(
    val currencyPair: String = "",
    val livePrice: Double? = null,
    val isLoadingTransactions: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val auditingIds: Set<String> = emptySet(),
    val message: String? = null
) {
    val pending: List<Transaction>
        get() = transactions.filter { it.status == TransactionStatus.PENDING }

    val history: List<Transaction>
        get() = transactions.filter { it.status != TransactionStatus.PENDING }
}

class AdminDashboardViewModel(
    private val finnhubRepository: FinnhubRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        AdminDashboardState(currencyPair = MarketConfig.CURRENCY_PAIR)
    )
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    init {
        observeTransactions()
        observeLivePrice()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(transactions = result.data, isLoadingTransactions = false)
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoadingTransactions = false, message = result.message)
                    }
                }
            }
        }
    }

    private fun observeLivePrice() {
        viewModelScope.launch {
            finnhubRepository.observePrice(MarketConfig.SYMBOL).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update { it.copy(livePrice = result.data) }
                    is Resource.Error -> _state.update {
                        it.copy(livePrice = null, message = result.message)
                    }
                }
            }
        }
    }

    /** Aprueba si el precio de ejecución está a ≤ 2.5% del precio en vivo; si no, rechaza. */
    fun validateAndAudit(transaction: Transaction) {
        val current = _state.value
        if (transaction.transactionId in current.auditingIds) return

        val livePrice = current.livePrice
        if (livePrice == null || livePrice <= 0.0) {
            _state.update { it.copy(message = "No hay precio en vivo disponible para validar.") }
            return
        }

        val deviation = abs(transaction.executionPrice - livePrice) / livePrice
        val isApproved = deviation <= MAX_PRICE_DEVIATION

        _state.update { it.copy(auditingIds = it.auditingIds + transaction.transactionId) }
        viewModelScope.launch {
            val result = transactionRepository.auditTransaction(transaction, isApproved)
            _state.update {
                it.copy(
                    auditingIds = it.auditingIds - transaction.transactionId,
                    message = when (result) {
                        is Resource.Success ->
                            "Auditoría completada. Desviación de precio: ${"%.2f".format(deviation * 100)}%."
                        is Resource.Error -> result.message
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val MAX_PRICE_DEVIATION = 0.025
    }
}