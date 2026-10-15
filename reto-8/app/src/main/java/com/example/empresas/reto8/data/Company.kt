package com.example.empresas.reto8.data

data class Company(
    val id: Long = 0,
    val name: String,
    val url: String = "",
    val phone: String = "",
    val email: String = "",
    val products: String = "",
    val classifications: Set<Classification> = emptySet(),
)
