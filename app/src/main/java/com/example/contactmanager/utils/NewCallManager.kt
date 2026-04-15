package com.example.contactmanager.utils

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import java.util.concurrent.CopyOnWriteArraySet

class NewCallManager {

    companion object{
        @SuppressLint("StaticFieldLeak")
        var inCallService: InCallService? = null
        private var call: Call? = null
        private val calls = mutableListOf<Call>()
        private val listeners = CopyOnWriteArraySet<CallManagerListener>()

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

                override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) {
                    updateState()
                }
            })
        }

        fun onCallRemoved(call: Call) {
            calls.remove(call)
            updateState()
        }

        private fun updateState() {
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
                for (listener in listeners) {
                    listener.onStateChanged()
                }
            }

            // remove all disconnected calls manually in case they are still here
            calls.removeAll { it.getStateCompat() == Call.STATE_DISCONNECTED }
        }

        fun getPhoneState(): PhoneState {
            val topLevelCalls = calls.filter { it.parent == null }
            return when (topLevelCalls.size) {
                0 -> NoCall
                1 -> SingleCall(topLevelCalls.first())
                2 -> {
                    val active = topLevelCalls.find { it.getStateCompat() == Call.STATE_ACTIVE }
                    val newCall = topLevelCalls.find { it.getStateCompat() == Call.STATE_CONNECTING || it.getStateCompat() == Call.STATE_DIALING }
                    val onHold = topLevelCalls.find { it.getStateCompat() == Call.STATE_HOLDING }
                    if (active != null && newCall != null) {
                        TwoCalls(newCall, active)
                    } else if (newCall != null && onHold != null) {
                        TwoCalls(newCall, onHold)
                    } else if (active != null && onHold != null) {
                        TwoCalls(active, onHold)
                    } else {
                        TwoCalls(topLevelCalls[0], topLevelCalls[1])
                    }
                }

                else -> {
                    if (topLevelCalls.isEmpty()) return NoCall
                    val activeOrNew = topLevelCalls.find { it.getStateCompat() != Call.STATE_HOLDING } ?: topLevelCalls[0]
                    val onHold = topLevelCalls.find { it.getStateCompat() == Call.STATE_HOLDING } ?: topLevelCalls.find { it != activeOrNew } ?: topLevelCalls[0]
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
            if (call != null) {
                val state = getState()
                if (state == Call.STATE_RINGING) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        call!!.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        call!!.reject(false, null)
                    }
                } else if (state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING) {
                    call!!.disconnect()
                }
            }
        }

        fun getState() = getPrimaryCall()?.getStateCompat()

        fun toggleHold(): Boolean {
            val isOnHold = getState() == Call.STATE_HOLDING
            if (isOnHold) {
                call?.unhold()
            } else {
                call?.hold()
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
            if (number == null) return false
            val sanitizedNew = number.replace(Regex("[^0-9+]"), "")
            return calls.any {
                val callNumber = it.details.handle?.schemeSpecificPart?.replace(Regex("[^0-9+]"), "")
                callNumber == sanitizedNew
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