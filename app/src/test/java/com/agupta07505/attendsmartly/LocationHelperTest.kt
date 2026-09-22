/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationHelperTest {

    @Test
    fun testFormatCoordinates() {
        val formattedNorthEast = LocationHelper.formatCoordinates(28.6139, 77.2090)
        assertTrue(formattedNorthEast.contains("28.61390° N"))
        assertTrue(formattedNorthEast.contains("77.20900° E"))

        val formattedSouthWest = LocationHelper.formatCoordinates(-33.8688, -151.2093)
        assertTrue(formattedSouthWest.contains("33.86880° S"))
        assertTrue(formattedSouthWest.contains("151.20930° W"))
    }

    @Test
    fun testDistanceSamePoint() {
        val distance = LocationHelper.distanceMeters(28.6139, 77.2090, 28.6139, 77.2090)
        assertEquals(0f, distance, 0.01f)
    }

    @Test
    fun testIsWithinRangeInside() {
        // Point 1: 28.6139, 77.2090
        // Point 2: very close (approx 8.8 meters away)
        val lat1 = 28.6139
        val lng1 = 77.2090
        val lat2 = 28.61398
        val lng2 = 77.2090

        val distance = LocationHelper.distanceMeters(lat1, lng1, lat2, lng2)
        assertTrue("Distance ($distance m) should be around 5-15 meters", distance in 5f..15f)

        // Within 50 meters radius
        assertTrue(LocationHelper.isWithinRange(lat1, lng1, lat2, lng2, radiusMeters = 50))
    }

    @Test
    fun testIsWithinRangeOutside() {
        // Point 1: 28.6139, 77.2090
        // Point 2: approx 500 meters away
        val lat1 = 28.6139
        val lng1 = 77.2090
        val lat2 = 28.6185
        val lng2 = 77.2090

        val distance = LocationHelper.distanceMeters(lat1, lng1, lat2, lng2)
        assertTrue("Distance ($distance m) should be > 100 meters", distance > 100f)

        // Outside 50 meters radius
        assertFalse(LocationHelper.isWithinRange(lat1, lng1, lat2, lng2, radiusMeters = 50))
    }
}
