package com.example.contactmanager.viewmodels

import android.telecom.Call
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.contactmanager.utils.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel

class CallViewModel : ViewModel() {
    val callState: LiveData<Pair<Call, Int>> = CallManager.callState
    val callList: LiveData<List<Call>> = CallManager.callList

}