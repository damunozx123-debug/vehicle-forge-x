import com.dan.bmwforgex.core.*

fun codingPlanSmoke() {
    check(F10CodingCatalog.features.any { it.controlUnits.contains("CAS") })
    val feature = F10CodingCatalog.features.first()
    val env = CodingEnvironment(true, true, true, 13.6, true)
    val deny = object : VerifiedCodingBackend {
        override val validatedVin = "WBA00000000000001"
        override val validatedSoftwareId = "DEMO"
        override val validatedModule = "CAS"
        override val isVehicleWriteApproved = false
        override suspend fun readSnapshot() = error("blocked")
        override suspend fun encodeChange(original: CodingSnapshot, change: CodingChange) = error("blocked")
        override suspend fun writeCoding(bytes: ByteArray): Unit = error("blocked")
        override suspend fun readBack() = error("blocked")
    }
    check(runCatching {
        CodingTransaction.check(feature, CodingChange(feature.id, "Active", "DEMO"), env, deny)
    }.isFailure)
}
