package com.gusak.lab.radarnebo

internal interface Platform {
    val name: String
}

internal expect fun getPlatform(): Platform