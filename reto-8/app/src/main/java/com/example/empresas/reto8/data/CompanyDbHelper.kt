package com.example.empresas.reto8.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Crea y versiona la base SQLite `empresas.db` con la tabla `companies`. */
class CompanyDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_URL TEXT,
                $COL_PHONE TEXT,
                $COL_EMAIL TEXT,
                $COL_PRODUCTS TEXT,
                $COL_CLASSIFICATION INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /** Empresas de ejemplo para que la app no arranque vacía. */
    private fun seed(db: SQLiteDatabase) {
        val samples = listOf(
            Company(
                name = "Andes Software Factory",
                url = "https://andessoftware.example.com",
                phone = "+57 601 555 0101",
                email = "contacto@andessoftware.example.com",
                products = "Desarrollo de apps móviles y web, mantenimiento de sistemas",
                classifications = setOf(Classification.FABRICA_SOFTWARE, Classification.DESARROLLO_MEDIDA),
            ),
            Company(
                name = "Bogotá Tech Consulting",
                url = "https://bogotatech.example.com",
                phone = "+57 601 555 0202",
                email = "info@bogotatech.example.com",
                products = "Arquitectura de software, transformación digital, auditorías de código",
                classifications = setOf(Classification.CONSULTORIA),
            ),
            Company(
                name = "Caribe Code",
                url = "https://caribecode.example.com",
                phone = "+57 605 555 0303",
                email = "hola@caribecode.example.com",
                products = "Software a la medida para logística y turismo",
                classifications = setOf(Classification.DESARROLLO_MEDIDA),
            ),
            Company(
                name = "Pacífico Digital",
                url = "https://pacificodigital.example.com",
                phone = "+57 602 555 0404",
                email = "ventas@pacificodigital.example.com",
                products = "Consultoría en nube, outsourcing de equipos de desarrollo",
                classifications = setOf(Classification.CONSULTORIA, Classification.FABRICA_SOFTWARE),
            ),
        )
        samples.forEach { db.insert(TABLE, null, it.toContentValues()) }
    }

    companion object {
        const val DB_NAME = "empresas.db"
        const val DB_VERSION = 1

        const val TABLE = "companies"
        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_URL = "url"
        const val COL_PHONE = "phone"
        const val COL_EMAIL = "email"
        const val COL_PRODUCTS = "products"
        const val COL_CLASSIFICATION = "classification"
    }
}

fun Company.toContentValues() = ContentValues().apply {
    put(CompanyDbHelper.COL_NAME, name)
    put(CompanyDbHelper.COL_URL, url)
    put(CompanyDbHelper.COL_PHONE, phone)
    put(CompanyDbHelper.COL_EMAIL, email)
    put(CompanyDbHelper.COL_PRODUCTS, products)
    put(CompanyDbHelper.COL_CLASSIFICATION, classifications.toMask())
}
