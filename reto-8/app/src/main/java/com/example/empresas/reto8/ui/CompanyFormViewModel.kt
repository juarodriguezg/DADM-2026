package com.example.empresas.reto8.ui

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.empresas.reto8.EmpresasApp
import com.example.empresas.reto8.R
import com.example.empresas.reto8.data.Classification
import com.example.empresas.reto8.data.Company
import com.example.empresas.reto8.data.CompanyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class FormState(
    val name: String = "",
    val url: String = "",
    val phone: String = "",
    val email: String = "",
    val products: String = "",
    val classifications: Set<Classification> = emptySet(),
)

data class FormErrors(
    @StringRes val name: Int? = null,
    @StringRes val url: Int? = null,
    @StringRes val email: Int? = null,
    @StringRes val classification: Int? = null,
) {
    val isEmpty get() = name == null && url == null && email == null && classification == null
}

enum class FormEvent { SAVED, DELETED }

/** Estado del formulario de crear/editar. Vive en el ViewModel, así que rotar no pierde lo escrito. */
class CompanyFormViewModel(
    private val repository: CompanyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val companyId: Long = savedStateHandle[ARG_ID] ?: NEW_COMPANY
    val isEditing get() = companyId != NEW_COMPANY

    var state by mutableStateOf(FormState())
        private set
    var errors by mutableStateOf(FormErrors())
        private set
    var loaded by mutableStateOf(!isEditing)
        private set

    private val _events = Channel<FormEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (isEditing) viewModelScope.launch {
            repository.getById(companyId)?.let {
                state = FormState(it.name, it.url, it.phone, it.email, it.products, it.classifications)
            }
            loaded = true
        }
    }

    fun update(transform: FormState.() -> FormState) {
        state = state.transform()
        // Al corregir un campo se limpia su error, sin esperar a "Guardar"
        if (!errors.isEmpty) errors = validate(state, onlyClearing = errors)
    }

    fun toggle(classification: Classification) = update {
        copy(classifications = if (classification in classifications) classifications - classification
        else classifications + classification)
    }

    fun save() {
        val result = validate(state)
        errors = result
        if (!result.isEmpty) return
        val company = Company(
            id = companyId.coerceAtLeast(0),
            name = state.name.trim(),
            url = state.url.trim(),
            phone = state.phone.trim(),
            email = state.email.trim(),
            products = state.products.trim(),
            classifications = state.classifications,
        )
        viewModelScope.launch {
            if (isEditing) repository.update(company) else repository.insert(company)
            _events.send(FormEvent.SAVED)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(companyId)
            _events.send(FormEvent.DELETED)
        }
    }

    private fun validate(s: FormState, onlyClearing: FormErrors? = null): FormErrors {
        val full = FormErrors(
            name = R.string.error_name_required.takeIf { s.name.isBlank() },
            url = R.string.error_url_invalid.takeIf {
                s.url.isNotBlank() && !Patterns.WEB_URL.matcher(s.url.trim()).matches()
            },
            email = R.string.error_email_invalid.takeIf {
                s.email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(s.email.trim()).matches()
            },
            classification = R.string.error_classification_required.takeIf { s.classifications.isEmpty() },
        )
        if (onlyClearing == null) return full
        // Mientras se escribe solo se quitan errores, no se agregan nuevos
        return FormErrors(
            name = full.name.takeIf { onlyClearing.name != null },
            url = full.url.takeIf { onlyClearing.url != null },
            email = full.email.takeIf { onlyClearing.email != null },
            classification = full.classification.takeIf { onlyClearing.classification != null },
        )
    }

    companion object {
        const val ARG_ID = "id"
        const val NEW_COMPANY = -1L

        val Factory = viewModelFactory {
            initializer {
                CompanyFormViewModel(
                    (this[APPLICATION_KEY] as EmpresasApp).repository,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
