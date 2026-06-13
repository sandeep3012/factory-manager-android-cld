package com.kulhad.manager.ui.screens.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.CustomerRepository
import com.kulhad.manager.domain.model.Customer
import com.kulhad.manager.domain.model.CustomerDetailData
import com.kulhad.manager.domain.model.CustomerWithStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearch(q: String) { _searchQuery.value = q }

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    fun toggleShowInactive() { _showInactive.value = !_showInactive.value }

    val customerList: StateFlow<List<CustomerWithStats>> = combine(
        customerRepository.observeAllWithStats(includeInactive = true),
        _searchQuery,
        _showInactive
    ) { all, query, inactive ->
        val filtered = if (inactive) all else all.filter { it.customer.isActive }
        if (query.isBlank()) filtered
        else filtered.filter {
            it.customer.name.contains(query, ignoreCase = true) ||
                it.customer.mobileNumber.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCustomers: StateFlow<List<Customer>> =
        customerRepository.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Edit support ─────────────────────────────────────────────────────────
    private val _editCustomer = MutableStateFlow<Customer?>(null)
    val editCustomer: StateFlow<Customer?> = _editCustomer.asStateFlow()

    fun loadCustomerForEdit(id: Long) {
        viewModelScope.launch {
            _editCustomer.value = customerRepository.findById(id)
        }
    }

    fun clearEditCustomer() { _editCustomer.value = null }

    // ── Last created ID — auto-select in CreateSaleScreen after quick-add ───
    private val _lastCreatedId = MutableStateFlow<Long?>(null)
    val lastCreatedId: StateFlow<Long?> = _lastCreatedId.asStateFlow()

    fun clearLastCreatedId() { _lastCreatedId.value = null }

    // ── Writes ───────────────────────────────────────────────────────────────
    fun saveCustomer(
        name: String,
        phone: String,
        address: String,
        customerType: String,
        editId: Long? = null,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (editId != null) {
                    customerRepository.updateCustomer(editId, name, phone, address, customerType)
                    onDone(editId)
                } else {
                    val id = customerRepository.addCustomer(name, phone, address, customerType)
                    _lastCreatedId.value = id
                    onDone(id)
                }
            } catch (_: Exception) {}
        }
    }

    fun deactivateCustomer(id: Long) {
        viewModelScope.launch {
            try { customerRepository.deactivateCustomer(id) } catch (_: Exception) {}
        }
    }

    fun reactivateCustomer(id: Long) {
        viewModelScope.launch {
            try { customerRepository.reactivateCustomer(id) } catch (_: Exception) {}
        }
    }
}

// ── Per-customer detail ViewModel ────────────────────────────────────────────

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    customerRepository: CustomerRepository
) : ViewModel() {

    private val customerId: Long = checkNotNull(savedStateHandle["customerId"])

    val detail: StateFlow<CustomerDetailData?> = customerRepository
        .observeCustomerDetail(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
