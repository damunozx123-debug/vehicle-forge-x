package com.dan.bmwforgex.core

import java.security.MessageDigest

/** F10 user-facing coding catalog; labels are NOT a validated CAFD-to-bytes mapping. */
data class CodingFeature(
    val id: String,
    val name: String,
    val controlUnits: Set<String>,
    val choices: List<String>,
    val explanation: String,
    val requirements: String
)

object F10CodingCatalog {
    const val SOURCE = "https://bimmercode.app/vehicles/f10/"
    val features = listOf(
        CodingFeature("auto-stop-memory", "Auto Start/Stop memory", setOf("CAS"), listOf("Active", "Not active"),
            "Remembers the last Start/Stop preference. ECO PRO can override it.", "Car Access System module with matching coding description"),
        CodingFeature("auto-stop-default", "Auto Start/Stop off by default", setOf("CAS"), listOf("Active", "Not active"),
            "Sets the Auto Start/Stop preference at engine start. This is not an emissions ECU tune.", "Compatible CAS software"),
        CodingFeature("climate-memory", "Air conditioning setting memory", setOf("IHKA_CBF"), listOf("Active", "Not active"),
            "Restores the last air-conditioning setting after restarting.", "IHKA_CBF fitted"),
        CodingFeature("air-recirculation", "Air circulation setting memory", setOf("IHKA_CBF"), listOf("Active", "Not active"),
            "Remembers the last selected air circulation setting.", "IHKA_CBF fitted"),
        CodingFeature("lock-confirmation", "Acoustic lock confirmation", setOf("FZD__04"), listOf("Active", "Not active"),
            "Sounds confirmation when locking or unlocking, where the alarm hardware supports it.", "FZD__04 and factory alarm"),
        CodingFeature("bluetooth-audio", "Bluetooth audio streaming", setOf("HU_NBT", "HU_NBT_EVO"), listOf("Active", "Not active"),
            "Enables supported Bluetooth audio functionality; equipment and software must support it.", "Matching NBT or NBT EVO and Bluetooth media hardware"),
        CodingFeature("comfort-close", "Comfort closing with remote", setOf("CAS"), listOf("Active", "Not active"),
            "Holding the remote lock button can close supported windows and fold mirrors when equipped.", "Compatible CAS and powered folding mirrors for mirror function")
    )
}

data class CodingIdentity(val vin: String, val ecu: String, val softwareId: String)
data class CodingEnvironment(val stationary: Boolean, val ignitionOn: Boolean, val engineOff: Boolean,
                             val supplyVolts: Double, val externalPowerSupplyConfirmed: Boolean)
data class CodingSnapshot(val identity: CodingIdentity, val bytes: ByteArray) {
    fun digest() = sha256(bytes)
}
data class CodingChange(val featureId: String, val optionValue: String, val expectedSoftwareId: String)

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }

/** A provider must be independently validated for an exact ECU software version before use. */
interface VerifiedCodingBackend {
    val validatedVin: String
    val validatedSoftwareId: String
    val validatedModule: String
    val isVehicleWriteApproved: Boolean
    suspend fun readSnapshot(): CodingSnapshot
    suspend fun encodeChange(original: CodingSnapshot, change: CodingChange): ByteArray
    suspend fun writeCoding(bytes: ByteArray)
    suspend fun readBack(): CodingSnapshot
}

data class CodingOutcome(val previousHash: String, val verifiedHash: String, val verified: Boolean)

/** Fail-closed transactional shell, not a BMW coding implementation. */
object CodingTransaction {
    fun check(feature: CodingFeature, selection: CodingChange, context: CodingEnvironment,
              backend: VerifiedCodingBackend) {
        require(backend.isVehicleWriteApproved) { "Vehicle coding backend is unverified. Writes are disabled." }
        require(feature.id == selection.featureId) { "Feature/selection mismatch" }
        require(selection.optionValue in feature.choices) { "Unsupported option value" }
        require(backend.validatedModule in feature.controlUnits) { "ECU is not compatible with this feature" }
        require(selection.expectedSoftwareId == backend.validatedSoftwareId && selection.expectedSoftwareId.isNotBlank()) {
            "ECU software version is not matched to a validated coding mapping"
        }
        require(backend.validatedVin.length == 17) { "ECU VIN not verified" }
        require(context.stationary && context.ignitionOn && context.engineOff) { "Park and turn on ignition; engine must be stopped" }
        require(context.externalPowerSupplyConfirmed && context.supplyVolts in 13.0..14.8) {
            "Stable external coding supply required, voltage 13.0-14.8 V"
        }
    }

    suspend fun apply(feature: CodingFeature, selection: CodingChange, context: CodingEnvironment,
                      backend: VerifiedCodingBackend, preserveBackup: suspend (CodingSnapshot) -> Boolean): CodingOutcome {
        check(feature, selection, context, backend)
        val original = backend.readSnapshot()
        require(original.identity.vin == backend.validatedVin && original.identity.ecu == backend.validatedModule &&
            original.identity.softwareId == backend.validatedSoftwareId) { "Snapshot identity mismatch" }
        require(original.bytes.isNotEmpty()) { "No coding bytes were read" }
        require(preserveBackup(original)) { "Backup was not successfully saved" }
        val proposed = backend.encodeChange(original, selection)
        require(proposed.isNotEmpty() && !proposed.contentEquals(original.bytes)) { "No validated change to apply" }
        backend.writeCoding(proposed)
        val readBack = backend.readBack()
        val valid = readBack.identity == original.identity && readBack.bytes.contentEquals(proposed)
        return CodingOutcome(original.digest(), readBack.digest(), valid)
    }
}
