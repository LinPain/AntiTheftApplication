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
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USERNAME = stringPreferencesKey("username")
        val IS_DEVICE_TRUSTED = booleanPreferencesKey("is_device_trusted")
        val LIVE_TRACKING_ENABLED = booleanPreferencesKey("live_tracking_enabled")
        val SAFE_ZONE_LAT = doublePreferencesKey("safe_zone_lat")
        val SAFE_ZONE_LON = doublePreferencesKey("safe_zone_lon")
        val SAFE_ZONE_RADIUS = floatPreferencesKey("safe_zone_radius")
    }

    val lastSimId: Flow<String?> = context.dataStore.data.map { it[LAST_SIM_ID] }
    val failedUnlockCount: Flow<Int> = context.dataStore.data.map { it[FAILED_UNLOCK_COUNT] ?: 0 }
    val lastInternetTimestamp: Flow<Long> = context.dataStore.data.map { it[LAST_INTERNET_TIMESTAMP] ?: 0L }
    val isRemoteLockdownActive: Flow<Boolean> = context.dataStore.data.map { it[REMOTE_LOCKDOWN_ACTIVE] ?: false }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val isDeviceTrusted: Flow<Boolean> = context.dataStore.data.map { it[IS_DEVICE_TRUSTED] ?: false }
    val isLiveTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { it[LIVE_TRACKING_ENABLED] ?: false }
    val safeZoneLat: Flow<Double?> = context.dataStore.data.map { it[SAFE_ZONE_LAT] }
    val safeZoneLon: Flow<Double?> = context.dataStore.data.map { it[SAFE_ZONE_LON] }
    val safeZoneRadius: Flow<Float> = context.dataStore.data.map { it[SAFE_ZONE_RADIUS] ?: 100f } // Default 100m

    suspend fun setSafeZone(lat: Double, lon: Double, radius: Float = 100f) {
        context.dataStore.edit {
            it[SAFE_ZONE_LAT] = lat
            it[SAFE_ZONE_LON] = lon
            it[SAFE_ZONE_RADIUS] = radius
        }
    }

    suspend fun setLiveTracking(enabled: Boolean) {
        context.dataStore.edit { it[LIVE_TRACKING_ENABLED] = enabled }
    }

    suspend fun setDeviceTrusted(trusted: Boolean) {
        context.dataStore.edit { it[IS_DEVICE_TRUSTED] = trusted }
    }

    suspend fun saveAuthData(token: String?, username: String?) {
        context.dataStore.edit {
            if (token != null) it[AUTH_TOKEN] = token
            if (username != null) it[USERNAME] = username
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(USERNAME)
        }
    }

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
