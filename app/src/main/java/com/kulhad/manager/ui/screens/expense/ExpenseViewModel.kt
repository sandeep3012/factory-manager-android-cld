package com.kulhad.manager.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ExpenseRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.domain.model.Expense
import com.kulhad.manager.domain.model.ExpenseType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseTabData(
    val totalThisMonth: Int,
    val laborTotal: Int,
    val soilTotal: Int,
    val transportTotal: Int,
    val breakdown: List<Pair<String, Int>>,
    val recent: List<Expense>
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val expenseTypes: StateFlow<List<ExpenseType>> = expenseRepository.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tabData: StateFlow<ExpenseTabData> = run {
        val from = DateUtils.startOfMonth(System.currentTimeMillis())
        val to = DateUtils.endOfMonth(System.currentTimeMillis())
        val totalsFlow = combine(
            expenseRepository.observeTotalInRange(from, to),
            expenseRepository.observeTotalByTypeThisMonth("Labor"),
            expenseRepository.observeTotalByTypeThisMonth("Soil"),
            expenseRepository.observeTotalByTypeThisMonth("Transport")
        ) { total, labor, soil, transport ->
            intArrayOf(total, labor, soil, transport)
        }
        combine(
            totalsFlow,
            expenseRepository.observeBreakdownThisMonth(),
            expenseRepository.observeAll()
        ) { numbers, breakdown, all ->
            ExpenseTabData(
                totalThisMonth = numbers[0],
                laborTotal = numbers[1],
                soilTotal = numbers[2],
                transportTotal = numbers[3],
                breakdown = breakdown,
                recent = all.take(20)
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            ExpenseTabData(0, 0, 0, 0, emptyList(), emptyList())
        )
    }

    fun saveExpense(typeId: Long, amount: Int, date: Long, remark: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                expenseRepository.addExpense(typeId, amount, date, remark, sessionManager.currentUserId)
                onDone()
            } catch (_: Exception) {}
        }
    }

    fun addExpenseType(name: String) {
        viewModelScope.launch {
            try {
                expenseRepository.addType(name)
            } catch (_: Exception) {}
        }
    }
}
