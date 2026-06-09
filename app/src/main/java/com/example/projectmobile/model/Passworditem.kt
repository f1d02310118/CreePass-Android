package com.example.projectmobile.model

data class PasswordItem(
    val id: Int,
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val category: String = "Lainnya"
)