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

    // Cross-brand planner never claims real ECU coding support.
    check(UniversalCoding.makes.containsAll(listOf("BMW", "Ford", "Toyota", "Volkswagen", "Honda")))
    val make = UniversalCoding.makeFromGarage("Toyota Corolla")
    check(make == "Toyota")
    val target = VehicleDescriptor("Toyota", "Corolla", "2020", "JTDBR32E720012345")
    val draft = UniversalCoding.plan(target, "cluster-units", "Metric")
    check(!draft.liveWriteEnabled)
    check(draft.requirements.any { it.contains("coding parameter") })
    check(!draft.safeSummary().contains("JTDBR32E720012345"))
    check(draft.safeSummary().contains("***12345"))
    check(runCatching { UniversalCoding.plan(target, "cluster-units", "Deactivate airbags") }.isFailure)
    check(!UniversalCoding.isVinFormatValid("INVALIDVIN"))
    val sandbox = CodingSandbox.simulate(UniversalCoding.features.first(), "Disabled")
    check(sandbox.verified && sandbox.beforeHash != sandbox.afterHash)
    val positive = UdsDidDecoder.parseReadOnly("62 F1 90 57 42 41", 0xF190)
    check(positive == UdsDidReply.Data(0xF190, listOf(0x57, 0x42, 0x41)))
    check(UdsDidDecoder.parseReadOnly("7F 22 31", 0xF190) == UdsDidReply.Negative(0x31))
    check(UdsDidDecoder.parseReadOnly("NO DATA", 0xF190) == UdsDidReply.Missing)
}
