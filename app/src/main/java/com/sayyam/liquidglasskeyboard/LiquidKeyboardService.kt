package com.sayyam.liquidglasskeyboard

import android.inputmethodservice.InputMethodService
import android.view.View

class LiquidKeyboardService : InputMethodService() {
    override fun onCreateInputView(): View = LiquidKeyboardView(this) { action ->
        val ic = currentInputConnection ?: return@LiquidKeyboardView
        when (action) {
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "↵" -> ic.commitText("\n", 1)
            "SPACE" -> ic.commitText(" ", 1)
            "123" -> { /* number layout can be added next */ }
            else -> ic.commitText(action, 1)
        }
    }
}
