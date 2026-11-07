package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.debug.TrafficRecorder

/** Configures [ServerTrafficCapture]: where calls are recorded, and the clock that times them. */
internal class TrafficCaptureConfig {
    var recorder: TrafficRecorder = TrafficRecorder { }

    /** Times each call; the server passes its own clock. */
    var clock: () -> Long = System::currentTimeMillis
}
