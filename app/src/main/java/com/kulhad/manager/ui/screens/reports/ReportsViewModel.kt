package com.kulhad.manager.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ReportRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.ProductionReport
import com.kulhad.manager.domain.model.ProfitLossReport
import com.kulhad.manager.domain.model.SalaryReport
import com.kulhad.manager.domain.model.SalesReport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _monthAnchor = MutableStateFlow(System.currentTimeMillis())
    val monthAnchor: StateFlow<Long> = _monthAnchor.asStateFlow()

    private val _profitLoss = MutableStateFlow<ProfitLossReport?>(null)
    val profitLoss: StateFlow<ProfitLossReport?> = _profitLoss.asStateFlow()

    private val _salary = MutableStateFlow<SalaryReport?>(null)
    val salary: StateFlow<SalaryReport?> = _salary.asStateFlow()

    private val _production = MutableStateFlow<ProductionReport?>(null)
    val production: StateFlow<ProductionReport?> = _production.asStateFlow()

    private val _sales = MutableStateFlow<SalesReport?>(null)
    val sales: StateFlow<SalesReport?> = _sales.asStateFlow()

    fun setMonth(anchor: Long) {
        _monthAnchor.value = anchor
        // refresh whatever the current screen is using on the next load*() call
    }

    fun nextMonth() = setMonth(DateUtils.addMonths(_monthAnchor.value, 1))
    fun prevMonth() = setMonth(DateUtils.addMonths(_monthAnchor.value, -1))

    fun loadProfitLoss() {
        viewModelScope.launch {
            _profitLoss.value = reportRepository.profitLossForMonth(_monthAnchor.value)
        }
    }

    fun loadSalary() {
        viewModelScope.launch {
            _salary.value = reportRepository.salaryReportForMonth(_monthAnchor.value)
        }
    }

    fun loadProduction() {
        viewModelScope.launch {
            _production.value = reportRepository.productionReportForMonth(_monthAnchor.value)
        }
    }

    fun loadSales() {
        viewModelScope.launch {
            _sales.value = reportRepository.salesReportForMonth(_monthAnchor.value)
        }
    }
}
