package com.example.contactmanager.models

data class LanguageModel(
    val name: String,
    val region: String,
    val code: String,
    var isSelected: Boolean = false
)
