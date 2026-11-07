package com.gusak.lab.radarnebo.data

import kotlinx.serialization.Serializable

@Serializable
enum class ThreatLevel {
    Red,
    Yellow,
    ;

    companion object {
        fun from(value: String?): ThreatLevel = if (value.equals("yellow", ignoreCase = true)) Yellow else Red
    }
}
