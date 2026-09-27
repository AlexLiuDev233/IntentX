// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.intent

import android.content.Intent

data class NamedIntentFlag(val name: String, val mask: Int)

/** Named Android Intent bits offered by the editor; unknown bits remain editable numerically. */
object IntentFlagCatalog {
    val defaultActivityFlags = Intent.FLAG_ACTIVITY_NEW_TASK
    val defaultActivityFlagsText = "0x" + defaultActivityFlags.toUInt().toString(16).padStart(8, '0')

    val entries: List<NamedIntentFlag> = listOf(
        NamedIntentFlag("ACTIVITY_NEW_TASK", Intent.FLAG_ACTIVITY_NEW_TASK),
        NamedIntentFlag("ACTIVITY_CLEAR_TOP", Intent.FLAG_ACTIVITY_CLEAR_TOP),
        NamedIntentFlag("ACTIVITY_SINGLE_TOP", Intent.FLAG_ACTIVITY_SINGLE_TOP),
        NamedIntentFlag("ACTIVITY_CLEAR_TASK", Intent.FLAG_ACTIVITY_CLEAR_TASK),
        NamedIntentFlag("ACTIVITY_NEW_DOCUMENT", Intent.FLAG_ACTIVITY_NEW_DOCUMENT),
        NamedIntentFlag("ACTIVITY_MULTIPLE_TASK", Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
        NamedIntentFlag("ACTIVITY_EXCLUDE_FROM_RECENTS", Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
        NamedIntentFlag("ACTIVITY_NO_HISTORY", Intent.FLAG_ACTIVITY_NO_HISTORY),
        NamedIntentFlag("ACTIVITY_REORDER_TO_FRONT", Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        NamedIntentFlag("ACTIVITY_NO_ANIMATION", Intent.FLAG_ACTIVITY_NO_ANIMATION),
        NamedIntentFlag("ACTIVITY_FORWARD_RESULT", Intent.FLAG_ACTIVITY_FORWARD_RESULT),
        NamedIntentFlag("ACTIVITY_BROUGHT_TO_FRONT", Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT),
        NamedIntentFlag("ACTIVITY_LAUNCHED_FROM_HISTORY", Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY),
        NamedIntentFlag("ACTIVITY_RESET_TASK_IF_NEEDED", Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        NamedIntentFlag("ACTIVITY_TASK_ON_HOME", Intent.FLAG_ACTIVITY_TASK_ON_HOME),
        NamedIntentFlag("ACTIVITY_RETAIN_IN_RECENTS", Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS),
        NamedIntentFlag("ACTIVITY_NO_USER_ACTION", Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        NamedIntentFlag("ACTIVITY_PREVIOUS_IS_TOP", Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP),
        NamedIntentFlag("ACTIVITY_LAUNCH_ADJACENT", Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT),
        NamedIntentFlag("ACTIVITY_MATCH_EXTERNAL", Intent.FLAG_ACTIVITY_MATCH_EXTERNAL),
        NamedIntentFlag("ACTIVITY_REQUIRE_DEFAULT", Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT),
        NamedIntentFlag("ACTIVITY_REQUIRE_NON_BROWSER", Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER),
        NamedIntentFlag("GRANT_READ_URI_PERMISSION", Intent.FLAG_GRANT_READ_URI_PERMISSION),
        NamedIntentFlag("GRANT_WRITE_URI_PERMISSION", Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
        NamedIntentFlag("GRANT_PERSISTABLE_URI_PERMISSION", Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
        NamedIntentFlag("GRANT_PREFIX_URI_PERMISSION", Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),
        NamedIntentFlag("INCLUDE_STOPPED_PACKAGES", Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
        NamedIntentFlag("EXCLUDE_STOPPED_PACKAGES", Intent.FLAG_EXCLUDE_STOPPED_PACKAGES),
        NamedIntentFlag("DEBUG_LOG_RESOLUTION", Intent.FLAG_DEBUG_LOG_RESOLUTION),
    )

    val broadcastEntries: List<NamedIntentFlag> = listOf(
        NamedIntentFlag("RECEIVER_FOREGROUND", Intent.FLAG_RECEIVER_FOREGROUND),
        NamedIntentFlag("RECEIVER_REGISTERED_ONLY", Intent.FLAG_RECEIVER_REGISTERED_ONLY),
        NamedIntentFlag("RECEIVER_REPLACE_PENDING", Intent.FLAG_RECEIVER_REPLACE_PENDING),
        NamedIntentFlag("RECEIVER_NO_ABORT", Intent.FLAG_RECEIVER_NO_ABORT),
        NamedIntentFlag("INCLUDE_STOPPED_PACKAGES", Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
        NamedIntentFlag("EXCLUDE_STOPPED_PACKAGES", Intent.FLAG_EXCLUDE_STOPPED_PACKAGES),
        NamedIntentFlag("GRANT_READ_URI_PERMISSION", Intent.FLAG_GRANT_READ_URI_PERMISSION),
        NamedIntentFlag("GRANT_WRITE_URI_PERMISSION", Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
    )
}
