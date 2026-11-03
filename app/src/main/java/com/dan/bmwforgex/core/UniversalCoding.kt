package com.dan.bmwforgex.core

import java.util.Locale

/**
 * Cross-brand coding PLANNER. This intentionally never sends vehicle commands.
 * Generic OBD-II does not define the manufacturer-specific bytes required to
 * configure body, lighting, safety, or comfort ECUs.
 */
data class VehicleFeature(
    val id: String,
    val title: String,
    val family: String,
    val options: List<String>,
    val explanation: String,
    val moduleHint: String,
    val warning: String = "Manufacturer/ECU/firmware applicability has not been verified."
)

data class VehicleDescriptor(
    val make: String,
    val model: String,
    val year: String,
    val vin: String
)

data class VehicleCodingPlan(
    val target: VehicleDescriptor,
    val feature: VehicleFeature,
    val selectedValue: String,
    val requirements: List<String>,
    val liveWriteEnabled: Boolean = false
) {
    fun safeSummary(): String = buildString {
        appendLine("VEHICLE FORGE X - CONFIGURATION PLAN (NOT APPLIED)")
        appendLine("Make: ${target.make}; Model: ${target.model.ifBlank { "Unspecified" }}")
        appendLine("Year: ${target.year.ifBlank { "Unspecified" }}")
        appendLine("VIN: ${if (target.vin.length == 17) "***" + target.vin.takeLast(5) else "Not verified"}")
        appendLine("Feature: ${feature.title}")
        appendLine("Requested value: $selectedValue")
        appendLine("Possible module family: ${feature.moduleHint}")
        appendLine("Status: NO VEHICLE WRITES - no validated ECU coding dataset")
        requirements.forEachIndexed { index, item -> appendLine("${index + 1}. $item") }
    }
}

object UniversalCoding {
    // A make appearing here means it can be described in the garage UI.
    // It does NOT mean its ECUs have been validated or tested.
    val makes = listOf(
        "BMW", "MINI", "Audi", "Volkswagen", "Skoda", "SEAT", "Cupra",
        "Mercedes-Benz", "Ford", "Toyota", "Honda", "Hyundai", "Kia",
        "Mazda", "Nissan", "Subaru", "Volvo", "Peugeot", "Citroen",
        "Renault", "Mitsubishi", "Suzuki", "Other"
    )

    val features = listOf(
        VehicleFeature("lock-chirp", "Lock / unlock confirmation", "Body",
            listOf("Enabled", "Disabled"),
            "Some body modules can provide acoustic or light confirmation when locking.",
            "Body control / central electronics",
            "Hardware and local noise regulations may limit availability."),
        VehicleFeature("auto-lock", "Automatic door locking", "Body",
            listOf("Enabled", "Disabled"),
            "Possible automatic locking preference; operation differs by vehicle.",
            "Body control / central electronics",
            "Only consider behaviour consistent with occupant egress and safety."),
        VehicleFeature("mirror-fold", "Mirror folding on lock", "Comfort",
            listOf("Enabled", "Disabled"),
            "Some vehicles with powered folding mirrors offer a locking preference.",
            "Door / body / comfort module",
            "Requires factory hardware. Never assume a feature is physically present."),
        VehicleFeature("home-light", "Coming-home lighting", "Lighting",
            listOf("Enabled", "Disabled"),
            "Potential welcome/exit lamp preference on compatible vehicles.",
            "Body / lighting module"),
        VehicleFeature("climate-memory", "Climate last-setting memory", "Comfort",
            listOf("Enabled", "Disabled"),
            "Retain the previous climate setting where supported.",
            "HVAC / climate controller"),
        VehicleFeature("cluster-units", "Display units", "Instruments",
            listOf("Metric", "Imperial"),
            "Instrument cluster unit preference, if a configurable parameter exists.",
            "Instrument cluster",
            "Changing units must not interfere with legal speed display requirements."),
        VehicleFeature("comfort-windows", "Remote comfort windows", "Comfort",
            listOf("Enabled", "Disabled"),
            "Remote window operation where factory anti-pinch hardware is fitted.",
            "Body / door controller",
            "Window anti-pinch protection must remain operational."),
        VehicleFeature("auto-start-memory", "Start/stop preference memory", "Powertrain comfort",
            listOf("Enabled", "Disabled"),
            "Potential memory of driver preference, only if manufacturer supports it.",
            "Body / powertrain gateway",
            "Emissions and roadworthiness rules vary by jurisdiction; no ECU map is supplied.")
    )

