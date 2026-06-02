package com.phonecall.dialcontacts.calldialer.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import java.util.Locale

object QuickResponseDefaults {

    const val DEFAULT_MESSAGE_COUNT = 4

    private val stringResIds = listOf(
        R.string.can_t_talk_right_now,
        R.string.i_ll_call_you_later,
        R.string.i_m_on_my_way,
        R.string.can_t_talk_now_call_me_later
    )

    fun getLocalizedMessages(context: Context): List<QuickResponseModel> {
        val localizedContext = getLocalizedContext(context)
        return stringResIds.mapIndexed { index, resId ->
            QuickResponseModel(
                id = -(index + 1),
                message = localizedContext.getString(resId)
            )
        }
    }

    fun buildDisplayList(
        context: Context,
        storedMessages: List<QuickResponseModel>
    ): List<QuickResponseModel> {
        val customMessages = if (storedMessages.size > DEFAULT_MESSAGE_COUNT) {
            storedMessages.drop(DEFAULT_MESSAGE_COUNT)
        } else {
            emptyList()
        }
        return getLocalizedMessages(context) + customMessages
    }

    private fun getLocalizedContext(context: Context): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return context

        val locale = locales[0] ?: return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(locale.toLanguageTag()))
        return context.createConfigurationContext(config)
    }
}
