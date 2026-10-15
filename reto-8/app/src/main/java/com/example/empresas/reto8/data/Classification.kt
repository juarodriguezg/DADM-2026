package com.example.empresas.reto8.data

import androidx.annotation.StringRes
import com.example.empresas.reto8.R

/**
 * Clasificación de una empresa. Una empresa puede tener varias ("y/o"), así que se guardan
 * todas juntas en una sola columna INTEGER como máscara de bits.
 */
enum class Classification(val bit: Int, @StringRes val label: Int) {
    CONSULTORIA(1, R.string.class_consultoria),
    DESARROLLO_MEDIDA(2, R.string.class_desarrollo_medida),
    FABRICA_SOFTWARE(4, R.string.class_fabrica_software);

    companion object {
        fun fromMask(mask: Int): Set<Classification> =
            entries.filter { mask and it.bit != 0 }.toSet()
    }
}

fun Set<Classification>.toMask(): Int = fold(0) { acc, c -> acc or c.bit }
