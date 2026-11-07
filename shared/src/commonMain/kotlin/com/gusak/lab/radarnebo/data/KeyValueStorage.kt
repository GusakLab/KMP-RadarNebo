package com.gusak.lab.radarnebo.data

/** Minimal platform key-value storage (SharedPreferences on Android, NSUserDefaults on iOS). */
interface KeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}
