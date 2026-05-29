package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ExpenseTypeDao
import com.kulhad.manager.data.local.entity.ExpenseEntity
import com.kulhad.manager.data.local.entity.ExpenseTypeEntity
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.Expense
import com.kulhad.manager.domain.model.ExpenseType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseTypeDao: ExpenseTypeDao,
    private val userSessionManager: UserSessionManager
) {

    fun observeTypes(): Flow<List<ExpenseType>> =
        expenseTypeDao.observeActive().map { list ->
            list.map { ExpenseType(it.id, it.name, it.isActive) }
        }

    suspend fun addType(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Type name cannot be empty" }
        val existing = expenseTypeDao.findByName(trimmed)
        return existing?.id ?: expenseTypeDao.insert(ExpenseTypeEntity(name = trimmed, isActive = true))
    }

    /**
     * Inserts a new expense row.
     *
     * [date] should be [WorkingDateManager.currentEpochMilli] from the ViewModel —
     * it is normalised to start-of-day and stored as the business date.
     * [auditCreatedAt] is always [System.currentTimeMillis] (via [AuditUtils.createAudit]),
     * keeping the actual write time independent of the chosen business date.
     */
    suspend fun addExpense(typeId: Long, amount: Int, date: Long, remark: String, userId: Long) {
        require(amount > 0) { "Amount must be positive" }
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        expenseDao.insert(
            ExpenseEntity(
                expenseTypeId  = typeId,
                amount         = amount,
                date           = DateUtils.startOfDay(date),
                remark         = remark,
                addedBy        = userId,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
    }

    /**
     * Updates an existing expense row's type, amount, and remark.
     *
     * Preserves the original [date] and creation-audit fields unchanged.
     * Stamps [auditUpdatedBy] / [auditUpdatedAt] via [AuditUtils.updateAudit].
     * No new row is created — this is a true Room UPDATE.
     */
    suspend fun updateExpense(id: Long, typeId: Long, amount: Int, remark: String) {
        require(amount > 0) { "Amount must be positive" }
        val existing = expenseDao.findById(id) ?: return
        val audit = AuditUtils.updateAudit(
            oldCreatedBy = existing.auditCreatedBy,
            oldCreatedAt = existing.auditCreatedAt,
            currentUser  = userSessionManager.currentUser.value
        )
        expenseDao.update(
            existing.copy(
                expenseTypeId  = typeId,
                amount         = amount,
                remark         = remark,
                auditUpdatedBy = audit.updatedBy,
                auditUpdatedAt = audit.updatedAt
            )
        )
    }

    fun observeAll(): Flow<List<Expense>> = combine(
        expenseDao.observeAll(),
        expenseTypeDao.observeActive()
    ) { rows, types ->
        val byId = types.associate { it.id to it.name }
        rows.map { it.toDomain(byId[it.expenseTypeId] ?: "Unknown") }
    }

    fun observeInRange(from: Long, to: Long): Flow<List<Expense>> = combine(
        expenseDao.observeInRange(from, to),
        expenseTypeDao.observeActive()
    ) { rows, types ->
        val byId = types.associate { it.id to it.name }
        rows.map { it.toDomain(byId[it.expenseTypeId] ?: "Unknown") }
    }

    /**
     * Reactive list of expenses for a single calendar day, newest first.
     *
     * [date] is normalised to start-of-day / end-of-day internally so any epoch-millis
     * value within the day (e.g. from [WorkingDateManager.currentEpochMilli]) works.
     *
     * Used by [ExpenseViewModel.historyDayExpenses] to power the date-based history screen.
     */
    fun observeExpensesForDay(date: Long): Flow<List<Expense>> {
        val start = DateUtils.startOfDay(date)
        val end   = DateUtils.endOfDay(start)
        return observeInRange(start, end)
    }

    fun observeTotalThisMonth(): Flow<Int> {
        val from = DateUtils.startOfMonth(System.currentTimeMillis())
        val to = DateUtils.endOfMonth(System.currentTimeMillis())
        return expenseDao.observeTotalInRange(from, to)
    }

    fun observeTotalByTypeThisMonth(typeName: String): Flow<Int> {
        val from = DateUtils.startOfMonth(System.currentTimeMillis())
        val to = DateUtils.endOfMonth(System.currentTimeMillis())
        return combine(
            expenseTypeDao.observeActive(),
            expenseDao.observeInRange(from, to)
        ) { types, expenses ->
            val typeId = types.firstOrNull { it.name.equals(typeName, ignoreCase = true) }?.id
            if (typeId == null) 0
            else expenses.filter { it.expenseTypeId == typeId }.sumOf { it.amount }
        }
    }

    fun observeBreakdownThisMonth(): Flow<List<Pair<String, Int>>> {
        val from = DateUtils.startOfMonth(System.currentTimeMillis())
        val to = DateUtils.endOfMonth(System.currentTimeMillis())
        return combine(
            expenseTypeDao.observeActive(),
            expenseDao.observeBreakdownInRange(from, to)
        ) { types, breakdown ->
            val byId = types.associate { it.id to it.name }
            breakdown.map { (byId[it.typeId] ?: "Other") to it.amount }
        }
    }

    fun observeBreakdownInRange(from: Long, to: Long): Flow<List<Pair<String, Int>>> = combine(
        expenseTypeDao.observeActive(),
        expenseDao.observeBreakdownInRange(from, to)
    ) { types, breakdown ->
        val byId = types.associate { it.id to it.name }
        breakdown.map { (byId[it.typeId] ?: "Other") to it.amount }
    }

    fun observeTotalInRange(from: Long, to: Long): Flow<Int> =
        expenseDao.observeTotalInRange(from, to)

    private fun ExpenseEntity.toDomain(typeName: String) = Expense(
        id       = id,
        typeId   = expenseTypeId,
        typeName = typeName,
        amount   = amount,
        date     = date,
        remark   = remark,
        addedBy  = addedBy,
        audit    = AuditInfo(
            createdBy = auditCreatedBy,
            createdAt = auditCreatedAt,
            updatedBy = auditUpdatedBy,
            updatedAt = auditUpdatedAt
        )
    )
}
