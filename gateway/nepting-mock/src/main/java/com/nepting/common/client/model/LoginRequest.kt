package com.nepting.common.client.model

import com.nepting.common.client.LoadBalancingAlgorithm

data class LoginRequest(
    val login: String,
    val password: String,
    val extra: String = "",
    val urls: Array<String>,
    val loadBalancingAlgorithm: LoadBalancingAlgorithm,
    val merchantCode: String = "",
    val sslConfig: Any? = null
) {
    var posEditor: String = ""
    var posSolution: String = ""
    var posVersion: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LoginRequest
        return login == other.login && password == other.password && urls.contentEquals(other.urls)
    }

    override fun hashCode(): Int {
        var result = login.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + urls.contentHashCode()
        return result
    }
}
