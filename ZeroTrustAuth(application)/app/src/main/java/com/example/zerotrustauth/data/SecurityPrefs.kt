package com.example.zerotrustauth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

class SecurityPrefs(private val context: Context) {

    companion object {
        val LAST_SIM_ID = stringPreferencesKey("last_sim_id")
        val FAILED_UNLOCK_COUNT = intPreferencesKey("failed_unlock_count")
        val LAST_INTERNET_TIMESTAMP = longPreferencesKey("last_internet_timestamp")
        val REMOTE_LOCKDOWN_ACTIVE = booleanPreferencesKey("remote_lockdown_active")
    }

    val lastSimId: Flow<String?> = context.dataStore.data.map { it[LAST_SIM_ID] }
    val failedUnlockCount: Flow<Int> = context.dataStore.data.map { it[FAILED_UNLOCK_COUNT] ?: 0 }
    val lastInternetTimestamp: Flow<Long> = context.dataStore.data.map { it[LAST_INTERNET_TIMESTAMP] ?: 0L }
    val isRemoteLockdownActive: Flow<Boolean> = context.dataStore.data.map { it[REMOTE_LOCKDOWN_ACTIVE] ?: false }

    suspend fun setRemoteLockdown(active: Boolean) {
        context.dataStore.edit { it[REMOTE_LOCKDOWN_ACTIVE] = active }
    }

    suspend fun saveSimId(simId: String) {
        context.dataStore.edit { it[LAST_SIM_ID] = simId }
    }

    suspend fun incrementFailedUnlock() {
        context.dataStore.edit {
            val current = it[FAILED_UNLOCK_COUNT] ?: 0
            it[FAILED_UNLOCK_COUNT] = current + 1
        }
    }

    suspend fun resetFailedUnlock() {
        context.dataStore.edit { it[FAILED_UNLOCK_COUNT] = 0 }
    }

    suspend fun updateInternetTimestamp(timestamp: Long) {
        context.dataStore.edit { it[LAST_INTERNET_TIMESTAMP] = timestamp }
    }
}
