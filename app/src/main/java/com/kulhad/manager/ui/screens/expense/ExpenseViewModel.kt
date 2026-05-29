package com.kulhad.manager.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ExpenseRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.di.WorkingDateManager
import com.kulhad.manager.domain.model.Expense
import com.kulhad.manager.domain.model.ExpenseType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val sessionManager: SessionManager,
    private val workingDateManager: WorkingDateManager
) : ViewModel() {

    // ── Global working date ──────────────────────────────────────────────────
    /**
     * The process-scoped working date from [WorkingDateManager].
     * Delegates the same StateFlow — no state is duplicated.
     * Expense inserts use [workingDateManager.currentEpochMilli] as their business date.
     */
    val workingDate: StateFlow<LocalDate> = workingDateManager.currentWorkingDate

    /** Forwards date selection to [WorkingDateManager]; future dates are silently rejected. */
    fun setWorkingDate(date: LocalDate) = workingDateManager.setWorkingDate(date)

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
                laborTotal     = numbers[1],
                soilTotal      = numbers[2],
                transportTotal = numbers[3],
                breakdown      = breakdown,
                recent         = all.take(20)
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            ExpenseTabData(0, 0, 0, 0, emptyList(), emptyList())
        )
    }

    /**
     * Expense entries for the currently selected [workingDate], newest first.
     *
     * Re-queries whenever the global working date changes via [WorkingDateManager].
     * Uses [flatMapLatest] so the previous DB subscription is cancelled on every date
     * change — stale data from a previous day cannot leak through.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyDayExpenses: StateFlow<List<Expense>> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                expenseRepository.observeExpensesForDay(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Saves a new expense using the current [workingDate] as the business date.
     * The [date] parameter is no longer accepted from callers — the ViewModel owns it.
     */
    fun saveExpense(typeId: Long, amount: Int, remark: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                expenseRepository.addExpense(
                    typeId = typeId,
                    amount = amount,
                    date   = workingDateManager.currentEpochMilli(),
                    remark = remark,
                    userId = sessionManager.currentUserId
                )
                onDone()
            } catch (_: Exception) {}
        }
    }

    /**
     * Updates an existing expense's type, amount, and remark.
     * A true Room UPDATE — no new row is created. Stamps auditUpdatedBy/At.
     */
    fun updateExpense(
        id: Long,
        typeId: Long,
        amount: Int,
        remark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                expenseRepository.updateExpense(id, typeId, amount, remark)
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
