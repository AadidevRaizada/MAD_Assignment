package com.mad.n152.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mad.n152.todo.data.TaskRepository
import com.mad.n152.todo.model.Priority
import com.mad.n152.todo.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/**
 * Everything the To-Do screen renders, in one immutable snapshot.
 *
 * [tasks] is already sorted -- High first, then Medium, then Low, and oldest-first inside each
 * band -- so the composable just renders the list it is given.
 */
data class TodoUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class TodoViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        repository.observeTasks()
            .onEach { tasks ->
                _uiState.value = TodoUiState(
                    tasks = sortByPriority(tasks),
                    isLoading = false,
                    errorMessage = null
                )
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Could not reach the Realtime Database."
                )
            }
            .launchIn(viewModelScope)
    }

    fun addTask(description: String, priority: Priority) {
        if (description.isBlank()) return
        launchGuarded { repository.addTask(description, priority) }
    }

    fun updateTask(id: String, description: String, priority: Priority) {
        if (id.isBlank() || description.isBlank()) return
        launchGuarded { repository.updateTask(id, description, priority) }
    }

    fun deleteTask(id: String) {
        if (id.isBlank()) return
        launchGuarded { repository.deleteTask(id) }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun launchGuarded(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = throwable.message ?: "That write did not reach Firebase."
                )
            }
        }
    }

    companion object {
        /** High -> Medium -> Low, then oldest-first so a re-prioritised task does not jump around. */
        fun sortByPriority(tasks: List<Task>): List<Task> =
            tasks.sortedWith(
                compareBy({ it.priorityEnum.sortOrder }, { it.createdAt })
            )
    }
}
