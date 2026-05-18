package com.phonecall.dialcontacts.calldialer.models


sealed class CallHistoryListItems {
    data class Header(val title: String) : CallHistoryListItems()
    data class Contact(val data: CallLogEntry) : CallHistoryListItems()
    object Loader : CallHistoryListItems()
}