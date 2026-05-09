package com.ubopod.uboapp.phone

import android.app.Application

/**
 * Custom [Application] entry point for the Ubo phone app.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/ubo_swift_appApp.swift` — global
 * one-time setup goes here (logging level, font registration, …). Kept
 * minimal in v1; we'll add Nerd-Font bootstrap once the icon Composable
 * lands.
 */
public class UboPhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: register ArimoNerdFont via androidx.compose.ui.text.font.Font
        // when the IconView Composable lands; mirrors Swift
        // UboIconFontBootstrap.ensureRegistered().
    }
}
