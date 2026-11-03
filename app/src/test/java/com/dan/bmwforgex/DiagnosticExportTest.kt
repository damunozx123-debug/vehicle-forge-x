package com.dan.bmwforgex

import com.dan.bmwforgex.core.*
import org.junit.Assert.*
import org.junit.Test

class DiagnosticExportTest {
    @Test fun genericReportExcludesUserIdentifiersAndKeepsDtcMeaning() {
        val report = DiagnosticExport.summary(
            mapOf("Stored" to listOf("P0301", "P0301", "P0133"),
                "Pending" to listOf("P0420"), "Permanent" to emptyList()),
            Readiness(true, 2, true, 0, 0, 0), false, 1780000000000L)
        assertTrue(report.contains("P0301 | Cylinder 1 misfire"))
        assertTrue(report.contains("Stored faults (2):"))
        assertTrue(report.contains("MIL: ON"))
        assertTrue(report.contains("VIN, plate, adapter MAC"))
        assertFalse(report.contains("JTDBR32E"))
    }

    @Test fun demoReportIsClearlySynthetic() {
        assertTrue(DiagnosticExport.summary(emptyMap(), null, true, 0)
            .contains("SYNTHETIC DEMO - NOT VEHICLE DATA"))
    }

    @Test fun udsOfflineDecoderHandlesCompletePositiveAndNegativeResponses() {
        val positive = UdsDtcDecoder.parsePayload("59 02 FF 12 34 56 09")
        assertTrue(positive is UdsDtcResult.Report)
        val entry = (positive as UdsDtcResult.Report).dtcs.single()
        assertEquals("123456", entry.hex)
        assertTrue(entry.active)
        assertTrue(entry.confirmed)
        assertFalse(entry.pending)
        assertEquals(UdsDtcResult.Negative(0x31), UdsDtcDecoder.parsePayload("7F 19 31"))
    }

    @Test fun udsDecoderRejectsIncompleteAndMalformedFrames() {
        assertEquals(UdsDtcResult.Unrecognised, UdsDtcDecoder.parsePayload("NO DATA"))
        assertTrue((UdsDtcDecoder.parsePayload("5902FF1234") as UdsDtcResult.Report).truncated)
        assertEquals(UdsDtcResult.Unrecognised, UdsDtcDecoder.parsePayload("5902FG"))
    }
}
