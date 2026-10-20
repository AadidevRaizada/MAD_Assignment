package com.mad.n152.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mad.n152.todo.model.Priority
import com.mad.n152.todo.model.Task
import com.mad.n152.todo.ui.theme.N152Theme

/**
 * One dialog for both CREATE and UPDATE. A [task] with a blank id means "new task", so the
 * caller decides which repository call to make; the dialog only collects a description and a
 * priority.
 */
@Composable
fun TaskEditorDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (String, Priority) -> Unit
) {
    val isNew = task.id.isBlank()
    var description by rememberSaveable(task.id) { mutableStateOf(task.description) }
    var priority by rememberSaveable(task.id) { mutableStateOf(task.priorityEnum) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                PriorityPicker(selected = priority, onSelect = { priority = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(description, priority) },
                enabled = description.isNotBlank()
            ) {
                Text(if (isNew) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PriorityPicker(
    selected: Priority,
    onSelect: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Priority.entries.forEach { priority ->
            PriorityChip(
                priority = priority,
                selected = priority == selected,
                onClick = { onSelect(priority) }
            )
        }
    }
}

/**
 * Shared by the picker (tappable) and by a list row (read-only badge, [onClick] null).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityChip(
    priority: Priority,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        label = { Text(priority.label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = priority.color().copy(alpha = 0.18f),
            selectedLabelColor = priority.color(),
            disabledSelectedContainerColor = priority.color().copy(alpha = 0.18f),
            disabledLabelColor = priority.color()
        ),
        modifier = modifier
    )
}

// ---------------------------------------------------------------- previews

@Preview(showBackground = true, name = "Editor - new task")
@Composable
fun TaskEditorDialogNewPreview() {
    N152Theme(dynamicColor = false) {
        TaskEditorDialog(task = Task(), onDismiss = {}, onConfirm = { _, _ -> })
    }
}

@Preview(showBackground = true, name = "Editor - existing task")
@Composable
fun TaskEditorDialogEditPreview() {
    N152Theme(dynamicColor = false) {
        TaskEditorDialog(
            task = Task("abc", "Revise Firebase security rules", Priority.HIGH.name, 1L),
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Priority picker")
@Composable
fun PriorityPickerPreview() {
    N152Theme(dynamicColor = false) {
        PriorityPicker(selected = Priority.MEDIUM, onSelect = {})
    }
}
