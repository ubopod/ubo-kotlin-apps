package com.ubopod.uboapp.phone.ui.common

import com.ubopod.ubokotlin.models.MenuItemData

/**
 * Partitions a notification's `items` list into the three contracts the
 * Web UI / Swift port use:
 *   - **main actions** — the buttons rendered in the body of the
 *     notification card;
 *   - **extra info** — an optional accent icon shown next to
 *     `extraInformation` (typically a "read aloud" affordance);
 *   - **dismiss** — whether the device sent an explicit dismiss item,
 *     which lets the renderer decide between showing a dedicated
 *     "Dismiss" footer vs. relying on the `Back` gesture.
 *
 * Mirrors `ubo-swift-app/.../Utilities/NotificationItems.swift`
 * (commit `97667df`). Keys + actionId-prefix constants match the
 * Web UI's `web-app/src/store/constants.ts`.
 */
public object NotificationItems {
    public const val DISMISS_KEY: String = "dismiss"
    public const val EXTRA_INFO_KEY: String = "extra_info"
    public const val DISMISS_PREFIX: String = "notification:dismiss:"
    public const val EXTRA_INFO_PREFIX: String = "notification:extra_info:"

    public fun isDismiss(item: MenuItemData): Boolean =
        item.key == DISMISS_KEY || item.actionId?.startsWith(DISMISS_PREFIX) == true

    public fun isExtraInfo(item: MenuItemData): Boolean =
        item.key == EXTRA_INFO_KEY || item.actionId?.startsWith(EXTRA_INFO_PREFIX) == true
}

public data class PartitionedNotificationItems(
    val mainActions: List<MenuItemData>,
    val extraInfo: MenuItemData?,
    val hasDismiss: Boolean,
)

public fun partitionNotificationItems(items: List<MenuItemData?>): PartitionedNotificationItems {
    val unwrapped = items.filterNotNull()
    val extra = unwrapped.firstOrNull(NotificationItems::isExtraInfo)
    val dismiss = unwrapped.any(NotificationItems::isDismiss)
    val main = unwrapped.filterNot {
        NotificationItems.isExtraInfo(it) || NotificationItems.isDismiss(it)
    }
    return PartitionedNotificationItems(mainActions = main, extraInfo = extra, hasDismiss = dismiss)
}
