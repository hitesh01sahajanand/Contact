package com.example.contactmanager.models


sealed class CallHistoryListItems {
    data class Header(val title: String) : CallHistoryListItems()
    data class Contact(val data: CallLogEntry) : CallHistoryListItems()
}