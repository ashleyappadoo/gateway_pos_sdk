package com.nepting.softpos.client

import com.nepting.common.client.model.TransactionResponse

interface UIRequestCallback {
    fun onSuccess(response: TransactionResponse? = null)
    fun onFailure(reason: String)
    fun onCancelled()
}
