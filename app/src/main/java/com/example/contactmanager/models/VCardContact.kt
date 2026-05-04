package com.example.contactmanager.models

data class VCardContact(
    var name: String = "",
    val phones: MutableList<String> = mutableListOf(),
    val emails: MutableList<String> = mutableListOf()
)
