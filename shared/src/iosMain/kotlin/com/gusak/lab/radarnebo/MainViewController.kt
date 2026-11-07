package com.gusak.lab.radarnebo

import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.gusak.lab.radarnebo.data.ThemeMode
import com.gusak.lab.radarnebo.di.initKoinIfNeeded
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoinIfNeeded()
    return ComposeUIViewController {
        // The controller this content is hosted in: the one returned here.
        val controller = LocalUIViewController.current
        App(
            // Override the interface style only for an explicit choice; "System" must release it, or
            // the system appearance could no longer be detected.
            onThemeApplied = { mode, _ ->
                controller.overrideUserInterfaceStyle = when (mode) {
                    ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                    ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
                }
            },
        )
    }
}
