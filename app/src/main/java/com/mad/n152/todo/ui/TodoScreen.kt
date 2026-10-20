package com.mad.n152.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mad.n152.todo.model.Priority
import com.mad.n152.todo.model.Task
import com.mad.n152.todo.ui.theme.N152Theme
import com.mad.n152.todo.ui.theme.PriorityHigh
import com.mad.n152.todo.ui.theme.PriorityLow
import com.mad.n152.todo.ui.theme.PriorityMedium

/** Colour for a priority -- one place, so the chip and the row's leading bar never disagree. */
fun Priority.color(): Color = when (this) {
    Priority.HIGH -> PriorityHigh
    Priority.MEDIUM -> PriorityMedium
    Priority.LOW -> PriorityLow
}

/**
 * Screen-level entry point. Owns the ViewModel and unwraps [TodoUiState] for the stateless
 * [TodoScreen] below, which is what the @Preview renders.
 */
@Composable
fun TodoRoute(
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodoScreen(
        uiState = uiState,
        onAdd = viewModel::addTask,
        onUpdate = viewModel::updateTask,
        onDelete = viewModel::deleteTask,
        onErrorShown = viewModel::dismissError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    uiState: TodoUiState,
    onAdd: (String, Priority) -> Unit,
    onUpdate: (String, String, Priority) -> Unit,
    onDelete: (String) -> Unit,
    onErrorShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // null = editor closed; a Task with a blank id = "add"; a Task with an id = "edit that task".
    var editing by rememberSaveable(stateSaver = TaskSaver) { mutableStateOf<Task?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("N152 To-Do") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Task() }) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.tasks.isEmpty() -> Text(
                    text = "No tasks yet.\nTap + to add one.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> TaskList(
                    tasks = uiState.tasks,
                    onEdit = { editing = it },
                    onDelete = onDelete
                )
            }
        }
    }

    editing?.let { task ->
        TaskEditorDialog(
            task = task,
            onDismiss = { editing = null },
            onConfirm = { description, priority ->
                if (task.id.isBlank()) onAdd(description, priority)
                else onUpdate(task.id, description, priority)
                editing = null
            }
        )
    }
}

/**
 * The LazyColumn the task sheet asks for. [tasks] arrives already sorted by priority, and the
 * stable `key` means a re-prioritised task animates to its new slot instead of being recycled
 * into the wrong row.
 */
@Composable
fun TaskList(
    tasks: List<Task>,
    onEdit: (Task) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = tasks, key = { it.id }) { task ->
            SwipeableTaskRow(
                task = task,
                onEdit = { onEdit(task) },
                onDelete = { onDelete(task.id) }
            )
        }
    }
}

/** A task row that deletes itself when swiped right-to-left. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskRow(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        // Require a half-width swipe so a stray horizontal scroll cannot delete a task.
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        TaskRow(task = task, onEdit = onEdit)
    }
}

@Composable
fun TaskRow(
    task: Task,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priority = task.priorityEnum
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Leading colour bar -- priority readable at a glance without reading the chip.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .background(priority.color())
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                PriorityChip(priority = priority, selected = true, onClick = null)
            }
            IconButton(onClick = onEdit, modifier = Modifier.padding(end = 6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit task")
            }
        }
    }
}

/** Saves the in-progress editor target across configuration changes and process death. */
private val TaskSaver = androidx.compose.runtime.saveable.listSaver<Task?, Any>(
    save = { task ->
        task?.let { listOf(it.id, it.description, it.priority, it.createdAt) } ?: emptyList()
    },
    restore = { stored ->
        if (stored.isEmpty()) null
        else Task(
            id = stored[0] as String,
            description = stored[1] as String,
            priority = stored[2] as String,
            createdAt = stored[3] as Long
        )
    }
)

// ---------------------------------------------------------------- previews

private val previewTasks = listOf(
    Task("1", "Submit MAD Firebase assignment", Priority.HIGH.name, 1L),
    Task("2", "Revise Realtime Database security rules", Priority.MEDIUM.name, 2L),
    Task("3", "Back up lab screenshots", Priority.LOW.name, 3L)
)

@Preview(showBackground = true, name = "To-Do list")
@Composable
fun TodoScreenPreview() {
    N152Theme(dynamicColor = false) {
        TodoScreen(
            uiState = TodoUiState(tasks = previewTasks, isLoading = false),
            onAdd = { _, _ -> },
            onUpdate = { _, _, _ -> },
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty state")
@Composable
fun TodoScreenEmptyPreview() {
    N152Theme(dynamicColor = false) {
        TodoScreen(
            uiState = TodoUiState(tasks = emptyList(), isLoading = false),
            onAdd = { _, _ -> },
            onUpdate = { _, _, _ -> },
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Single task row")
@Composable
fun TaskRowPreview() {
    N152Theme(dynamicColor = false) {
        TaskRow(task = previewTasks.first(), onEdit = {})
    }
}
