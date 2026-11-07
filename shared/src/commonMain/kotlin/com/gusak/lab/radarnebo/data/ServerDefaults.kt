package com.gusak.lab.radarnebo.data

/**
 * Where the app looks for its server, and the key it sends, until the user sets them: client.properties at build time
 * (see [ClientConfig]), else the deployed server without a key, unless a platform overrides it.
 */
data class ServerDefaults(
    val url: String = ClientConfig.SERVER_URL.ifBlank { RadarNeboApi.DEFAULT_SERVER_URL },
    val accessKey: String = ClientConfig.ACCESS_KEY,
)
