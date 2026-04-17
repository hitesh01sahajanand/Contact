package com.example.contactmanager.utils

object Constance {
    const val PREF_NAME = "my_app_prefs"
    const val IS_LOG_IN = "isLogIn"

    const val CALL_DISCONNECTED = "CALL_DISCONNECTED"
    const val ADD_TO_FAVORITE = "Add to favorite"
    const val REMOVE_TO_FAVORITE = "Remove to favorite"

    const val IS_DIALER = "isDialer"
    const val ACTION_CALL = "action_call"
    const val ACTION_SEND_MESSAGE = "action_send_message"
    const val ACTION_VIDEO_CALL = "action_video_call"
    const val ACTION_INFO = "action_info"
    const val ACTION_ADD_TO_CONTACT = "action_add_to_contact"
    const val ACTION_ADD_TAG = "action_add_tag"
    const val ACTION_BLOCK_CONTACT = "action_block_contact"
    const val CONTACT_ID = "contactId"
    const val IS_CONTACT_SAVED = "isContactSaved"
    const val CONTACT_MODEL = "contactModel"

    const val DATA_FETCH = "dataFetch"

    const val WHATSAPP: String = "com.whatsapp"
    const val WHATSAPP_BUSSINESS: String = "com.whatsapp.w4b"
    const val DUO: String = "com.google.android.apps.tachyon"
    val videoCallList: Array<String> = arrayOf(WHATSAPP, WHATSAPP_BUSSINESS, DUO)
}