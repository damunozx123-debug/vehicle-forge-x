package com.dan.bmwforgex

import com.dan.bmwforgex.core.*
import org.junit.Assert.*
import org.junit.Test

class CodingPlanTest {
    @Test fun catalogueHasRealF10ModuleDependencies() {
        assertTrue(F10CodingCatalog.features.any { it.id == "auto-stop-memory" && "CAS" in it.controlUnits })
        assertTrue(F10CodingCatalog.features.any { it.id == "climate-memory" && "IHKA_CBF" in it.controlUnits })
    }
    @Test fun missingVerifiedBackendBlocksVehicleWrites() {
        val feature = F10CodingCatalog.features.first()
        val backend = object : VerifiedCodingBackend {
            override val validatedVin = "WBA00000000000001"
            override val validatedSoftwareId = "TEST_VERSION"
            override val validatedModule = "CAS"
            override val isVehicleWriteApproved = false
            override suspend fun readSnapshot(): CodingSnapshot = error("not used")
            override suspend fun encodeChange(original: CodingSnapshot, change: CodingChange): ByteArray = error("not used")
            override suspend fun writeCoding(bytes: ByteArray): Unit = error("not used")
            override suspend fun readBack(): CodingSnapshot = error("not used")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CodingTransaction.check(feature, CodingChange(feature.id,"Active","TEST_VERSION"),
                CodingEnvironment(true,true,true,13.6,true),backend)
        }
    }
}
