package com.example.app_reto2.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.model.TransactionType
import com.example.app_reto2.data.repository.FinnhubRepository
import com.example.app_reto2.data.repository.TransactionRepository
import com.example.app_reto2.util.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserDashboardState(
    val currencyPair: String = "",
    val price: Double? = null,
    val isLoadingPrice: Boolean = true,
    val isSavingTrade: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class UserDashboardViewModel(
    private val finnhubRepository: FinnhubRepository,
    private val transactionRepository: TransactionRepository,
    private val userIdProvider: () -> String? = { FirebaseAuth.getInstance().uid }
) : ViewModel() {

    private val _state = MutableStateFlow(UserDashboardState(currencyPair = BASE + QUOTE))
    val state: StateFlow<UserDashboardState> = _state.asStateFlow()

    init {
        loadPrice()
    }

    fun loadPrice() {
        _state.update { it.copy(isLoadingPrice = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = finnhubRepository.getRate(BASE, QUOTE)) {
                is Resource.Success -> _state.update {
                    it.copy(price = result.data.rate, isLoadingPrice = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoadingPrice = false, errorMessage = result.message)
                }
            }
        }
    }

    fun executeTrade(amount: Double, tradeType: String) {
        val current = _state.value
        if (current.isSavingTrade) return

        val price = current.price ?: return fail("El precio aún no está disponible.")
        val userId = userIdProvider() ?: return fail("No hay una sesión activa.")
        if (tradeType != TransactionType.BUY && tradeType != TransactionType.SELL) {
            return fail("Tipo de operación inválido.")
        }
        if (!(amount > 0.0) || amount.isInfinite()) {
            return fail("Ingresa una cantidad válida.")
        }

        _state.update { it.copy(isSavingTrade = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val transaction = Transaction(
                userId = userId,
                type = tradeType,
                currencyPair = current.currencyPair,
                amount = amount,
                executionPrice = price,
                status = TransactionStatus.PENDING
            )
            when (val result = transactionRepository.saveTransaction(transaction)) {
                is Resource.Success -> {
                    val label = if (tradeType == TransactionType.BUY) "compra" else "venta"
                    _state.update {
                        it.copy(isSavingTrade = false, successMessage = "Orden de $label registrada correctamente.")
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingTrade = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(errorMessage = message, successMessage = null) }
    }

    private companion object {
        const val BASE = "USD"
        const val QUOTE = "EUR"
    }
}