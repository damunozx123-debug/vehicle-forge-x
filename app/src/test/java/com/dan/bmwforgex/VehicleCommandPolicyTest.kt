package com.dan.bmwforgex

import com.dan.bmwforgex.core.VehicleCommandPolicy
import org.junit.Assert.*
import org.junit.Test

class VehicleCommandPolicyTest {
    @Test fun allowsOnlySafeUserReadCommands() {
        for (cmd in listOf("010C", "0100", "0902", "03", "07", "0A", "020C00", "ATRV", "ATDP")) {
            assertTrue(cmd, VehicleCommandPolicy.allowedLabRead(cmd))
        }
    }

    @Test fun transportAllowsInternalInitButUserLabDoesNot() {
        for (cmd in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "ATCAF1", "ATAT1", "ATST64")) {
            assertTrue(cmd, VehicleCommandPolicy.allowedAdapterRequest(cmd))
            assertFalse(cmd, VehicleCommandPolicy.allowedLabRead(cmd))
        }
    }

    @Test fun refusesAllWriteOrStateChangingUdsAndObdCommands() {
        for (cmd in listOf("04", "2E1234FF", "2701", "1003", "1101", "3101",
            "3400", "3600", "3E00", "ATSH7E0", "ATCRA7E8",
            "012345", "010C\r04", "020C01", "garbage")) {
            assertFalse(cmd, VehicleCommandPolicy.allowedAdapterRequest(cmd))
            assertFalse(cmd, VehicleCommandPolicy.allowedLabRead(cmd))
        }
    }
}
