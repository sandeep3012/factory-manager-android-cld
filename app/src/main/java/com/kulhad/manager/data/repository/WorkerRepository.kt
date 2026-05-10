package com.kulhad.manager.data.repository

import androidx.room.withTransaction
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.UserDao
import com.kulhad.manager.data.local.dao.WorkerAdvanceDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.dao.WorkerTypeHistoryDao
import com.kulhad.manager.data.local.entity.AttendanceEntity
import com.kulhad.manager.data.local.entity.UserEntity
import com.kulhad.manager.data.local.entity.WorkerAdvanceEntity
import com.kulhad.manager.data.local.entity.WorkerEntity
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.local.entity.WorkerTypeHistoryEntity
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.PasswordHasher
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.domain.model.WorkerAdvanceRecord
import com.kulhad.manager.domain.model.WorkerTypeChange
import com.kulhad.manager.domain.model.WorkerWithAttendance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class WorkerRepository @Inject constructor(
    private val database: KulhadDatabase,
    private val workerDao: WorkerDao,
    private val typeHistoryDao: WorkerTypeHistoryDao,
    private val advanceDao: WorkerAdvanceDao,
    private val attendanceDao: AttendanceDao,
    private val userDao: UserDao
) {

    // -------- Auth --------

    suspend fun login(email: String, password: String): UserEntity? {
        val candidate = userDao.findByEmail(email.trim().lowercase()) ?: return null
        return if (candidate.passwordHash == PasswordHasher.sha256(password)) candidate else null
    }

    suspend fun userById(id: Long): UserEntity? = userDao.findById(id)

    // -------- Workers --------

    fun observeActiveWorkers(): Flow<List<Worker>> =
        workerDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAllWorkers(): Flow<List<Worker>> =
        workerDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveCount(): Flow<Int> = workerDao.observeActiveCount()

    fun observeWorkersWithTodayAttendance(): Flow<List<WorkerWithAttendance>> {
        val today = DateUtils.todayStart()
        return combine(
            workerDao.observeAll(),
            attendanceDao.observeByDate(today)
        ) { workers, todays ->
            val map = todays.associateBy { it.workerId }
            workers.map { w ->
                WorkerWithAttendance(
                    worker = w.toDomain(),
                    isPresentToday = map[w.id]?.isPresent
                )
            }
        }
    }

    suspend fun getWorker(id: Long): Worker? = workerDao.findById(id)?.toDomain()

    fun observeWorker(id: Long): Flow<Worker?> =
        workerDao.observeById(id).map { it?.toDomain() }

    suspend fun saveNewWorker(
        name: String,
        phone: String,
        address: String,
        joiningDate: Long,
        type: WorkerType,
        dailyRate: Int
    ): Long = database.withTransaction {
        val id = workerDao.insert(
            WorkerEntity(
                name = name,
                phone = phone,
                address = address,
                joiningDate = joiningDate,
                currentType = type.name,
                dailyRate = if (type == WorkerType.SALARY) dailyRate else 0,
                isActive = true
            )
        )
        typeHistoryDao.insert(
            WorkerTypeHistoryEntity(
                workerId = id,
                workerType = type.name,
                dailyRate = if (type == WorkerType.SALARY) dailyRate else 0,
                effectiveFrom = joiningDate
            )
        )
        id
    }

    suspend fun updateWorker(worker: Worker) {
        workerDao.update(
            WorkerEntity(
                id = worker.id,
                name = worker.name,
                phone = worker.phone,
                address = worker.address,
                joiningDate = worker.joiningDate,
                currentType = worker.currentType.name,
                dailyRate = worker.dailyRate,
                isActive = worker.isActive
            )
        )
    }

    suspend fun changeType(workerId: Long, newType: WorkerType, dailyRate: Int, effectiveFrom: Long) {
        database.withTransaction {
            workerDao.updateTypeAndRate(
                workerId,
                newType.name,
                if (newType == WorkerType.SALARY) dailyRate else 0
            )
            typeHistoryDao.insert(
                WorkerTypeHistoryEntity(
                    workerId = workerId,
                    workerType = newType.name,
                    dailyRate = if (newType == WorkerType.SALARY) dailyRate else 0,
                    effectiveFrom = effectiveFrom
                )
            )
        }
    }

    fun observeTypeHistory(workerId: Long): Flow<List<WorkerTypeChange>> =
        typeHistoryDao.observeForWorker(workerId).map { list ->
            list.map {
                WorkerTypeChange(
                    id = it.id,
                    workerId = it.workerId,
                    workerType = WorkerType.valueOf(it.workerType),
                    dailyRate = it.dailyRate,
                    effectiveFrom = it.effectiveFrom
                )
            }
        }

    // -------- Attendance --------

    suspend fun saveAttendance(workerId: Long, date: Long, present: Boolean) {
        attendanceDao.upsert(
            AttendanceEntity(
                workerId = workerId,
                date = DateUtils.startOfDay(date),
                isPresent = present
            )
        )
    }

    suspend fun saveAttendanceBatch(date: Long, presence: Map<Long, Boolean>) {
        val day = DateUtils.startOfDay(date)
        // Need to preserve any existing row IDs so we update them rather than create dupes.
        val merged = presence.map { (workerId, isPresent) ->
            val existing = attendanceDao.findByWorkerAndDate(workerId, day)
            AttendanceEntity(
                id = existing?.id ?: 0L,
                workerId = workerId,
                date = day,
                isPresent = isPresent
            )
        }
        attendanceDao.upsertAll(merged)
    }

    fun observeAttendanceForDate(date: Long): Flow<Map<Long, Boolean>> =
        attendanceDao.observeByDate(DateUtils.startOfDay(date))
            .map { rows -> rows.associate { it.workerId to it.isPresent } }

    fun observePresentCountToday(): Flow<Int> =
        attendanceDao.observePresentCount(DateUtils.todayStart())

    fun observeAbsentCountToday(): Flow<Int> =
        attendanceDao.observeAbsentCount(DateUtils.todayStart())

    fun observeAttendanceTrend(days: Int = 7): Flow<List<Pair<Long, Int>>> {
        val starts = (days - 1 downTo 0).map { DateUtils.addDays(DateUtils.todayStart(), -it) }
        val from = starts.first()
        val to = DateUtils.endOfDay(starts.last())
        return attendanceDao.observeRangeCounts(from, to).map { rows ->
            val byDay = rows.associate { it.day to it.presentCount }
            starts.map { it to (byDay[it] ?: 0) }
        }
    }

    /** Mark a worker as present for a date if no row exists. Used by production entries. */
    suspend fun ensureMarkedPresent(workerId: Long, date: Long) {
        val day = DateUtils.startOfDay(date)
        val existing = attendanceDao.findByWorkerAndDate(workerId, day)
        if (existing == null) {
            attendanceDao.upsert(
                AttendanceEntity(workerId = workerId, date = day, isPresent = true)
            )
        }
    }

    // -------- Advances --------

    suspend fun saveAdvance(workerId: Long, amount: Int, date: Long, remark: String) {
        advanceDao.insert(
            WorkerAdvanceEntity(
                workerId = workerId,
                amount = amount,
                date = DateUtils.startOfDay(date),
                remark = remark
            )
        )
    }

    fun observeAdvances(workerId: Long): Flow<List<WorkerAdvanceRecord>> =
        advanceDao.observeForWorker(workerId).map { list ->
            list.map { WorkerAdvanceRecord(it.id, it.workerId, it.amount, it.date, it.remark) }
        }

    fun observeAllAdvances(): Flow<List<WorkerAdvanceRecord>> =
        advanceDao.observeAll().map { list ->
            list.map { WorkerAdvanceRecord(it.id, it.workerId, it.amount, it.date, it.remark) }
        }

    fun observeAdvanceTotalThisMonth(workerId: Long): Flow<Int> {
        val from = DateUtils.startOfMonth(System.currentTimeMillis())
        val to = DateUtils.endOfMonth(System.currentTimeMillis())
        return advanceDao.observeTotalForWorkerInRange(workerId, from, to)
    }

    suspend fun advanceTotalInRange(workerId: Long, from: Long, to: Long): Int =
        advanceDao.totalForWorkerInRange(workerId, from, to)

    suspend fun firstActiveWorkerOrNull(): Worker? =
        workerDao.observeActive().first().firstOrNull()?.toDomain()
}

internal fun WorkerEntity.toDomain(): Worker = Worker(
    id = id,
    name = name,
    phone = phone,
    address = address,
    joiningDate = joiningDate,
    currentType = runCatching { WorkerType.valueOf(currentType) }.getOrDefault(WorkerType.PIECE),
    dailyRate = dailyRate,
    isActive = isActive
)
