package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ExpenseTypeDao
import com.kulhad.manager.data.local.entity.ExpenseEntity
import com.kulhad.manager.data.local.entity.ExpenseTypeEntity
import com.kulhad.manager.data.util.DateUtils
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
    private val expenseTypeDao: ExpenseTypeDao
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

    suspend fun addExpense(typeId: Long, amount: Int, date: Long, remark: String, userId: Long) {
        require(amount > 0) { "Amount must be positive" }
        expenseDao.insert(
            ExpenseEntity(
                expenseTypeId = typeId,
                amount = amount,
                date = DateUtils.startOfDay(date),
                remark = remark,
                addedBy = userId
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
        id = id,
        typeId = expenseTypeId,
        typeName = typeName,
        amount = amount,
        date = date,
        remark = remark,
        addedBy = addedBy
    )
}
