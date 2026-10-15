package com.example.empresas.reto8.data

import android.database.Cursor
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_CLASSIFICATION
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_EMAIL
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_ID
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_NAME
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_PHONE
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_PRODUCTS
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.COL_URL
import com.example.empresas.reto8.data.CompanyDbHelper.Companion.TABLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Operaciones CRUD sobre la tabla `companies`. Todo el acceso a disco va en Dispatchers.IO. */
class CompanyRepository(private val dbHelper: CompanyDbHelper) {

    // Cambia en cada escritura para que la lista vuelva a consultar la base
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    suspend fun insert(company: Company): Long = write {
        dbHelper.writableDatabase.insert(TABLE, null, company.toContentValues())
    }

    suspend fun update(company: Company): Int = write {
        dbHelper.writableDatabase.update(
            TABLE, company.toContentValues(), "$COL_ID = ?", arrayOf(company.id.toString())
        )
    }

    suspend fun delete(id: Long): Int = write {
        dbHelper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun getById(id: Long): Company? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        ).use { if (it.moveToFirst()) it.toCompany() else null }
    }

    /**
     * Lista las empresas cuyo nombre contiene [name] y que tienen al menos una de las
     * clasificaciones de [classMask] (0 = cualquier clasificación).
     */
    suspend fun search(name: String, classMask: Int): List<Company> = withContext(Dispatchers.IO) {
        val where = StringBuilder("$COL_NAME LIKE ?")
        val args = mutableListOf("%${name.trim()}%")
        if (classMask != 0) {
            where.append(" AND ($COL_CLASSIFICATION & ?) != 0")
            args += classMask.toString()
        }
        dbHelper.readableDatabase.query(
            TABLE, null, where.toString(), args.toTypedArray(),
            null, null, "$COL_NAME COLLATE NOCASE"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toCompany()) }
        }
    }

    private suspend fun <T> write(block: () -> T): T = withContext(Dispatchers.IO) {
        block().also { _version.update { it + 1 } }
    }

    private fun Cursor.toCompany() = Company(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        name = getString(getColumnIndexOrThrow(COL_NAME)),
        url = getString(getColumnIndexOrThrow(COL_URL)).orEmpty(),
        phone = getString(getColumnIndexOrThrow(COL_PHONE)).orEmpty(),
        email = getString(getColumnIndexOrThrow(COL_EMAIL)).orEmpty(),
        products = getString(getColumnIndexOrThrow(COL_PRODUCTS)).orEmpty(),
        classifications = Classification.fromMask(getInt(getColumnIndexOrThrow(COL_CLASSIFICATION))),
    )
}
