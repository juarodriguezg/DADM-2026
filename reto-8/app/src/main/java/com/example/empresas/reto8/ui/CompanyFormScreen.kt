package com.example.empresas.reto8.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.empresas.reto8.R
import com.example.empresas.reto8.data.Classification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFormScreen(
    onDone: () -> Unit,
    viewModel: CompanyFormViewModel = viewModel(factory = CompanyFormViewModel.Factory),
) {
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val msg = if (event == FormEvent.SAVED) R.string.company_saved else R.string.company_deleted
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (viewModel.isEditing) R.string.title_edit_company else R.string.title_new_company))
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (viewModel.isEditing) IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val s = viewModel.state
        val e = viewModel.errors
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = s.name, label = R.string.field_name, error = e.name,
                onChange = { viewModel.update { copy(name = it) } },
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            FormField(
                value = s.url, label = R.string.field_url, error = e.url,
                onChange = { viewModel.update { copy(url = it) } },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            )
            FormField(
                value = s.phone, label = R.string.field_phone, error = null,
                onChange = { viewModel.update { copy(phone = it) } },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
            FormField(
                value = s.email, label = R.string.field_email, error = e.email,
                onChange = { viewModel.update { copy(email = it) } },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FormField(
                value = s.products, label = R.string.field_products, error = null,
                onChange = { viewModel.update { copy(products = it) } },
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = false,
            )

            Text(
                stringResource(R.string.field_classification),
                style = MaterialTheme.typography.titleSmall,
                color = if (e.classification != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Column {
                Classification.entries.forEach { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = c in s.classifications,
                                role = Role.Checkbox,
                                onValueChange = { viewModel.toggle(c) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = c in s.classifications, onCheckedChange = null,
                            modifier = Modifier.padding(12.dp))
                        Text(stringResource(c.label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                e.classification?.let {
                    Text(
                        stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            companyName = viewModel.state.name,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FormField(
    value: String,
    label: Int,
    error: Int?,
    onChange: (String) -> Unit,
    keyboard: KeyboardOptions,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        keyboardOptions = keyboard,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
    )
}
