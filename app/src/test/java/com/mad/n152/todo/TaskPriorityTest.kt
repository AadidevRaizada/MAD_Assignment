package com.mad.n152.todo

import com.mad.n152.todo.model.Priority
import com.mad.n152.todo.model.Task
import com.mad.n152.todo.ui.TodoViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskPriorityTest {

    @Test
    fun `tasks are ordered high then medium then low`() {
        val unsorted = listOf(
            Task("a", "low", Priority.LOW.name, 1L),
            Task("b", "high", Priority.HIGH.name, 2L),
            Task("c", "medium", Priority.MEDIUM.name, 3L)
        )

        val sorted = TodoViewModel.sortByPriority(unsorted)

        assertEquals(listOf("b", "c", "a"), sorted.map { it.id })
    }

    @Test
    fun `tasks of equal priority keep oldest-first order`() {
        val unsorted = listOf(
            Task("newer", "second", Priority.HIGH.name, 200L),
            Task("older", "first", Priority.HIGH.name, 100L)
        )

        val sorted = TodoViewModel.sortByPriority(unsorted)

        assertEquals(listOf("older", "newer"), sorted.map { it.id })
    }

    @Test
    fun `an unknown stored priority falls back to medium`() {
        assertEquals(Priority.MEDIUM, Priority.fromName("URGENT"))
        assertEquals(Priority.MEDIUM, Priority.fromName(null))
        assertEquals(Priority.HIGH, Priority.fromName("high"))
    }

    @Test
    fun `the id is not written into the task node`() {
        val task = Task("push-key", "write me", Priority.LOW.name, 42L)

        assertEquals(setOf("description", "priority", "createdAt"), task.toMap().keys)
    }
}
