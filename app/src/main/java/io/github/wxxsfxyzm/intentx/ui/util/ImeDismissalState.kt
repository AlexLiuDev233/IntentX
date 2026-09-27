// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.util

/** Tracks keyboard dismissal for the current focus session, never a subsequent field or tap. */
internal class ImeDismissalState<T : Any> {
    internal data class Keyboard(val visible: Boolean, val bottom: Int, val sourceBottom: Int, val targetBottom: Int)

    var focusedField: T? = null
        private set
    private var generation = 0L
    private var armedGeneration: Long? = null
    private var dismissingGeneration: Long? = null
    private var keyboardClosing = false

    fun focusChanged(field: T, focused: Boolean, keyboard: Keyboard?) {
        if (!focused && focusedField !== field) return
        val nextField = if (focused) field else null
        if (focusedField === nextField) return
        focusedField = nextField
        resetObservation()
        if (keyboard != null) keyboardChanged(keyboard)
    }

    fun pressed(field: T, keyboard: Keyboard?) {
        resetObservation()
        if (focusedField === field && keyboard != null) keyboardChanged(keyboard)
    }

    fun resetObservation() {
        generation++
        armedGeneration = null
        dismissingGeneration = null
    }

    /** Returns true once, after the same field's keyboard has finished closing. */
    fun keyboardChanged(keyboard: Keyboard): Boolean {
        // Predictive-back dismissal can retain the animation's original source height after
        // the IME is hidden. Its starting point is not a condition for dismissal completion.
        val fullyHidden = !keyboard.visible && keyboard.bottom == 0 && keyboard.targetBottom == 0
        if (keyboard.targetBottom > 0 || fullyHidden) {
            keyboardClosing = false
        } else if (keyboard.bottom > 0 || keyboard.sourceBottom > 0) {
            keyboardClosing = true
        }
        if (focusedField == null) return false
        // Visibility can reach composition after animated insets. A closing keyboard at zero
        // height must not be mistaken for a newly shown floating keyboard and rearm a fresh tap.
        val floatingKeyboard = keyboard.visible && !keyboardClosing && keyboard.bottom == 0 && keyboard.sourceBottom == 0
        if (keyboard.targetBottom > 0 || floatingKeyboard) {
            dismissingGeneration = null
            armedGeneration = generation.takeIf { keyboard.visible }
            return false
        }

        if (dismissingGeneration == null) dismissingGeneration = armedGeneration
        armedGeneration = null
        if (fullyHidden && dismissingGeneration == generation) {
            resetObservation()
            return true
        }
        return false
    }
}
