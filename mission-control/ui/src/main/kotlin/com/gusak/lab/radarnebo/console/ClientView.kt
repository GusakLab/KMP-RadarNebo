package com.gusak.lab.radarnebo.console

/**
 * How the console shows the client app: not at all, in a pane on the right, or over the whole console below the top
 * bar.
 */
internal enum class ClientView(val label: String, val glyph: String) {
    Hidden("Hidden", "▢"),
    Embedded("Embedded", "◨"),
    Fullscreen("Fullscreen", "■"),
    ;

    val next: ClientView get() = entries[(ordinal + 1) % entries.size]
}
