// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeDismissalStateTest {
    private val hidden = ImeDismissalState.Keyboard(false, 0, 0, 0)
    private val shown = ImeDismissalState.Keyboard(true, 300, 300, 300)
    private val closing = ImeDismissalState.Keyboard(true, 150, 300, 0)
    private val field = Any()

    @Test
    fun dismissalWaitsForAnimationEndAndOnlyClearsOnce() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)

        assertFalse(state.keyboardChanged(closing))
        assertFalse(state.keyboardChanged(closing.copy(visible = false)))
        assertTrue(state.keyboardChanged(closing.copy(visible = false, bottom = 0)))
        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun initialHiddenKeyboardAndHardwareKeyboardDoNotClearNewFocus() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, hidden)

        assertFalse(state.keyboardChanged(hidden))
        state.focusChanged(field, false, hidden)
        state.focusChanged(Any(), true, hidden)
        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun switchingFieldsDuringDismissalDoesNotClearTheNewField() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.focusChanged(field, false, closing)
        state.focusChanged(Any(), true, closing)

        assertFalse(state.keyboardChanged(hidden))
        state.keyboardChanged(shown)
        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun tappingTheSameFieldDuringDismissalCancelsPendingClear() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.pressed(field, closing)

        assertFalse(state.keyboardChanged(hidden))
        state.keyboardChanged(shown)
        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun tappingAnotherFieldCancelsClearBeforeThatFieldReceivesFocus() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.pressed(Any(), closing)

        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun newFocusDoesNotInheritTheOldFieldsVisibleKeyboardDuringClosing() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, closing)

        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun switchingFieldsWhileKeyboardStaysOpenArmsTheNewField() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        val nextField = Any()
        state.focusChanged(nextField, true, shown)
        // A late unfocus/disposal notification for the old field must not disarm the new one.
        state.focusChanged(field, false, shown)

        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun leavingAndReturningToTheSameFieldInvalidatesTheOldSession() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.focusChanged(field, false, closing)
        state.focusChanged(field, true, closing)

        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun keyboardHeightChangeDoesNotClearFocus() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)

        assertFalse(state.keyboardChanged(shown.copy(bottom = 270, targetBottom = 240)))
        assertFalse(state.keyboardChanged(shown.copy(bottom = 240, sourceBottom = 240, targetBottom = 240)))
        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun reopeningKeyboardCancelsOldDismissalEvenAtZeroCurrentHeight() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)

        assertFalse(state.keyboardChanged(hidden.copy(targetBottom = 300)))
        assertFalse(state.keyboardChanged(hidden))
        state.keyboardChanged(shown)
        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun pausingPageOrCoveringWindowDiscardsPendingDismissal() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.resetObservation()

        assertFalse(state.keyboardChanged(hidden))
        state.keyboardChanged(shown)
        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun losingFocusOrRemovingFieldDoesNotClearAnotherTarget() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.focusChanged(field, false, null)

        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun searchEditorAndModalKeepIndependentSessions() {
        val search = ImeDismissalState<Any>()
        val editor = ImeDismissalState<Any>()
        val modal = ImeDismissalState<Any>()
        search.focusChanged(field, true, shown)
        search.resetObservation()
        editor.focusChanged(Any(), true, shown)
        editor.resetObservation()
        modal.focusChanged(Any(), true, shown)

        assertFalse(search.keyboardChanged(hidden))
        assertFalse(editor.keyboardChanged(hidden))
        assertTrue(modal.keyboardChanged(hidden))
    }

    @Test
    fun floatingKeyboardUsesVisibilityWhenItsInsetsAreZero() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, hidden.copy(visible = true))

        assertTrue(state.keyboardChanged(hidden))
    }

    @Test
    fun delayedVisibilityAtAnimationEndDoesNotRearmAFreshTap() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.pressed(field, closing)

        assertFalse(state.keyboardChanged(hidden.copy(visible = true)))
        assertFalse(state.keyboardChanged(hidden))
    }

    @Test
    fun recordedPredictiveBackDismissalClearsFocusWithRetainedSourceHeight() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, hidden)

        // Captured on device: sourceBottom stays at the pre-dismissal height after onHidden.
        assertFalse(state.keyboardChanged(ImeDismissalState.Keyboard(true, 51, 0, 1095)))
        assertFalse(state.keyboardChanged(ImeDismissalState.Keyboard(true, 1095, 1095, 1095)))
        assertFalse(state.keyboardChanged(ImeDismissalState.Keyboard(true, 1095, 1095, 0)))
        assertFalse(state.keyboardChanged(ImeDismissalState.Keyboard(true, 0, 1095, 0)))
        assertTrue(state.keyboardChanged(ImeDismissalState.Keyboard(false, 0, 1095, 0)))
        assertFalse(state.keyboardChanged(ImeDismissalState.Keyboard(false, 0, 1095, 0)))
    }

    @Test
    fun retainedSourceHeightDoesNotClearAFieldSelectedDuringDismissal() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.focusChanged(Any(), true, closing)

        assertFalse(state.keyboardChanged(hidden.copy(sourceBottom = 300)))
    }

    @Test
    fun retainedSourceHeightDoesNotClearAFieldTappedAgainDuringDismissal() {
        val state = ImeDismissalState<Any>()
        state.focusChanged(field, true, shown)
        state.keyboardChanged(closing)
        state.pressed(field, closing)

        assertFalse(state.keyboardChanged(hidden.copy(sourceBottom = 300)))
    }
}
