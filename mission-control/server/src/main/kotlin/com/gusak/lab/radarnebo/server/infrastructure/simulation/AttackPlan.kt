package com.gusak.lab.radarnebo.server.infrastructure.simulation

/** What an attack consists of: the flights it launches and the regional threats around them. */
internal class AttackPlan(val flights: List<Flight>, val areas: List<AreaThreat>)
