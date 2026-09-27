// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatus
import io.github.wxxsfxyzm.intentx.domain.authorization.RootMode
import io.github.wxxsfxyzm.intentx.domain.authorization.ShizukuMode
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import io.github.wxxsfxyzm.intentx.ui.icons.AppIcons
import io.github.wxxsfxyzm.intentx.ui.page.main.widget.setting.RadioButtonWidget
import io.github.wxxsfxyzm.intentx.ui.page.main.widget.setting.SegmentedColumn

@Composable
internal fun AuthorizationContent(
    state: AuthorizationViewState,
    onAction: (AuthorizationViewAction) -> Unit,
) {
    SegmentedColumn(
        title = stringResource(R.string.work_mode),
    ) {
        listOf(Authorizer.Auto, Authorizer.Root, Authorizer.Shizuku, Authorizer.None).forEach { authorizer ->
            item {
                RadioButtonWidget(
                    title = when (authorizer) {
                        Authorizer.Auto -> stringResource(R.string.auth_mode_auto)
                        Authorizer.None -> stringResource(R.string.normal)
                        Authorizer.Root -> stringResource(R.string.auth_mode_root)
                        Authorizer.Shizuku -> stringResource(R.string.auth_mode_shizuku)
                    },
                    description = when (authorizer) {
                        Authorizer.Auto -> stringResource(R.string.auth_auto_note)

                        Authorizer.None -> stringResource(R.string.normal_note)

                        Authorizer.Root -> when (state.rootStatus) {
                            AuthorizationStatus.Checking -> stringResource(R.string.checking)

                            AuthorizationStatus.Ready -> {
                                val modeName = when (state.rootMode) {
                                    RootMode.Magisk -> R.string.auth_mode_magisk
                                    RootMode.KernelSU -> R.string.auth_mode_kernelsu
                                    RootMode.APatch -> R.string.auth_mode_apatch
                                    else -> null
                                }
                                if (modeName == null) {
                                    stringResource(R.string.ready)
                                } else {
                                    stringResource(R.string.auth_root_mode, stringResource(modeName))
                                }
                            }

                            else -> stringResource(R.string.failed)
                        }

                        Authorizer.Shizuku -> when (state.shizukuState.status) {
                            AuthorizationStatus.Checking -> stringResource(R.string.checking)

                            AuthorizationStatus.Ready -> when (state.shizukuState.mode) {
                                ShizukuMode.Root -> stringResource(
                                    R.string.auth_shizuku_mode,
                                    stringResource(R.string.auth_mode_root),
                                )

                                ShizukuMode.Shell -> stringResource(
                                    R.string.auth_shizuku_mode,
                                    stringResource(R.string.auth_mode_shell),
                                )

                                else -> stringResource(R.string.granted)
                            }

                            AuthorizationStatus.PermissionRequired -> stringResource(R.string.auth_permission_required)

                            AuthorizationStatus.Unavailable -> stringResource(R.string.auth_not_running)
                        }
                    },
                    icon = when (authorizer) {
                        Authorizer.Auto -> AppIcons.AutoAuthorizer
                        Authorizer.None -> AppIcons.None
                        Authorizer.Root -> AppIcons.Root
                        Authorizer.Shizuku -> ImageVector.vectorResource(R.drawable.ic_shizuku)
                    },
                    selected = state.selected == authorizer,
                    onClick = { onAction(AuthorizationViewAction.Select(authorizer)) },
                )
            }
        }
    }
}
