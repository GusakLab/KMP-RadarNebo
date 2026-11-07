package com.gusak.lab.radarnebo.server.config

/**
 * Where the server's alerts come from. Clients never see this: they only talk to the server.
 *
 * [key] is stored as the archive partition and served as `/health`'s `source`, so it must never change,
 * even if an entry is renamed.
 */
enum class DataSourceKind(internal val key: String) {
    AlertsInUa("AlertsInUa"),
    Simulation("Simulation"),
}
