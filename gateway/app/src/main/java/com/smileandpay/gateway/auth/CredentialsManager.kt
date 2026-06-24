package com.smileandpay.gateway.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nepting.common.client.LoadBalancingAlgorithm
import com.nepting.common.client.model.LoginRequest

class CredentialsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gateway_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasCredentials(): Boolean =
        prefs.getString(KEY_LOGIN, null) != null &&
                prefs.getString(KEY_PASSWORD, null) != null

    fun saveCredentials(login: String, password: String, clientId: String) {
        prefs.edit()
            .putString(KEY_LOGIN, login)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_CLIENT_ID, clientId)
            .apply()
    }

    fun getLoginRequest(): LoginRequest {
        val login = prefs.getString(KEY_LOGIN, "") ?: ""
        val password = prefs.getString(KEY_PASSWORD, "") ?: ""
        return LoginRequest(
            login = login,
            password = password,
            urls = arrayOf(QualifConfig.NEPTING_URL),
            loadBalancingAlgorithm = LoadBalancingAlgorithm.FIRST_ALIVE,
            merchantCode = QualifConfig.SMILE_MERCHANT_CODE
        ).apply {
            posEditor = QualifConfig.POS_EDITOR
            posSolution = QualifConfig.POS_SOLUTION
            posVersion = "1.0.0"
        }
    }

    fun getClientId(): String = prefs.getString(KEY_CLIENT_ID, "") ?: ""

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
        private const val KEY_CLIENT_ID = "client_id"
    }
}

object QualifConfig {
    const val NEPTING_URL = "qualif.nepting.com:443/nepweb/ws?wsdl"
    const val SMILE_MERCHANT_CODE = "72086618504806197"
    const val SSL_PINNING_ENABLE = false
    const val SSL_ENABLE = false
    const val POS_EDITOR = "SMILEANDPAY"
    const val POS_SOLUTION = "SMILEANDPAY"
}
