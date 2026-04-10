package com.example.contactmanager.models

sealed class ContactListItem {
    data class Header(val title: String) : ContactListItem()
    data class Contact(val data: ContactModel) : ContactListItem()
}