package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.geo.UkraineCities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OblastCentresTest {
    @Test
    fun everyOblastHasItsCentreAndNoNamesakeTown() {
        val centres = UkraineCities.oblastCentres
        assertEquals(26, centres.size)
        assertEquals(26, centres.map { it.name }.toSet().size, "one city per name")
        assertTrue(
            centres.single { it.name == "Mykolaiv" }.lon > 31,
            "the oblast centre on the Southern Bug, not the town near Lviv",
        )
        assertTrue(
            centres.single { it.name == "Rivne" }.lat > 50,
            "the oblast centre, not the town in Kirovohrad Oblast",
        )
    }
}
