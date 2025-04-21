package com.example.todolist

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val PinkPrimary = Color(0xFFF48FB1)        // Rosa medio
private val PinkLight = Color(0xFFFFC1E3)          // Rosa claro
private val PinkDark = Color(0xFFBF5F82)           // Rosa oscuro
private val TextOnPink = Color(0xFF442C2E)         // Texto oscuro para fondos claros

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(todoViewModel: TodoViewModel = viewModel()) {
    // Obtenemos las tareas del ViewModel
    val todos by todoViewModel.todoList.collectAsState()
    val uiState by todoViewModel.uiState.collectAsState()

    // Estados para controlar los diálogos
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var currentTodo by remember { mutableStateOf<Todo?>(null) }

    // Estado para los mensajes de error o confirmación
    val snackbarHostState = remember { SnackbarHostState() }

    // Log para depuración
    LaunchedEffect(Unit) {
        Log.d("TodoScreen", "Conectado a Firestore")
    }

    // Mostrar mensaje según el estado UI
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as UiState.Error).message
                )
            }
            UiState.Success -> {
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tareas", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PinkPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PinkLight.copy(alpha = 0.1f))
        ) {
            if (uiState is UiState.Loading) {
                // Mostrar indicador de carga
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PinkPrimary
                )
            } else if (todos.isEmpty()) {
                // No mostrar ningún mensaje cuando no hay tareas
                Text(
                    text = "No hay tareas. ¡Añade una!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = PinkDark.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(todos) { todo ->
                        SimpleTodoItem(
                            todo = todo,
                            onCheckedChange = { isChecked ->
                                // Actualizar el estado de la tarea en Firestore
                                todoViewModel.updateTodoStatus(todo, isChecked)
                            },
                            onEdit = {
                                currentTodo = todo
                                showEditDialog = true
                            },
                            onDelete = {
                                // Eliminar la tarea en Firestore
                                todoViewModel.deleteTodo(todo)
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para añadir tarea
    if (showAddDialog) {
        SimpleTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, priority ->
                // Añadir una nueva tarea en Firestore
                todoViewModel.addTodo(title, description, priority)
                showAddDialog = false
            }
        )
    }

    // Diálogo para editar tarea
    if (showEditDialog && currentTodo != null) {
        SimpleTodoDialog(
            onDismiss = { showEditDialog = false },
            onConfirm = { title, description, priority ->
                // Actualizar la tarea existente en Firestore
                val updatedTodo = currentTodo!!.copy(
                    title = title,
                    description = description,
                    priority = priority
                )
                todoViewModel.updateTodo(updatedTodo)
                showEditDialog = false
            },
            initialTitle = currentTodo?.title ?: "",
            initialDescription = currentTodo?.description ?: "",
            initialPriority = currentTodo?.priority ?: 1
        )
    }
}

@Composable
fun SimpleTodoItem(
    todo: Todo,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox para marcar tarea como completada
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = PinkPrimary,
                        uncheckedColor = PinkDark
                    )
                )

                // Contenido de la tarea
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (todo.isCompleted) PinkDark.copy(alpha = 0.7f) else TextOnPink
                    )

                    if (todo.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnPink.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    PriorityBadge(priority = todo.priority)
                }

                // Botones de acciones
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = PinkPrimary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = PinkDark
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Int) {
    val (backgroundColor, text) = when (priority) {
        3 -> Pair(Color(0xFFF48FB1), "Alta")    // Rosa más intenso
        2 -> Pair(Color(0xFFFFC1E3), "Media")   // Rosa medio
        else -> Pair(Color(0xFFFFF9C4), "Baja") // Amarillo claro
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = TextOnPink
        )
    }
}

@Composable
fun SimpleTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, priority: Int) -> Unit,
    initialTitle: String = "",
    initialDescription: String = "",
    initialPriority: Int = 1
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var priority by remember { mutableStateOf(initialPriority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tarea", color = PinkDark) },
        containerColor = Color.White,
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Prioridad:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PinkDark
                )

                Row(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    PriorityButton(
                        text = "Baja",
                        isSelected = priority == 1,
                        onClick = { priority = 1 },
                        color = Color(0xFFFFF9C4)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PriorityButton(
                        text = "Media",
                        isSelected = priority == 2,
                        onClick = { priority = 2 },
                        color = Color(0xFFFFC1E3)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PriorityButton(
                        text = "Alta",
                        isSelected = priority == 3,
                        onClick = { priority = 3 },
                        color = Color(0xFFF48FB1)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, priority)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PinkDark)
            }
        }
    )
}


@Composable
fun TestFirestoreButton(viewModel: TodoViewModel) {
    Button(
        onClick = { viewModel.testFirestoreConnection() },
        colors = ButtonDefaults.buttonColors(
            containerColor = PinkDark
        ),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Probar conexión Firestore", color = Color.White)
    }
}

@Composable
fun PriorityButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val buttonColor = if (isSelected) color else Color.LightGray.copy(alpha = 0.3f)
    val textColor = if (isSelected) TextOnPink else TextOnPink.copy(alpha = 0.5f)
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.bodySmall
        )
    }

}