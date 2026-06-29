package com.example.projectmobile.model

import com.google.firebase.firestore.Exclude

data class PasswordItem(
    @Exclude val id: String = "",
    val userId: String = "",
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val category: String = "Lainnya"
)