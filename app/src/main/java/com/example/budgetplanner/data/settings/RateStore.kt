package com.example.budgetplanner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.rateDataStore by preferencesDataStore("rate_store")

class RateStore(private val context: Context) {
    private val KEY_RATE = doublePreferencesKey("eur_ron")
    private val KEY_TS   = longPreferencesKey("eur_ron_ts")

    val flow = context.rateDataStore.data.map { p ->
        (p[KEY_RATE] ?: 0.0) to (p[KEY_TS] ?: 0L)
    }

    suspend fun save(rate: Double, ts: Long) {
        context.rateDataStore.edit { p ->
            p[KEY_RATE] = rate
            p[KEY_TS] = ts
        }
    }
}
