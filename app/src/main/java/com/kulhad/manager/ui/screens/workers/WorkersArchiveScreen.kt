package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// ── Filter enum ───────────────────────────────────────────────────────────────

enum class ArchiveFilter { ALL, ACTIVE, INACTIVE }

// ── UI state ──────────────────────────────────────────────────────────────────

data class ArchiveListData(
    val workers: List<Worker>,
    val totalCount: Int,
    val activeCount: Int,
    val inactiveCount: Int
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkersArchiveViewModel @Inject constructor(
    repository: WorkerRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ArchiveFilter.ALL)
    val filter: StateFlow<ArchiveFilter> = _filter.asStateFlow()

    val archiveData: StateFlow<ArchiveListData> = combine(
        repository.observeAllWorkers(),
        _filter
    ) { all, filter ->
        val filtered = when (filter) {
            ArchiveFilter.ALL      -> all
            ArchiveFilter.ACTIVE   -> all.filter { it.isActive }
            ArchiveFilter.INACTIVE -> all.filter { !it.isActive }
        }
        ArchiveListData(
            workers      = filtered,
            totalCount   = all.size,
            activeCount  = all.count { it.isActive },
            inactiveCount = all.count { !it.isActive }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ArchiveListData(emptyList(), 0, 0, 0)
    )

    fun setFilter(filter: ArchiveFilter) {
        _filter.value = filter
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WorkersArchiveScreen(
    onBack: () -> Unit,
    onEditWorker: (Long) -> Unit,
    onWorkerHistory: (Long) -> Unit,
    viewModel: WorkersArchiveViewModel = hiltViewModel()
) {
    val data by viewModel.archiveData.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(
            title    = "Workers Archive",
            subtitle = "${data.totalCount} total · ${data.activeCount} active · ${data.inactiveCount} inactive",
            onBack   = onBack
        )

        SegmentedControl(
            options  = listOf("All", "Active", "Inactive"),
            selected = when (filter) {
                ArchiveFilter.ALL      -> "All"
                ArchiveFilter.ACTIVE   -> "Active"
                ArchiveFilter.INACTIVE -> "Inactive"
            },
            onSelect = {
                viewModel.setFilter(
                    when (it) {
                        "Active"   -> ArchiveFilter.ACTIVE
                        "Inactive" -> ArchiveFilter.INACTIVE
                        else       -> ArchiveFilter.ALL
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )

        if (data.workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text  = when (filter) {
                            ArchiveFilter.ACTIVE   -> "No active workers"
                            ArchiveFilter.INACTIVE -> "No inactive workers"
                            ArchiveFilter.ALL      -> "No workers added yet"
                        },
                        color    = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding     = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(data.workers, key = { it.id }) { worker ->
                    val isLast = worker == data.workers.last()
                    ArchiveWorkerRow(
                        worker          = worker,
                        onClick         = { onEditWorker(worker.id) },
                        onHistory       = { onWorkerHistory(worker.id) },
                        isLast          = isLast
                    )
                }
            }
        }
    }
}

// ── Row composable ────────────────────────────────────────────────────────────

@Composable
private fun ArchiveWorkerRow(
    worker: Worker,
    onClick: () -> Unit,
    onHistory: () -> Unit,
    isLast: Boolean = false
) {
    val isActive = worker.isActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Dim inactive rows slightly — same pattern used in Product Master / Stock screens
            .alpha(if (isActive) 1f else 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkerAvatar(name = worker.name, size = 38.dp, fontSize = 12)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = worker.name,
                    color      = if (isActive) TextPrimary else TextSecondary,
                    fontSize   = 14.sp,
                    fontWeight = if (isActive) FontWeight.W500 else FontWeight.W400
                )
                val typeStr = if (worker.currentType == WorkerType.PIECE)
                    "Piece · ${worker.phone}"
                else
                    "Salary · ${Money.formatRupees(worker.dailyRate)}/day"
                Text(text = typeStr, color = TextSecondary, fontSize = 12.sp)
            }

            // Active → green badge, Inactive → amber badge
            if (isActive) {
                StatusBadge("Active", BadgeType.SUCCESS)
            } else {
                StatusBadge("Inactive", BadgeType.WARNING)
            }

            // Edit button → AddWorkerScreen pre-filled with this worker
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint     = PrimaryBlue,
                    modifier = Modifier.size(17.dp)
                )
            }

            // History button → WorkerHistoryScreen for this worker
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .clickable { onHistory() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = "History",
                    tint     = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(OverlayWhite07)
            )
        }
    }
}
