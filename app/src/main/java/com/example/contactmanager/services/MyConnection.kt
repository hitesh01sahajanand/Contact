package com.example.contactmanager.services

import android.telecom.Connection
import android.telecom.DisconnectCause

class MyConnection: Connection() {
    override fun onAnswer() {
        super.onAnswer()
        setActive()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }
}