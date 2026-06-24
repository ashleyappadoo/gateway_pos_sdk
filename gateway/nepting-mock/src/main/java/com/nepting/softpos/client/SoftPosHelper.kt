package com.nepting.softpos.client

import android.content.Context
import com.nepting.common.client.callback.UICallback

object SoftPosHelper {
    fun createClient(
        context: Context,
        uiCallback: UICallback,
        debugEnabled: Boolean = false
    ): SoftPosClient = SoftPosClient(
        uiCallback = uiCallback,
        logger = null,
        debugEnabled = debugEnabled,
        context = context
    )
}
