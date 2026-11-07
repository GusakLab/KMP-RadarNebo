package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.debug.TrafficMasking

/** The RadarNebo server the app talks to. [accessKey] is only needed when the server requires client keys. */
data class ServerEndpoint(val url: String, val accessKey: String = "") {
    // The access key is a credential: the generated toString would print it wherever an endpoint is logged.
    override fun toString(): String =
        "ServerEndpoint(url=$url, accessKey=${if (accessKey.isEmpty()) "" else TrafficMasking.MASK})"
}
