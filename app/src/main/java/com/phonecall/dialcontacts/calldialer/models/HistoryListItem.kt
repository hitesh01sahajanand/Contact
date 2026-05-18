package com.phonecall.dialcontacts.calldialer.models

sealed class HistoryListItem {
    data class Header(val title: String) : HistoryListItem()
    data class History(val data: CallLogEntry) : HistoryListItem()
}
