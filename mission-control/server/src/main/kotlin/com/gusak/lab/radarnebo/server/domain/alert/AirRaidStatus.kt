package com.gusak.lab.radarnebo.server.domain.alert

/**
 * Air raid status of one location, as the alerts.in.ua IoT endpoints encode it: one character per
 * location. v1 does not grade alerts, v2 tells red alerts from yellow (preliminary) ones.
 */
internal enum class AirRaidStatus(val v1: Char, val v2: Char) {
    /** Air raid alert over the whole location, red level. */
    Active(v1 = 'A', v2 = 'R'),

    /** Preliminary threat over the whole location, yellow level. */
    Preliminary(v1 = 'A', v2 = 'Y'),

    /** Alerts in some of the location's raions or hromadas only. */
    Partial(v1 = 'P', v2 = 'P'),

    /** No air raid alert. */
    None(v1 = 'N', v2 = 'N');

    fun code(version: IotVersion): Char = if (version == IotVersion.V1) v1 else v2
}
