package com.dan.bmwforgex

import com.dan.bmwforgex.core.*
import org.junit.Assert.*
import org.junit.Test

class OfflineCodingCodecTest {
    private val profile = OfflineEcuProfile(
        "synthetic-body-1", "SYNTHETIC-BCM", "SW_TEST_01", 4,
        listOf(
            CodingFieldDefinition("welcome-light", 1, 0x03, mapOf("Off" to 0, "Low" to 1, "High" to 2)),
            CodingFieldDefinition("lock-chirp", 1, 0x04, mapOf("Off" to 0, "On" to 4)),
            CodingFieldDefinition("display-units", 2, 0x80, mapOf("Metric" to 0, "Imperial" to 128))
        )
    )

    @Test fun changesOnlySpecifiedBitsAndPreservesOriginalSnapshot() {
        val original = byteArrayOf(0x55, 0xF0.toByte(), 0x01, 0x66)
        val out = OfflineCodingCodec.preview(profile, "SW_TEST_01", original,
            mapOf("welcome-light" to "High", "lock-chirp" to "On"))
        assertArrayEquals(byteArrayOf(0x55, 0xF6.toByte(), 0x01, 0x66), out.proposedBytes)
        assertArrayEquals(byteArrayOf(0x55, 0xF0.toByte(), 0x01, 0x66), original)
        assertEquals(listOf(CodingByteChange(1, 0xF0, 0xF6)), out.changes)
        assertNotEquals(out.originalDigest, out.proposedDigest)
    }

    @Test fun rejectsMismatchedFirmwareAndUnknownValues() {
        val bytes = byteArrayOf(0,0,0,0)
        assertThrows(IllegalArgumentException::class.java) {
            OfflineCodingCodec.preview(profile, "UNKNOWN", bytes, mapOf("lock-chirp" to "On"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflineCodingCodec.preview(profile, "SW_TEST_01", bytes, mapOf("lock-chirp" to "Unrecognised"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflineCodingCodec.preview(profile, "SW_TEST_01", byteArrayOf(1), mapOf("lock-chirp" to "On"))
        }
    }

    @Test fun rejectsOverlappingFieldsAndOutOfRangeBits() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineCodingCodec.validate(profile.copy(fields = profile.fields + CodingFieldDefinition(
                "overlap", 1, 0x01, mapOf("On" to 1))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflineCodingCodec.validate(profile.copy(fields = listOf(CodingFieldDefinition(
                "invalid-bits", 1, 0x02, mapOf("Overflow" to 0x80)))))
        }
    }

    @Test fun offlineSimulatorUsesExactlyOneByteForDemoChange() {
        val feature = UniversalCoding.features.first()
        val result = CodingSandbox.simulate(feature, "Disabled")
        assertTrue(result.verified)
        assertEquals(1, result.changedBytes)
    }
}
