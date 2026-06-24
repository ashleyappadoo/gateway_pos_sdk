package com.smileandpay.gateway.nepting

import com.nepting.common.client.callback.UICallback
import com.nepting.common.client.callback.UIRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class UIEvent {
    data class NfcLedsChanged(val active: Boolean) : UIEvent()
    data class MessageReceived(val message: String) : UIEvent()
    object PinEntryRequested : UIEvent()
    data class QuestionAsked(val message: String) : UIEvent()
}

class UICallbackHandler : UICallback {

    private val _events = MutableSharedFlow<UIEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UIEvent> = _events.asSharedFlow()

    override fun postUIRequest(request: UIRequest) {
        val event = when (request.actionType) {
            UIRequest.ActionType.NFC_LEDS -> UIEvent.NfcLedsChanged(active = true)
            UIRequest.ActionType.MESSAGE -> UIEvent.MessageReceived(request.message)
            UIRequest.ActionType.PIN_ENTRY -> UIEvent.PinEntryRequested
            UIRequest.ActionType.QUESTION -> UIEvent.QuestionAsked(request.message)
            else -> UIEvent.MessageReceived(request.message)
        }
        _events.tryEmit(event)
    }
}
