package com.gusak.lab.radarnebo.console

import ch.qos.logback.classic.Level

internal data class LogLine(val id: Long, val time: Long, val level: Level, val logger: String, val message: String)
