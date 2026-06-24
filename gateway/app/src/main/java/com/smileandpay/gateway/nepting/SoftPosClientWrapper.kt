package com.smileandpay.gateway.nepting

import android.content.Context
import com.nepting.softpos.client.SoftPosClient
import com.nepting.softpos.client.SoftPosHelper

object SoftPosClientWrapper {
    fun create(context: Context, handler: UICallbackHandler): SoftPosClient =
        SoftPosHelper.createClient(context, handler, debugEnabled = true)
}
