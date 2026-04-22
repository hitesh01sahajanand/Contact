package com.example.contactmanager.models

sealed class HistoryListItem {
    data class Header(val title: String) : HistoryListItem()
    data class History(val data: CallLogEntry) : HistoryListItem()
}
