package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.WorkerType

data class Worker(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val joiningDate: Long,
    val currentType: WorkerType,
    val dailyRate: Int,
    val isActive: Boolean
)

data class WorkerWithAttendance(
    val worker: Worker,
    val isPresentToday: Boolean?
)

data class WorkerTypeChange(
    val id: Long,
    val workerId: Long,
    val workerType: WorkerType,
    val dailyRate: Int,
    val effectiveFrom: Long
)

data class WorkerAdvanceRecord(
    val id: Long,
    val workerId: Long,
    val amount: Int,
    val date: Long,
    val remark: String
)
