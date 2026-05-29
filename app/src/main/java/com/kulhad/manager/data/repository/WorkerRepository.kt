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
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.PasswordHasher
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AttendanceRecord
import com.kulhad.manager.domain.model.AuditInfo
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
    private val userDao: UserDao,
    private val userSessionManager: UserSessionManager
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
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        typeHistoryDao.insert(
            WorkerTypeHistoryEntity(
                workerId = id,
                workerType = type.name,
                dailyRate = if (type == WorkerType.SALARY) dailyRate else 0,
                effectiveFrom = joiningDate,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
        id
    }

    /**
     * Updates only the profile fields (name, phone, address, joiningDate, isActive).
     * [Worker.currentType] and [Worker.dailyRate] are intentionally ignored — changes
     * to those MUST go through [changeType] or [saveWorkerEdit] so that a
     * [WorkerTypeHistoryEntity] row is always recorded.
     */
    suspend fun updateWorker(worker: Worker) {
        workerDao.updateProfile(
            id          = worker.id,
            name        = worker.name,
            phone       = worker.phone,
            address     = worker.address,
            joiningDate = worker.joiningDate,
            isActive    = worker.isActive
        )
    }

    /**
     * Atomically handles a full worker edit (from AddWorkerScreen).
     *
     * 1. Reads the current DB state so we can compare type/rate.
     * 2. Updates profile fields (name, phone, address, joiningDate) via [updateProfile]
     *    — this path structurally cannot touch current_type or daily_rate.
     * 3. If [newType] or [newDailyRate] differ from the stored values:
     *    - Updates current_type / daily_rate on the worker row.
     *    - Inserts a [WorkerTypeHistoryEntity] row.
     *    Steps 2–3 are in a single [withTransaction], so either both commit or
     *    neither does — the worker cache and history table are always consistent.
     */
    suspend fun saveWorkerEdit(
        workerId    : Long,
        name        : String,
        phone       : String,
        address     : String,
        joiningDate : Long,
        newType     : WorkerType,
        newDailyRate: Int
    ) = database.withTransaction {
        val current = workerDao.findById(workerId)
            ?: error("Worker $workerId not found")

        // Profile-only update — type/rate columns are NOT touched.
        workerDao.updateProfile(
            id          = workerId,
            name        = name,
            phone       = phone,
            address     = address,
            joiningDate = joiningDate,
            isActive    = current.isActive  // deactivation is a separate explicit operation
        )

        // Conditionally update type/rate + append history row.
        val resolvedRate = if (newType == WorkerType.SALARY) newDailyRate else 0
        if (current.currentType != newType.name || current.dailyRate != resolvedRate) {
            workerDao.updateTypeAndRate(workerId, newType.name, resolvedRate)
            val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
            typeHistoryDao.insert(
                WorkerTypeHistoryEntity(
                    workerId      = workerId,
                    workerType    = newType.name,
                    dailyRate     = resolvedRate,
                    effectiveFrom = DateUtils.todayStart(),
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
        }
    }

    suspend fun changeType(workerId: Long, newType: WorkerType, dailyRate: Int, effectiveFrom: Long) {
        database.withTransaction {
            workerDao.updateTypeAndRate(
                workerId,
                newType.name,
                if (newType == WorkerType.SALARY) dailyRate else 0
            )
            val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
            typeHistoryDao.insert(
                WorkerTypeHistoryEntity(
                    workerId = workerId,
                    workerType = newType.name,
                    dailyRate = if (newType == WorkerType.SALARY) dailyRate else 0,
                    effectiveFrom = effectiveFrom,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
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
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        attendanceDao.upsert(
            AttendanceEntity(
                workerId       = workerId,
                date           = DateUtils.startOfDay(date),
                isPresent      = present,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
    }

    suspend fun saveAttendanceBatch(date: Long, presence: Map<Long, Boolean>) {
        val day = DateUtils.startOfDay(date)
        val user = userSessionManager.currentUser.value
        val now  = System.currentTimeMillis()

        // For each worker: read the existing row to preserve creation audit and row ID.
        // CONFLICT_REPLACE on upsertAll would otherwise wipe the original audit_created_by.
        val merged = presence.map { (workerId, isPresent) ->
            val existing = attendanceDao.findByWorkerAndDate(workerId, day)
            AttendanceEntity(
                id        = existing?.id ?: 0L,
                workerId  = workerId,
                date      = day,
                isPresent = isPresent,
                // Preserve original creation stamp; stamp update fields if row existed.
                auditCreatedBy = existing?.auditCreatedBy ?: user,
                auditCreatedAt = existing?.auditCreatedAt ?: now,
                auditUpdatedBy = if (existing != null) user else null,
                auditUpdatedAt = if (existing != null) now  else null
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

    /**
     * Returns a reactive list of attendance records for [date], optionally scoped to one worker.
     *
     * Pass [workerId] = null to get all workers' records for that date.
     * [date] is normalized to start-of-day before querying so timezone mismatches don't
     * produce empty results.
     */
    fun observeAttendanceHistory(date: Long, workerId: Long?): Flow<List<AttendanceRecord>> =
        attendanceDao.observeAttendanceHistory(DateUtils.startOfDay(date), workerId)
            .map { rows ->
                rows.map {
                    AttendanceRecord(
                        workerId  = it.workerId,
                        date      = it.date,
                        isPresent = it.isPresent,
                        audit     = AuditInfo(
                            createdBy = it.auditCreatedBy,
                            createdAt = it.auditCreatedAt,
                            updatedBy = it.auditUpdatedBy,
                            updatedAt = it.auditUpdatedAt
                        )
                    )
                }
            }

    /**
     * Updates the presence flag on an existing attendance row (edit-only, never inserts).
     *
     * Stamps audit_updated_by / audit_updated_at with the current user and wall-clock time.
     * If no row exists for (workerId, date), the underlying UPDATE is a safe no-op.
     */
    suspend fun editAttendance(workerId: Long, date: Long, isPresent: Boolean) {
        attendanceDao.updateAttendance(
            workerId   = workerId,
            date       = DateUtils.startOfDay(date),
            isPresent  = isPresent,
            updatedBy  = userSessionManager.currentUser.value,
            updatedAt  = System.currentTimeMillis()
        )
    }

    /** Mark a worker as present for a date if no row exists. Used by production entries. */
    suspend fun ensureMarkedPresent(workerId: Long, date: Long) {
        val day = DateUtils.startOfDay(date)
        val existing = attendanceDao.findByWorkerAndDate(workerId, day)
        if (existing == null) {
            val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
            attendanceDao.upsert(
                AttendanceEntity(
                    workerId       = workerId,
                    date           = day,
                    isPresent      = true,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
        }
    }

    // -------- Advances --------

    suspend fun saveAdvance(workerId: Long, amount: Int, date: Long, remark: String) {
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        advanceDao.insert(
            WorkerAdvanceEntity(
                workerId       = workerId,
                amount         = amount,
                date           = DateUtils.startOfDay(date),
                remark         = remark,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
    }

    fun observeAdvances(workerId: Long): Flow<List<WorkerAdvanceRecord>> =
        advanceDao.observeForWorker(workerId).map { list ->
            list.map { it.toDomain() }
        }

    fun observeAllAdvances(): Flow<List<WorkerAdvanceRecord>> =
        advanceDao.observeAll().map { list ->
            list.map { it.toDomain() }
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

internal fun WorkerAdvanceEntity.toDomain(): WorkerAdvanceRecord = WorkerAdvanceRecord(
    id       = id,
    workerId = workerId,
    amount   = amount,
    date     = date,
    remark   = remark,
    audit    = AuditInfo(
        createdBy = auditCreatedBy,
        createdAt = auditCreatedAt,
        updatedBy = auditUpdatedBy,
        updatedAt = auditUpdatedAt
    )
)
