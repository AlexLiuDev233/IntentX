// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.editor

import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation

sealed interface EditorViewAction {
    data class SetOperation(val operation: IntentOperation) : EditorViewAction
    data class SetProfileName(val value: String) : EditorViewAction
    data class SetProfileDescription(val value: String) : EditorViewAction
    data class SetField(val field: EditorField, val value: String) : EditorViewAction
    data class SetChoice(val choice: EditorChoice, val value: Int) : EditorViewAction
    data class SetFlags(val value: String) : EditorViewAction
    data class ToggleFlag(val flag: Int, val checked: Boolean) : EditorViewAction
    data object AddExtra : EditorViewAction
    data class UpdateExtra(val row: DraftRowState) : EditorViewAction
    data class RemoveExtra(val id: Int) : EditorViewAction
    data object AddCategory : EditorViewAction
    data class UpdateCategory(val row: DraftRowState) : EditorViewAction
    data class RemoveCategory(val id: Int) : EditorViewAction
    data object AddClip : EditorViewAction
    data class UpdateClip(val row: DraftRowState) : EditorViewAction
    data class RemoveClip(val id: Int) : EditorViewAction
    data class Save(val name: String, val description: String) : EditorViewAction
    data object Launch : EditorViewAction
    data object PinShortcut : EditorViewAction
    data object ImportUri : EditorViewAction
    data object ExportUri : EditorViewAction
}
