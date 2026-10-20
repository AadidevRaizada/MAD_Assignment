package com.mad.n152.todo.model

/**
 * One To-Do item as it is stored under `/tasks/{id}` in the Firebase Realtime Database.
 *
 * Every property needs a default value: Firebase deserialises a [com.google.firebase.database.DataSnapshot]
 * by calling the no-argument constructor and then setting fields, so a class without defaults
 * throws `DatabaseException: Class does not define a no-argument constructor`.
 *
 * [id] is the Realtime Database push key. It is not written into the task node itself -- it is the
 * node's own name -- so it is excluded from the map sent to Firebase by [toMap].
 */
data class Task(
    val id: String = "",
    val description: String = "",
    val priority: String = Priority.MEDIUM.name,
    val createdAt: Long = 0L
) {
    /** Priority as an enum, falling back to MEDIUM if the stored string is unrecognised. */
    val priorityEnum: Priority
        get() = Priority.fromName(priority)

    /** The payload actually written to `/tasks/{id}` -- the key lives in the path, not the value. */
    fun toMap(): Map<String, Any> = mapOf(
        "description" to description,
        "priority" to priority,
        "createdAt" to createdAt
    )
}
