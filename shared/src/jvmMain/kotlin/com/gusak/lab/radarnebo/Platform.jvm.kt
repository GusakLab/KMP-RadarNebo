package com.gusak.lab.radarnebo

internal class JvmPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")} on ${System.getProperty("os.name")}"
}

internal actual fun getPlatform(): Platform = JvmPlatform()
