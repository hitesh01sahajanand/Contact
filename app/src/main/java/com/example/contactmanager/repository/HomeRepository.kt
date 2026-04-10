package com.example.contactmanager.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HomeRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /*fun getRecentList(): List<CallLogModel> {

        val callList = mutableListOf<CallLogModel>()

        try {

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {

                    val number = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    )

                    val name = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    )

                    val type = it.getInt(
                        it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    )

                    val date = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    )

                    val duration = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    )

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
                    )

                    val isMissed = type == CallLog.Calls.MISSED_TYPE
                    val isIncoming = type == CallLog.Calls.INCOMING_TYPE
                    val isOutgoing = type == CallLog.Calls.OUTGOING_TYPE
                    val isRejected = type == CallLog.Calls.REJECTED_TYPE

                    val nameTemp = if (name.isNullOrEmpty()) number else name
                    val imageAvatar = Common.generateAvatar(nameTemp)

                    // 👇 check last item
                    if (callList.isNotEmpty() && callList.last().number == number) {

                        // 👉 same number consecutive → increase count
                        callList.last().callCount += 1

                    } else {

                        // 👉 new group
                        callList.add(
                            CallLogModel(
                                name = name,
                                number = number,
                                type = type,
                                date = date,
                                duration = duration,
                                isMissed = isMissed,
                                isIncoming = isIncoming,
                                isOutgoing = isOutgoing,
                                photoUri = photoUri,
                                callCount = 1,
                                avatar = imageAvatar,
                                isRejected = isRejected
                            )
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("TAG", "getRecentList: ${e.message}")
        }
        return callList
    }*/

    fun getContactName(phoneNumber: String): String {

        var name = phoneNumber

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    name = it.getString(0)
                }
            }

        } catch (e: Exception) {
            Log.e("TAG", "getContactName: ${e.message}")
        }

        return name
    }



}