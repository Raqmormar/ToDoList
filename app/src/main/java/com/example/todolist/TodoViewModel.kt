package com.example.todolist

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class TodoViewModel(
    private val repository: TodoRepository = TodoRepository(),
    private val context: Context? = null
) : ViewModel() {
    private val TAG = "TodoViewModel"

    // Estado para indicar carga o errores
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    // Lista de todas las tareas
    val todoList = repository.getAllTodos()
        .catch { e ->
            Log.e(TAG, "Error al obtener tareas: ${e.message}")
            _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            showToast("Error al cargar tareas: ${e.message}")
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista de tareas pendientes
    val pendingTodos = repository.getPendingTodos()
        .catch { e ->
            Log.e(TAG, "Error al obtener tareas pendientes: ${e.message}")
            _uiState.value = UiState.Error(e.message ?: "Error desconocido")
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Agregar una nueva tarea
    fun addTodo(title: String, description: String, priority: Int = 1, dueDate: Date? = null) {
        Log.d(TAG, "Añadiendo tarea: $title")
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val newTodo = Todo(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    createdAt = Date()
                )
                val id = repository.addTodo(newTodo)
                Log.d(TAG, "Tarea añadida con ID: $id")
                _uiState.value = UiState.Success
                showToast("Tarea añadida: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir tarea: ${e.message}")
                _uiState.value = UiState.Error(e.message ?: "Error al añadir tarea")
                showToast("Error al añadir tarea: ${e.message}")
            }
        }
    }

    // Actualizar el estado de una tarea (completada/pendiente)
    fun updateTodoStatus(todo: Todo, isCompleted: Boolean) {
        Log.d(TAG, "Actualizando estado de tarea: ${todo.id} a isCompleted=$isCompleted")

        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val updatedTodo = todo.copy(isCompleted = isCompleted)
                repository.updateTodo(updatedTodo)

                _uiState.value = UiState.Success
                val status = if (isCompleted) "completada" else "pendiente"
                showToast("Tarea marcada como $status")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar estado: ${e.message}")
                _uiState.value = UiState.Error(e.message ?: "Error al actualizar tarea")
                showToast("Error al actualizar estado: ${e.message}")
            }
        }
    }

    // Actualizar todos los datos de una tarea
    fun updateTodo(todo: Todo) {
        Log.d(TAG, "Actualizando tarea: ${todo.id}")
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.updateTodo(todo)
                _uiState.value = UiState.Success
                showToast("Tarea actualizada: ${todo.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar tarea: ${e.message}")
                _uiState.value = UiState.Error(e.message ?: "Error al actualizar tarea")
                showToast("Error al actualizar tarea: ${e.message}")
            }
        }
    }

    // Eliminar una tarea
    fun deleteTodo(todo: Todo) {
        Log.d(TAG, "Eliminando tarea: ${todo.id}")
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.deleteTodo(todo)
                _uiState.value = UiState.Success
                showToast("Tarea eliminada: ${todo.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar tarea: ${e.message}")
                _uiState.value = UiState.Error(e.message ?: "Error al eliminar tarea")
                showToast("Error al eliminar tarea: ${e.message}")
            }
        }
    }

    // Probar la conexión a Firestore
    fun testFirestoreConnection() {
        Log.d(TAG, "Iniciando prueba de conexión a Firestore")
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.testFirestoreConnection()
                _uiState.value = UiState.Success
                showToast("Prueba de conexión completada. Revisa los logs.")
            } catch (e: Exception) {
                Log.e(TAG, "Error en prueba de conexión: ${e.message}")
                _uiState.value = UiState.Error(e.message ?: "Error en prueba")
                showToast("Error en prueba: ${e.message}")
            }
        }
    }

    // Metodo para mostrar Toast
    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}

// Estados de la UI
sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}