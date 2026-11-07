package com.gusak.lab.radarnebo.domain.usecase

/** What a settings change means for the data already fetched. */
data class SettingsUpdate(
    /** Another server or access key: everything fetched from the old one was dropped. */
    val sourceChanged: Boolean,
)
