package com.ubopod.uboapp.wear.ui.controls

import com.ubopod.ubokotlin.models.AssistantTriggerSource
import com.ubopod.ubokotlin.models.WakeMode

/**
 * Trigger sent when the watch starts a listening session.
 *
 * Declared as a quick-chat wake so the core applies its quick-chat policy: the
 * pod ends the turn after its configured silence window rather than waiting for
 * the watch to stop the session. That also arms the core's stage-1
 * voice-shortcut grammar against this session's audio source, matching what a
 * spoken quick-chat wake does on the device.
 *
 * `phrase`/`detector` are diagnostic only — nothing in the core branches on
 * them — so they name the real trigger rather than a spoken phrase.
 *
 * Mirrors `WatchAssistantTrigger.swift` in the watchOS app.
 */
internal val WATCH_ASSISTANT_TRIGGER: AssistantTriggerSource =
    AssistantTriggerSource.WakePhrase(
        phrase = "watch button",
        detector = "wear",
        mode = WakeMode.QUICK_CHAT,
    )
