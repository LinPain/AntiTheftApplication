package com.example.zerotrustauth.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

/**
 * Enhanced Security Preferences with Direct Boot Support
 */
class SecurityPrefs(context: Context) {
    
    // Use device protected storage to ensure security flags are available before user unlock
    private val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else {
        context
    }

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
        val IS_LOST_MODE_ACTIVE = booleanPreferencesKey("is_lost_mode_active")
        val LOST_MODE_MESSAGE = stringPreferencesKey("lost_mode_message")
        val LOST_MODE_PHONE = stringPreferencesKey("lost_mode_phone")
        val IS_OUTSIDE_SAFE_ZONE = booleanPreferencesKey("is_outside_safe_zone")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LOCAL_PIN = stringPreferencesKey("local_pin")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val OWNER_PHONE = stringPreferencesKey("owner_phone")
        val OWNER_EMAIL = stringPreferencesKey("owner_email")
    }

    val lastSimId: Flow<String?> = safeContext.dataStore.data.map { it[LAST_SIM_ID] }.distinctUntilChanged()
    val failedUnlockCount: Flow<Int> = safeContext.dataStore.data.map { it[FAILED_UNLOCK_COUNT] ?: 0 }.distinctUntilChanged()
    val lastInternetTimestamp: Flow<Long> = safeContext.dataStore.data.map { it[LAST_INTERNET_TIMESTAMP] ?: 0L }.distinctUntilChanged()
    val isRemoteLockdownActive: Flow<Boolean> = safeContext.dataStore.data.map { it[REMOTE_LOCKDOWN_ACTIVE] ?: false }.distinctUntilChanged()
    val authToken: Flow<String?> = safeContext.dataStore.data.map { it[AUTH_TOKEN] }.distinctUntilChanged()
    val username: Flow<String?> = safeContext.dataStore.data.map { it[USERNAME] }.distinctUntilChanged()
    val isDeviceTrusted: Flow<Boolean> = safeContext.dataStore.data.map { it[IS_DEVICE_TRUSTED] ?: false }.distinctUntilChanged()
    val isLiveTrackingEnabled: Flow<Boolean> = safeContext.dataStore.data.map { it[LIVE_TRACKING_ENABLED] ?: false }.distinctUntilChanged()
    val safeZoneLat: Flow<Double?> = safeContext.dataStore.data.map { it[SAFE_ZONE_LAT] }.distinctUntilChanged()
    val safeZoneLon: Flow<Double?> = safeContext.dataStore.data.map { it[SAFE_ZONE_LON] }.distinctUntilChanged()
    val safeZoneRadius: Flow<Float> = safeContext.dataStore.data.map { it[SAFE_ZONE_RADIUS] ?: 100f }.distinctUntilChanged()
    val isLostModeActive: Flow<Boolean> = safeContext.dataStore.data.map { it[IS_LOST_MODE_ACTIVE] ?: false }.distinctUntilChanged()
    val lostModeMessage: Flow<String> = safeContext.dataStore.data.map { it[LOST_MODE_MESSAGE] ?: "THIS DEVICE IS LOST" }.distinctUntilChanged()
    val lostModePhone: Flow<String> = safeContext.dataStore.data.map { it[LOST_MODE_PHONE] ?: "" }.distinctUntilChanged()
    val isOutsideSafeZone: Flow<Boolean> = safeContext.dataStore.data.map { it[IS_OUTSIDE_SAFE_ZONE] ?: false }.distinctUntilChanged()
    val isRememberMeEnabled: Flow<Boolean> = safeContext.dataStore.data.map { it[REMEMBER_ME] ?: false }.distinctUntilChanged()
    val localPin: Flow<String?> = safeContext.dataStore.data.map { it[LOCAL_PIN] }.distinctUntilChanged()
    val ownerName: Flow<String?> = safeContext.dataStore.data.map { it[OWNER_NAME] }.distinctUntilChanged()
    val ownerPhone: Flow<String?> = safeContext.dataStore.data.map { it[OWNER_PHONE] }.distinctUntilChanged()
    val ownerEmail: Flow<String?> = safeContext.dataStore.data.map { it[OWNER_EMAIL] }.distinctUntilChanged()

    suspend fun setRememberMe(enabled: Boolean) {
        safeContext.dataStore.edit { it[REMEMBER_ME] = enabled }
    }

    suspend fun setLocalPin(pin: String?) {
        safeContext.dataStore.edit {
            if (pin == null) it.remove(LOCAL_PIN)
            else it[LOCAL_PIN] = pin
        }
    }

    suspend fun setOutsideSafeZone(isOutside: Boolean) {
        safeContext.dataStore.edit { it[IS_OUTSIDE_SAFE_ZONE] = isOutside }
    }

    suspend fun setLostMode(active: Boolean, message: String? = null, phone: String? = null) {
        safeContext.dataStore.edit {
            it[IS_LOST_MODE_ACTIVE] = active
            if (message != null) it[LOST_MODE_MESSAGE] = message
            if (phone != null) it[LOST_MODE_PHONE] = phone
        }
    }

    suspend fun setSafeZone(lat: Double, lon: Double, radius: Float = 100f) {
        safeContext.dataStore.edit {
            it[SAFE_ZONE_LAT] = lat
            it[SAFE_ZONE_LON] = lon
            it[SAFE_ZONE_RADIUS] = radius
        }
    }

    suspend fun setLiveTracking(enabled: Boolean) {
        safeContext.dataStore.edit { it[LIVE_TRACKING_ENABLED] = enabled }
    }

    suspend fun setDeviceTrusted(trusted: Boolean) {
        safeContext.dataStore.edit { it[IS_DEVICE_TRUSTED] = trusted }
    }

    suspend fun saveAuthData(token: String?, username: String?, name: String? = null, phone: String? = null, email: String? = null) {
        safeContext.dataStore.edit {
            if (token != null) it[AUTH_TOKEN] = token
            if (username != null) it[USERNAME] = username
            if (name != null) it[OWNER_NAME] = name
            if (phone != null) it[OWNER_PHONE] = phone
            if (email != null) it[OWNER_EMAIL] = email
        }
    }

    suspend fun clearAuthData() {
        safeContext.dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(USERNAME)
            it.remove(OWNER_NAME)
            it.remove(OWNER_PHONE)
            it.remove(OWNER_EMAIL)
        }
    }

    suspend fun setRemoteLockdown(active: Boolean) {
        safeContext.dataStore.edit { it[REMOTE_LOCKDOWN_ACTIVE] = active }
    }

    suspend fun clearAllLocks() {
        safeContext.dataStore.edit {
            it[REMOTE_LOCKDOWN_ACTIVE] = false
            it[IS_LOST_MODE_ACTIVE] = false
            it[FAILED_UNLOCK_COUNT] = 0
        }
    }

    suspend fun saveSimId(simId: String) {
        safeContext.dataStore.edit { it[LAST_SIM_ID] = simId }
    }

    suspend fun incrementFailedUnlock() {
        safeContext.dataStore.edit {
            val current = it[FAILED_UNLOCK_COUNT] ?: 0
            it[FAILED_UNLOCK_COUNT] = current + 1
        }
    }

    suspend fun resetFailedUnlock() {
        safeContext.dataStore.edit { it[FAILED_UNLOCK_COUNT] = 0 }
    }

    suspend fun updateInternetTimestamp(timestamp: Long) {
        safeContext.dataStore.edit { it[LAST_INTERNET_TIMESTAMP] = timestamp }
    }
}
