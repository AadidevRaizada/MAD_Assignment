package com.mad.n152.todo.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.mad.n152.todo.model.Priority
import com.mad.n152.todo.model.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * All four CRUD operations against the Firebase Realtime Database node `/tasks`.
 *
 * Read is a *stream*, not a one-shot fetch: `addValueEventListener` fires once with the current
 * value and then again on every server-side change, so the UI stays live without polling. The
 * listener is bridged into a Kotlin [Flow] with [callbackFlow], and [awaitClose] removes it when
 * the collector goes away -- without that the listener leaks for the lifetime of the process.
 */
class TaskRepository(
    private val tasksRef: DatabaseReference = Firebase.database.reference.child(NODE_TASKS)
) {

    /** READ -- live list of every task, re-emitted whenever the database changes. */
    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = snapshot.children.mapNotNull { child ->
                    child.getValue(Task::class.java)?.copy(id = child.key.orEmpty())
                }
                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        tasksRef.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    /** CREATE -- push() mints a chronologically-ordered unique key on the client. */
    suspend fun addTask(description: String, priority: Priority) {
        val key = tasksRef.push().key ?: return
        val task = Task(
            id = key,
            description = description.trim(),
            priority = priority.name,
            createdAt = System.currentTimeMillis()
        )
        tasksRef.child(key).setValue(task.toMap()).await()
    }

    /** UPDATE -- updateChildren() patches only the two fields the editor can change. */
    suspend fun updateTask(id: String, description: String, priority: Priority) {
        tasksRef.child(id).updateChildren(
            mapOf(
                "description" to description.trim(),
                "priority" to priority.name
            )
        ).await()
    }

    /** DELETE -- removing the node removes the task for every connected client. */
    suspend fun deleteTask(id: String) {
        tasksRef.child(id).removeValue().await()
    }

    companion object {
        const val NODE_TASKS = "tasks"
    }
}
