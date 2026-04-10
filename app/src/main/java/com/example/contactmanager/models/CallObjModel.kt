package com.example.contactmanager.models

import android.telecom.Call

data class CallObjModel(
    var call: Call, var state: Int
)