package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("simchat_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_GUEST = "key_is_guest"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_BIO = "key_user_bio"
        private const val KEY_USER_PHOTO = "key_user_photo"
        private const val KEY_THEME = "key_theme" // "light", "dark", "system"
        private const val KEY_PIN_LOCKED = "key_pin_locked"
        private const val KEY_PIN_CODE = "key_pin_code"
        private const val KEY_BIOMETRIC_LOCKED = "key_biometric_locked"
        private const val KEY_CARRIER_SIM = "key_carrier_sim" // "SIM 1" or "SIM 2"
        private const val KEY_NOTIFS_ENABLED = "key_notifs_enabled"
        private const val KEY_SUPABASE_URL = "key_supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "key_supabase_anon_key"
    }

    var isGuest: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "SIM User") ?: "SIM User"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userBio: String
        get() = prefs.getString(KEY_USER_BIO, "Hey there! I am using SimChat.") ?: "Hey there! I am using SimChat."
        set(value) = prefs.edit().putString(KEY_USER_BIO, value).apply()

    var userPhoto: String
        get() = prefs.getString(KEY_USER_PHOTO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHOTO, value).apply()

    var theme: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var isPinLocked: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCKED, value).apply()

    var pinCode: String?
        get() = prefs.getString(KEY_PIN_CODE, null)
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var isBiometricLocked: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCKED, value).apply()

    var preferredCarrierSim: String
        get() = prefs.getString(KEY_CARRIER_SIM, "SIM 1") ?: "SIM 1"
        set(value) = prefs.edit().putString(KEY_CARRIER_SIM, value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, value).apply()

    var supabaseUrl: String
        get() = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value).apply()

    var supabaseAnonKey: String
        get() = prefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON_KEY, value).apply()

    var lockedChatsPasskey: String?
        get() = prefs.getString("locked_chats_passkey", null)
        set(value) = prefs.edit().putString("locked_chats_passkey", value).apply()

    var isLockedChatsBiometricEnabled: Boolean
        get() = prefs.getBoolean("locked_chats_biometric", false)
        set(value) = prefs.edit().putBoolean("locked_chats_biometric", value).apply()

    var lockedChatsBiometricType: String?
        get() = prefs.getString("locked_chats_biometric_type", "FINGERPRINT")
        set(value) = prefs.edit().putString("locked_chats_biometric_type", value).apply()

    var lockedChatsPattern: String?
        get() = prefs.getString("locked_chats_pattern", null)
        set(value) = prefs.edit().putString("locked_chats_pattern", value).apply()

    var isScreenshotProtectionEnabled: Boolean
        get() = prefs.getBoolean("screenshot_protection_enabled", false)
        set(value) = prefs.edit().putBoolean("screenshot_protection_enabled", value).apply()

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean("key_is_setup_completed", false)
        set(value) = prefs.edit().putBoolean("key_is_setup_completed", value).apply()

    var fcmToken: String?
        get() = prefs.getString("fcm_token", null)
        set(value) = prefs.edit().putString("fcm_token", value).apply()

    var chatBackgroundTheme: String
        get() = prefs.getString("chat_bg_theme", "default") ?: "default"
        set(value) = prefs.edit().putString("chat_bg_theme", value).apply()

    var localWallpaperUri: String
        get() = prefs.getString("local_wallpaper_uri", "") ?: ""
        set(value) = prefs.edit().putString("local_wallpaper_uri", value).apply()

    var chatTextColorHex: String
        get() = prefs.getString("chat_text_color_hex", "") ?: ""
        set(value) = prefs.edit().putString("chat_text_color_hex", value).apply()

    var chatFontFamily: String
        get() = prefs.getString("chat_font_family", "default") ?: "default"
        set(value) = prefs.edit().putString("chat_font_family", value).apply()

    var deviceFingerprint: String
        get() {
            var fp = prefs.getString("device_fingerprint_id", "") ?: ""
            if (fp.isEmpty()) {
                val manufacturer = android.os.Build.MANUFACTURER ?: "Android"
                val model = android.os.Build.MODEL ?: "Emulator"
                val randomSuffix = (1000..9999).random().toString()
                fp = "${manufacturer.uppercase()}-${model.uppercase()}-$randomSuffix"
                prefs.edit().putString("device_fingerprint_id", fp).apply()
            }
            return fp
        }
        set(value) {
            prefs.edit().putString("device_fingerprint_id", value).apply()
        }

    fun getAccountFingerprint(address: String): String? {
        return prefs.getString("acc_fingerprint_${address}", null)
    }

    fun setAccountFingerprint(address: String, fingerprint: String) {
        prefs.edit().putString("acc_fingerprint_${address}", fingerprint).apply()
    }

    fun getBackupPhone(address: String): String {
        val phone = prefs.getString("backup_phone_${address}", "") ?: ""
        return if (phone.isNotEmpty()) phone else {
            // Default simulated backup phone if none specified
            if (address.contains("@")) "+254712345678" else address
        }
    }

    fun setBackupPhone(address: String, phone: String) {
        prefs.edit().putString("backup_phone_${address}", phone).apply()
    }

    fun getBackupEmail(address: String): String {
        val email = prefs.getString("backup_email_${address}", "") ?: ""
        return if (email.isNotEmpty()) email else {
            // Default simulated backup email if none specified
            if (address.contains("@")) address else "ronohkiptoo44@gmail.com"
        }
    }

    fun setBackupEmail(address: String, email: String) {
        prefs.edit().putString("backup_email_${address}", email).apply()
    }

    fun registerGoogleAccount(email: String, name: String, pass: String) {
        val emails = getRegisteredEmails().toMutableSet()
        emails.add(email)
        prefs.edit()
            .putStringSet("reg_emails_list", emails)
            .putString("pass_${email}", pass)
            .putString("name_${email}", name)
            .apply()
    }

    fun getRegisteredEmails(): Set<String> {
        return prefs.getStringSet("reg_emails_list", emptySet()) ?: emptySet()
    }

    fun registerPhoneAccount(phone: String, pass: String) {
        val phones = getRegisteredPhones().toMutableSet()
        phones.add(phone)
        prefs.edit()
            .putStringSet("reg_phones_list", phones)
            .putString("pass_${phone}", pass)
            .apply()
    }

    fun getRegisteredPhones(): Set<String> {
        return prefs.getStringSet("reg_phones_list", emptySet()) ?: emptySet()
    }

    fun checkEmailPassword(email: String, pass: String): Boolean {
        val storedPass = prefs.getString("pass_${email}", null)
        return storedPass != null && storedPass == pass
    }

    fun checkPhonePassword(phone: String, pass: String): Boolean {
        val storedPass = prefs.getString("pass_${phone}", null)
        return storedPass != null && storedPass == pass
    }

    fun getPassword(address: String): String? {
        return prefs.getString("pass_${address}", null)
    }

    fun updatePassword(address: String, newPass: String) {
        prefs.edit().putString("pass_${address}", newPass).apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .putBoolean(KEY_IS_GUEST, false)
            .putBoolean("key_is_setup_completed", false)
            .apply()
    }
}
