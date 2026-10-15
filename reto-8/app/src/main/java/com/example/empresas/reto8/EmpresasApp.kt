package com.example.empresas.reto8

import android.app.Application
import com.example.empresas.reto8.data.CompanyDbHelper
import com.example.empresas.reto8.data.CompanyRepository

/** Crea un único repositorio (y una única conexión SQLite) para toda la app. */
class EmpresasApp : Application() {
    val repository by lazy { CompanyRepository(CompanyDbHelper(this)) }
}
