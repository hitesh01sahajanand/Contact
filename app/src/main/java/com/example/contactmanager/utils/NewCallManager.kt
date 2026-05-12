package com.example.contactmanager.utils

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import java.util.concurrent.CopyOnWriteArraySet
import android.os.Handler
import android.os.Looper
import android.util.Log

class NewCallManager {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var inCallService: InCallService? = null
        private var call: Call? = null
        private val calls = mutableListOf<Call>()
        private val listeners = CopyOnWriteArraySet<CallManagerListener>()
        var isCallActivityVisible: Boolean = false

        fun onCallAdded(call: Call) {
            this.call = call
            calls.add(call)
            for (listener in listeners) {
                listener.onPrimaryCallChanged(call)
            }
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    updateState()
                }

                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateState()
                }

                override fun onConferenceableCallsChanged(
                    call: Call,
                    conferenceableCalls: MutableList<Call>
                ) {
                    updateState()
                }

                override fun onParentChanged(call: Call, parent: Call?) {
                    updateState()
                }

                override fun onChildrenChanged(call: Call, children: MutableList<Call>) {
                    updateState()
                }
            })
        }

        fun onCallRemoved(call: Call) {
            val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
            Log.d("TAG", "launchEndCallActivity: NewCallManager onCallRemoved: $number")
            calls.remove(call)
            updateState()
        }

        private fun updateState() {
            // remove all disconnected calls manually early to avoid picking them as primary
            calls.removeAll { it.getStateCompat() == Call.STATE_DISCONNECTED }

            val primaryCall = when (val phoneState = getPhoneState()) {
                is NoCall -> null
                is SingleCall -> phoneState.call
                is TwoCalls -> phoneState.active
            }
            var notify = true
            if (primaryCall == null) {
                call = null
            } else if (primaryCall != call) {
                call = primaryCall
                for (listener in listeners) {
                    listener.onPrimaryCallChanged(primaryCall)
                }
                notify = false
            }
            if (notify) {
                notifyListeners()
            }
        }

        fun notifyListeners() {
            for (listener in listeners) {
                listener.onStateChanged()
            }
        }

        fun getPhoneState(): PhoneState {
            val topLevelCalls = calls.filter { it.parent == null }
            val nonDisconnected = topLevelCalls.filter {
                val state = it.getStateCompat()
                state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING
            }

            if (nonDisconnected.isEmpty()) {
                return if (topLevelCalls.isEmpty()) NoCall else SingleCall(topLevelCalls.first())
            }

            return when (nonDisconnected.size) {
                1 -> SingleCall(nonDisconnected.first())
                2 -> {
                    val active = nonDisconnected.find { it.getStateCompat() == Call.STATE_ACTIVE }
                    val newCall =
                        nonDisconnected.find { it.getStateCompat() == Call.STATE_RINGING }
                            ?: nonDisconnected.find { it.getStateCompat() == Call.STATE_CONNECTING || it.getStateCompat() == Call.STATE_DIALING }
                    val onHold = nonDisconnected.find { it.getStateCompat() == Call.STATE_HOLDING }

                    if (active != null && newCall != null) {
                        TwoCalls(newCall, active)
                    } else if (newCall != null && onHold != null) {
                        TwoCalls(newCall, onHold)
                    } else if (active != null && onHold != null) {
                        TwoCalls(active, onHold)
                    } else {
                        TwoCalls(nonDisconnected[0], nonDisconnected[1])
                    }
                }

                else -> {
                    val activeConference =
                        nonDisconnected.find { it.isConference() && it.getStateCompat() == Call.STATE_ACTIVE }
                    val conferenceCall = nonDisconnected.find { it.isConference() }

                    val activeOrNew = activeConference
                        ?: nonDisconnected.find { it.getStateCompat() == Call.STATE_ACTIVE }
                        ?: conferenceCall
                        ?: nonDisconnected.find { it.getStateCompat() != Call.STATE_HOLDING }
                        ?: nonDisconnected[0]

                    val onHold =
                        nonDisconnected.find { it != activeOrNew && it.getStateCompat() == Call.STATE_HOLDING }
                            ?: nonDisconnected.find { it != activeOrNew }
                            ?: nonDisconnected[0]
                    TwoCalls(activeOrNew, onHold)
                }
            }
        }

        fun getPrimaryCall(): Call? {
            return call
        }

        fun getConferenceCalls(): List<Call> {
            return calls.find { it.isConference() }?.children ?: emptyList()
        }

        fun accept() {
            call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun reject() {
            // Collect all ringing calls and reject/disconnect them to ensure the network signals are sent
            val ringingCalls = calls.filter { it.getStateCompat() == Call.STATE_RINGING }
            if (ringingCalls.isNotEmpty()) {
                ringingCalls.forEach { ringingCall ->
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            ringingCall.reject(Call.REJECT_REASON_DECLINED)
                        } else {
                            @Suppress("DEPRECATION")
                            ringingCall.reject(false, null)
                        }
                    } catch (e: Exception) {
                        try {
                            @Suppress("DEPRECATION")
                            ringingCall.reject(false, null)
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                    // Force disconnect as well after a short delay to ensure network signal is sent.
                    // Some devices (Samsung, Pixel) may cancel the reject signal if disconnect() is called immediately.
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            if (ringingCall.getStateCompat() != Call.STATE_DISCONNECTED) {
                                ringingCall.disconnect()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 200L)
                }
            } else {
                // If no ringing calls, disconnect the primary call (active/dialing/etc)
                try {
                    val primaryCall = call
                    if (primaryCall != null && primaryCall.getStateCompat() != Call.STATE_DISCONNECTED) {
                        primaryCall.disconnect()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun getState(): Int {
            val call = getPrimaryCall() ?: return Call.STATE_DISCONNECTED
            if (call.isConference()) {
                val children = call.children
                if (children != null && children.isNotEmpty() && children.all { it.getStateCompat() == Call.STATE_HOLDING }) {
                    return Call.STATE_HOLDING
                }
            }
            return call.getStateCompat()
        }

        fun toggleHold(): Boolean {
            val primaryCall = getPrimaryCall() ?: return false
            val isOnHold = getState() == Call.STATE_HOLDING
            if (isOnHold) {
                primaryCall.unhold()
                if (primaryCall.isConference()) {
                    primaryCall.children?.forEach { it.unhold() }
                }
            } else {
                primaryCall.hold()
                if (primaryCall.isConference()) {
                    primaryCall.children?.forEach { it.hold() }
                }
            }
            return !isOnHold
        }

        fun canAddCall(): Boolean {
            val state = getPhoneState()
            return when (state) {
                is NoCall -> true
                is SingleCall -> true
                is TwoCalls -> false // Standard limit is 2 lines before merging
            }
        }

        fun swap() {
            val state = getPhoneState()
            if (state is TwoCalls) {
                val active = state.active
                val onHold = state.onHold

                if (active.getStateCompat() == Call.STATE_ACTIVE) {
                    active.hold()
                    onHold.unhold()
                } else if (active.getStateCompat() == Call.STATE_HOLDING) {
                    active.unhold()
                    onHold.hold()
                } else if (onHold.getStateCompat() == Call.STATE_HOLDING) {
                    onHold.unhold()
                }
            }
        }

        fun merge() {
            val state = getPhoneState()
            if (state is TwoCalls) {
                // If we have a conference already, we merge into it
                if (state.active.isConference()) {
                    state.active.conference(state.onHold)
                } else if (state.onHold.isConference()) {
                    state.onHold.conference(state.active)
                } else {
                    state.active.conference(state.onHold)
                }
            }
        }

        fun isNumberActive(number: String?): Boolean {
            return calls.any {
                Common.compareNumbers(it.details.handle?.schemeSpecificPart, number)
            }
        }

        fun addListener(listener: CallManagerListener) {
            listeners.add(listener)
        }

        fun removeListener(listener: CallManagerListener) {
            listeners.remove(listener)
        }

    }


    interface CallManagerListener {
        fun onStateChanged()
        fun onAudioStateChanged()
        fun onPrimaryCallChanged(call: Call)
        fun onMuteChanged(isMuted: Boolean)
    }

    sealed class PhoneState
    data object NoCall : PhoneState()
    class SingleCall(val call: Call) : PhoneState()
    class TwoCalls(val active: Call, val onHold: Call) : PhoneState()

}