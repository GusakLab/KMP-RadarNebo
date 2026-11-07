package com.gusak.lab.radarnebo.server.infrastructure.simulation

/** A strike objective: a city with what makes it a target, weighted by how often it is attacked. */
internal class Objective(val settlement: String, val weight: Int)
