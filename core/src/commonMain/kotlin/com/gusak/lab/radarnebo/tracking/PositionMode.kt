package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

@Serializable
enum class PositionMode { Held, Interpolated, Extrapolated }
