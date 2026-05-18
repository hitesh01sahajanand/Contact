package com.phonecall.dialcontacts.calldialer.models

data class LanguageModel(
    val flag: Int,
    val nameLocal: String,
    val nameEnglish: String,
    val countryName: String,
    val code: String,
    var isSelected: Boolean = false,
)
