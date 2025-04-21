package com.example.todolist

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Todo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 1,
    val createdAt: Date = Date(),
    val dueDate: Date? = null
) {
    // Convertir a mapa para Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "isCompleted" to isCompleted,
            "priority" to priority,
            "createdAt" to createdAt,
            "dueDate" to dueDate
        )
    }

    companion object {
        // Convertir desde un documento de Firestore
        fun fromDocument(document: DocumentSnapshot): Todo? {
            return try {
                val id = document.id
                val title = document.getString("title") ?: ""
                val description = document.getString("description") ?: ""
                val isCompleted = document.getBoolean("isCompleted") ?: false
                val priority = document.getLong("priority")?.toInt() ?: 1
                val createdAt = document.getTimestamp("createdAt")?.toDate() ?: Date()
                val dueDate = document.getTimestamp("dueDate")?.toDate()

                Todo(
                    id = id,
                    title = title,
                    description = description,
                    isCompleted = isCompleted,
                    priority = priority,
                    createdAt = createdAt,
                    dueDate = dueDate
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}