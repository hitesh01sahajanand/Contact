package com.phonecall.dialcontacts.calldialer.models

data class VCardContact(
    var name: String = "",
    val phones: MutableList<String> = mutableListOf(),
    val emails: MutableList<String> = mutableListOf()
)
