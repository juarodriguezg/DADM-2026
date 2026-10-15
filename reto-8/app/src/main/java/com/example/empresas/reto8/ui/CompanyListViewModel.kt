package com.example.empresas.reto8.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.empresas.reto8.EmpresasApp
import com.example.empresas.reto8.data.Classification
import com.example.empresas.reto8.data.Company
import com.example.empresas.reto8.data.CompanyRepository
import com.example.empresas.reto8.data.toMask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CompanyListViewModel(private val repository: CompanyRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selected = MutableStateFlow(emptySet<Classification>())
    val selected: StateFlow<Set<Classification>> = _selected.asStateFlow()

    /** null mientras se hace la primera consulta. Se recalcula al filtrar o al cambiar la base. */
    val companies: StateFlow<List<Company>?> =
        combine(_query.debounce(200), _selected, repository.version) { q, s, _ -> q to s.toMask() }
            .mapLatest { (q, mask) -> repository.search(q, mask) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) = _query.update { value }

    fun toggle(classification: Classification) = _selected.update {
        if (classification in it) it - classification else it + classification
    }

    fun delete(company: Company) {
        viewModelScope.launch { repository.delete(company.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CompanyListViewModel((this[APPLICATION_KEY] as EmpresasApp).repository) }
        }
    }
}
