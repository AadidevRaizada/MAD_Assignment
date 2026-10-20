package com.mad.n152.todo.model

/**
 * Task priority. [sortOrder] drives the ordering of the list: High (0) first, Low (2) last.
 */
enum class Priority(val label: String, val sortOrder: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2);

    companion object {
        fun fromName(name: String?): Priority =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}
