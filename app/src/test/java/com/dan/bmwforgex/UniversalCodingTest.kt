package com.dan.bmwforgex

import com.dan.bmwforgex.core.*
import org.junit.Assert.*
import org.junit.Test

class UniversalCodingTest {
    private val vehicle = VehicleDescriptor("Ford", "Focus", "2018", "WF000000000000001")

    @Test fun featureCatalogIsMakeNeutralAndNeverOffersVehicleWrites() {
        assertTrue(UniversalCoding.makes.containsAll(listOf("Ford", "Toyota", "BMW", "Volkswagen")))
        for (feature in UniversalCoding.features) {
            val plan = UniversalCoding.plan(vehicle, feature.id, feature.options.first())
            assertFalse(plan.liveWriteEnabled)
            assertTrue(plan.requirements.any { it.contains("coding parameter") })
        }
    }

    @Test fun planDoesNotSharePlaintextVin() {
        val plan = UniversalCoding.plan(vehicle, "mirror-fold", "Enabled")
        assertFalse(plan.safeSummary().contains(vehicle.vin))
        assertTrue(plan.safeSummary().contains("***00001"))
    }

    @Test fun badSelectionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            UniversalCoding.plan(vehicle, "mirror-fold", "Disable ABS")
        }
        assertThrows(IllegalArgumentException::class.java) {
            UniversalCoding.plan(vehicle, "unknown-feature", "Enabled")
        }
    }

    @Test fun simulatorDoesNotClaimLiveCoding() {
        val demo = CodingSandbox.simulate(UniversalCoding.features.first(), "Disabled")
        assertTrue(demo.verified)
        assertNotEquals(demo.beforeHash, demo.afterHash)
    }

    @Test fun udsResponsesParsedOfflineOnly() {
        assertEquals(UdsDidReply.Data(0xF190, listOf(0x57, 0x42, 0x41)),
            UdsDidDecoder.parseReadOnly("62 F1 90 57 42 41", 0xF190))
        assertEquals(UdsDidReply.Negative(0x31),
            UdsDidDecoder.parseReadOnly("7F 22 31", 0xF190))
        assertEquals(UdsDidReply.Missing,
            UdsDidDecoder.parseReadOnly("NO DATA", 0xF190))
    }
}
