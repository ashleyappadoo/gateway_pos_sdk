package com.nepting.common.client.callback

data class UIRequest(
    val actionType: ActionType,
    val message: String = "",
    val data: Map<String, Any> = emptyMap()
) {
    enum class ActionType {
        NFC_LEDS,
        MESSAGE,
        VIDEO,
        PIN_ENTRY,
        QUESTION,
        MENU,
        KEYS_ENTRY
    }
}
