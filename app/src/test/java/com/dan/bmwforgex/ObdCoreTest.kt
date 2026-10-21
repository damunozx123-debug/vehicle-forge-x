package com.dan.bmwforgex

import com.dan.bmwforgex.core.*
import org.junit.Assert.*
import org.junit.Test

class ObdCoreTest {
    @Test fun formatsAndCanHeader() {
        for (reply in listOf("41 0C 1A F8","010C\r410C1AF8\r>","7E8 04 41 0C 1A F8"))
            assertEquals(1726.0, Sensors.byPid.getValue(0x0C).decode(reply)!!, 0.0001)
    }
    @Test fun standardSensorEquations() {
        assertEquals(83.0, Sensors.byPid.getValue(0x05).decode("41057B")!!,0.0001)
        assertEquals(14.232, Sensors.byPid.getValue(0x42).decode("41423798")!!,0.0001)
        assertEquals(80.0, Sensors.byPid.getValue(0x5C).decode("415C78")!!,0.0001)
    }
    @Test fun multiEcuDtcNotMerged() {
        assertEquals(listOf("P0133","P0301"),ObdCore.dtcs("7E8 05 43 01 33 00 00\r7E9 05 43 03 01 00 00\r>"))
        assertEquals(listOf("U0123"),ObdCore.dtcs("47 C1 23",7))
    }
    @Test fun supportedPidBitset() {
        assertEquals(setOf(1,32),ObdCore.supportedPids("41 00 80 00 00 01",0))
    }
    @Test fun vinSegmentsAndMissing() {
        assertEquals("WBAFW11C0DE000012",ObdCore.vin("7E8 10 14 49 02 01 57 42\r7E8 21 41 46 57 31 31 43 30\r7E8 22 44 45 30 30 30 30 31 32\r>"))
        assertNull(ObdCore.vin("49 02 01 57 42"))
    }
    @Test fun freezeFrameSeparatedFromLive() {
        assertEquals(1726.0,FreezeFrame.decode("42 0C 00 1A F8",0x0C)!!,0.0001)
        assertNull(FreezeFrame.decode("42 0C 01 1A F8",0x0C))
    }
    @Test fun diagnosticInsightsThresholded() {
        assertTrue(Insights.from(mapOf(0x05 to 118.0)).any { it.level == "Urgent" })
        assertTrue(Insights.from(mapOf(0x42 to 14.2)).none { it.level == "Urgent" })
    }
}
