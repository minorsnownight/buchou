package com.buchou.app.ui

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CurrencyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("currency", Context.MODE_PRIVATE)
    private val mutableCurrency = MutableStateFlow(read())

    val currencyCode: StateFlow<String> = mutableCurrency

    fun setCurrency(code: String) {
        preferences.edit { putString(KEY_CURRENCY, code) }
        mutableCurrency.value = code
    }

    private fun read(): String =
        preferences.getString(KEY_CURRENCY, null) ?: "CNY"

    private companion object {
        const val KEY_CURRENCY = "code"
    }
}
