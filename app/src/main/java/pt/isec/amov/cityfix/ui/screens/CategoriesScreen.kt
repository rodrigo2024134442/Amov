package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Category

/**
 * Gestão das categorias pelo operador (enunciado I.3.E): criar, editar, ativar e desativar.
 * Tocar numa categoria abre a caixa de edição; o + cria uma nova.
 */
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onSave: (id: String?, name: String, description: String) -> Unit,
    onActiveChange: (id: String, active: Boolean) -> Unit,
    onCreateDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Caixa de edição aberta: null = fechada; "" = nova categoria; outro valor = ID a editar
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    editingId?.let { id ->
        val category = categories.find { it.id == id }
        CategoryDialog(
            initialName = category?.name ?: "",
            initialDescription = category?.description ?: "",
            onConfirm = { name, description ->
                onSave(category?.id, name, description)
                editingId = null
            },
            onDismiss = { editingId = null }
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { editingId = "" }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_category))
            }
        }
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = onCreateDefaults) { Text(stringResource(R.string.action_create_default_categories)) }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingId = category.id }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            if (category.description.isNotBlank()) {
                                Text(category.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(
                            checked = category.isActive,
                            onCheckedChange = { onActiveChange(category.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    initialName: String,
    initialDescription: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.field_description)) })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, description) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}