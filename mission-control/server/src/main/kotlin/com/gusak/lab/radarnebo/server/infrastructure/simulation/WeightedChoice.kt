package com.gusak.lab.radarnebo.server.infrastructure.simulation

import kotlin.random.Random

/** One element drawn from [random], each as likely as its [weight] makes it. */
internal fun <T> List<T>.weighted(random: Random, weight: (T) -> Int): T {
    var roll = random.nextInt(sumOf(weight))
    return first { roll -= weight(it); roll < 0 }
}
