package com.example.todolist

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TodoRepository {
    private val TAG = "TodoRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val tasksCollection = firestore.collection("tasks")  // Usa la colección "tasks"

    // Obtener todas las tareas
    fun getAllTodos(): Flow<List<Todo>> = callbackFlow {
        Log.d(TAG, "Solicitando todas las tareas de Firestore")
        val listenerRegistration = tasksCollection
            .orderBy("priority", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al obtener tareas: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val todos = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Todo.fromDocument(document)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al convertir documento: ${e.message}")
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Recibidas ${todos.size} tareas de Firestore")
                trySend(todos)
            }

        awaitClose {
            Log.d(TAG, "Cerrando listener de Firestore")
            listenerRegistration.remove()
        }
    }

    // Obtener tareas pendientes
    fun getPendingTodos(): Flow<List<Todo>> = callbackFlow {
        val listenerRegistration = tasksCollection
            .whereEqualTo("isCompleted", false)
            .orderBy("priority", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val todos = snapshot?.documents?.mapNotNull { document ->
                    Todo.fromDocument(document)
                } ?: emptyList()

                trySend(todos)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Añadir una nueva tarea
    suspend fun addTodo(todo: Todo): String {
        Log.d(TAG, "Añadiendo nueva tarea: ${todo.title}")

        try {
            val todoMap = todo.toMap()
            Log.d(TAG, "Datos a guardar: $todoMap")

            val documentRef = tasksCollection.add(todoMap).await()
            Log.d(TAG, "Tarea añadida con éxito, ID: ${documentRef.id}")
            return documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir tarea: ${e.message}")
            throw e
        }
    }

    // Actualizar una tarea existente
    suspend fun updateTodo(todo: Todo) {
        Log.d(TAG, "Actualizando tarea: ${todo.title}, ID: ${todo.id}, Completada: ${todo.isCompleted}")

        try {
            val updates = todo.toMap().toMutableMap()
            updates["isCompleted"] = todo.isCompleted

            Log.d(TAG, "Datos a actualizar: $updates")
            tasksCollection.document(todo.id).update(updates).await()
            Log.d(TAG, "Tarea actualizada con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar tarea: ${e.message}")
            throw e
        }
    }

    // Eliminar una tarea
    suspend fun deleteTodo(todo: Todo) {
        Log.d(TAG, "Eliminando tarea: ${todo.title}, ID: ${todo.id}")

        try {
            tasksCollection.document(todo.id).delete().await()
            Log.d(TAG, "Tarea eliminada con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar tarea: ${e.message}")
            throw e
        }
    }

    // Metodo para verificar la conexión a Firestore
    suspend fun testFirestoreConnection() {
        Log.d(TAG, "Iniciando prueba de conexión a Firestore")

        try {
            // 1. Crear un documento de prueba
            val testTodo = Todo(
                id = "",
                title = "Tarea de prueba",
                description = "Esta es una tarea de prueba",
                isCompleted = false,
                priority = 1
            )

            Log.d(TAG, "Creando documento de prueba en Firestore")
            val docId = addTodo(testTodo)
            Log.d(TAG, "Documento de prueba creado con ID: $docId")

            // 2. Verificar la creación
            val createdDoc = tasksCollection.document(docId).get().await()
            val createdTitle = createdDoc.getString("title") ?: ""
            Log.d(TAG, "Documento creado con título: $createdTitle")

            // 3. Actualizar el documento marcándolo como completado
            val updatedTodo = testTodo.copy(id = docId, isCompleted = true)
            Log.d(TAG, "Actualizando documento de prueba (marcando como completado)")
            updateTodo(updatedTodo)

            // 4. Verificar la actualización
            Log.d(TAG, "Verificando la actualización")
            val updatedDoc = tasksCollection.document(docId).get().await()
            val isCompleted = updatedDoc.getBoolean("isCompleted")

            if (isCompleted == true) {
                Log.d(TAG, "¡ÉXITO! El documento se actualizó correctamente a completado=true")
            } else {
                Log.e(TAG, "ERROR: El documento no se actualizó correctamente. Estado: $isCompleted")
            }

            // 5. Eliminar el documento de prueba
            Log.d(TAG, "Eliminando documento de prueba")
            tasksCollection.document(docId).delete().await()
            Log.d(TAG, "Documento de prueba eliminado")

        } catch (e: Exception) {
            Log.e(TAG, "Error durante la prueba de Firestore: ${e.message}", e)
            throw e
        }
    }
}