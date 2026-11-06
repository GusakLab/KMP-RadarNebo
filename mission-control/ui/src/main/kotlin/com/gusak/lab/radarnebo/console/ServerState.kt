package com.gusak.lab.radarnebo.console

internal sealed interface ServerState {
    data object Stopped : ServerState
    data object Starting : ServerState
    data class Running(val port: Int, val since: Long) : ServerState
    data object Stopping : ServerState
    data class Failed(val message: String) : ServerState
}
