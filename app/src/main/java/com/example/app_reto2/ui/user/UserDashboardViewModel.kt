package com.example.app_reto2.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.model.TransactionType
import com.example.app_reto2.data.repository.FinnhubRepository
import com.example.app_reto2.data.repository.TransactionRepository
import com.example.app_reto2.data.repository.UserRepository
import com.example.app_reto2.util.MarketConfig
import com.example.app_reto2.util.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserDashboardState(
    val currencyPair: String = "",
    val price: Double? = null,
    val isLoadingPrice: Boolean = true,
    val balance: Double? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoadingHistory: Boolean = true,
    val isSavingTrade: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class UserDashboardViewModel(
    private val finnhubRepository: FinnhubRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val userIdProvider: () -> String? = { FirebaseAuth.getInstance().uid }
) : ViewModel() {

    private val _state = MutableStateFlow(UserDashboardState(currencyPair = MarketConfig.CURRENCY_PAIR))
    val state: StateFlow<UserDashboardState> = _state.asStateFlow()

    private var priceJob: Job? = null

    init {
        loadPrice()
        observeUserData()
    }

    /** Abre (o reabre) la conexión de precios en tiempo real. */
    fun loadPrice() {
        priceJob?.cancel()
        _state.update {
            it.copy(
                price = null,
                isLoadingPrice = true,
                statusMessage = null,
                errorMessage = null,
                successMessage = null
            )
        }
        priceJob = viewModelScope.launch {
            val waitNotice = launch {
                delay(NO_TICK_NOTICE_MS)
                _state.update { it.copy(statusMessage = NO_TICK_MESSAGE) }
            }
            finnhubRepository.observePrice(MarketConfig.SYMBOL).collect { result ->
                waitNotice.cancel()
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(price = result.data, isLoadingPrice = false, statusMessage = null)
                    }
                    is Resource.Error -> _state.update {
                        it.copy(
                            price = null,
                            isLoadingPrice = false,
                            statusMessage = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /** Mantiene actualizados el saldo y el historial del usuario autenticado. */
    private fun observeUserData() {
        val userId = userIdProvider()
        if (userId == null) {
            _state.update {
                it.copy(isLoadingHistory = false, errorMessage = "No hay una sesión activa.")
            }
            return
        }

        viewModelScope.launch {
            userRepository.observeUserBalance(userId)
                .catch { _state.update { it.copy(errorMessage = "No se pudo cargar tu saldo.") } }
                .collect { balance -> _state.update { it.copy(balance = balance) } }
        }

        viewModelScope.launch {
            transactionRepository.getUserTransactions(userId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(transactions = result.data, isLoadingHistory = false)
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoadingHistory = false, errorMessage = result.message)
                    }
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
        if (tradeType == TransactionType.BUY) {
            val balance = current.balance ?: return fail("Tu saldo aún no está disponible.")
            if (amount * price > balance) return fail("Saldo insuficiente para esta compra.")
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
        const val NO_TICK_NOTICE_MS = 8_000L
        const val NO_TICK_MESSAGE =
            "Esperando cotizaciones… Si el mercado forex está cerrado (viernes en la tarde a domingo), no habrá precios hasta que abra."
    }
}