    fun makeFromGarage(description: String): String =
        makes.firstOrNull { description.trim().startsWith(it, ignoreCase = true) } ?: "Other"

    fun plan(target: VehicleDescriptor, featureId: String, selectedValue: String): VehicleCodingPlan {
        val feature = features.firstOrNull { it.id == featureId }
            ?: throw IllegalArgumentException("Unknown feature")
        require(selectedValue in feature.options) { "Selection is not in the feature choices" }
        require(target.make in makes) { "Unknown make; use Other" }
        val blockers = mutableListOf(
            "Identify the exact target ECU and its firmware/software identifier.",
            "Obtain a licensed, version-matched coding parameter definition.",
            "Establish a proven physical ECU transport, with authorised diagnostic access.",
            "Save and independently verify a full original coding backup.",
            "Validate power, parked state, rollback and a post-write read-back on a bench.",
            "Enable writes only after documented make/model/ECU-specific regression tests."
        )
        if (target.model.isBlank() || target.year.isBlank()) blockers.add(0, "Complete the vehicle model and build year.")
        if (!isVinFormatValid(target.vin)) blockers.add(0, "Read and verify the vehicle's 17-character VIN.")
        return VehicleCodingPlan(target, feature, selectedValue, blockers)
    }

    fun isVinFormatValid(vin: String): Boolean =
        vin.length == 17 && vin.uppercase(Locale.ROOT).matches(Regex("[A-HJ-NPR-Z0-9]{17}"))
}

/** Simulation only. All data are synthetic and no transport or ECU is referenced. */
data class DemoCodingResult(
    val beforeHash: String, val afterHash: String, val verified: Boolean, val description: String
)
object CodingSandbox {
    fun simulate(feature: VehicleFeature, selectedValue: String): DemoCodingResult {
        require(selectedValue in feature.options)
        val before = "SYNTHETIC-ECU:${feature.id}=${feature.options.first()}".toByteArray(Charsets.UTF_8)
        val after = "SYNTHETIC-ECU:${feature.id}=$selectedValue".toByteArray(Charsets.UTF_8)
        val readBack = after.copyOf()
        return DemoCodingResult(sha256(before), sha256(after),
            readBack.contentEquals(after), "Synthetic memory changed in an isolated demonstration")
    }
}

/** Parses responses supplied by the user, but does not send diagnostic requests. */
sealed class UdsDidReply {
    data class Data(val did: Int, val bytes: List<Int>) : UdsDidReply()
    data class Negative(val code: Int) : UdsDidReply()
    data object Missing : UdsDidReply()
}
object UdsDidDecoder {
    fun parseReadOnly(raw: String, did: Int): UdsDidReply {
        require(did in 0..0xFFFF) { "DID must be exactly two bytes" }
        val bytes = ObdCore.hexBytes(raw)
        val hi = did ushr 8
        val lo = did and 0xFF
        for (i in 0 until (bytes.size - 2).coerceAtLeast(0)) {
            if (bytes[i] == 0x62 && bytes[i + 1] == hi && bytes[i + 2] == lo) {
                return UdsDidReply.Data(did, bytes.drop(i + 3))
            }
            if (bytes[i] == 0x7F && bytes[i + 1] == 0x22) {
                return UdsDidReply.Negative(bytes[i + 2])
            }
        }
        return UdsDidReply.Missing
    }
}
