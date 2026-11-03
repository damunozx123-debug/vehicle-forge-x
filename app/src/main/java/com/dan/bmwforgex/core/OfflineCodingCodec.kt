package com.dan.bmwforgex.core

/**
 * Strict OFFLINE coding-byte planner.
 * This consumes an already obtained snapshot and a caller-supplied definition.
 * No vehicle transport, security access, firmware flash, or ECU write exists here.
 */
data class CodingFieldDefinition(
    val id: String,
    val byteOffset: Int,
    val mask: Int,
    val allowedValues: Map<String, Int>
)

data class OfflineEcuProfile(
    val profileId: String,
    val ecuId: String,
    val softwareId: String,
    val expectedSize: Int,
    val fields: List<CodingFieldDefinition>
)

data class CodingByteChange(val offset: Int, val before: Int, val after: Int)

data class OfflineCodingPreview(
    val originalDigest: String,
    val proposedDigest: String,
    val changes: List<CodingByteChange>,
    val proposedBytes: ByteArray,
    val profileId: String
)

object OfflineCodingCodec {
    fun validate(profile: OfflineEcuProfile) {
        require(profile.profileId.isNotBlank() && profile.ecuId.isNotBlank() &&
            profile.softwareId.isNotBlank()) { "Incomplete ECU identity" }
        require(profile.expectedSize in 1..65536) { "Snapshot byte length out of range" }
        require(profile.fields.isNotEmpty()) { "No coding fields" }
        val used = mutableMapOf<Int, Int>()
        val ids = mutableSetOf<String>()
        for (field in profile.fields) {
            require(field.id.isNotBlank() && ids.add(field.id)) { "Duplicate or blank field identifier" }
            require(field.byteOffset in 0 until profile.expectedSize) { "Coding offset out of bounds" }
            require(field.mask in 1..255) { "Coding mask must contain at least one bit" }
            require(((used[field.byteOffset] ?: 0) and field.mask) == 0) {
                "Overlapping coding definitions on byte ${field.byteOffset}"
            }
            used[field.byteOffset] = (used[field.byteOffset] ?: 0) or field.mask
            require(field.allowedValues.isNotEmpty()) { "No allowed choices" }
            for ((label, bits) in field.allowedValues) {
                require(label.isNotBlank() && bits in 0..255 && bits and field.mask == bits) {
                    "Choice extends beyond field mask"
                }
            }
        }
    }

    fun preview(
        profile: OfflineEcuProfile,
        softwareIdFromSnapshot: String,
        originalBytes: ByteArray,
        selections: Map<String, String>
    ): OfflineCodingPreview {
        validate(profile)
        require(profile.softwareId == softwareIdFromSnapshot) { "Exact ECU software ID mismatch" }
        require(originalBytes.size == profile.expectedSize) { "ECU coding snapshot length mismatch" }
        require(selections.isNotEmpty()) { "No changes selected" }
        require(selections.size <= profile.fields.size) { "Too many field selections" }
        val out = originalBytes.copyOf()
        val byId = profile.fields.associateBy { it.id }
        for ((id, choice) in selections) {
            val field = byId[id] ?: throw IllegalArgumentException("Unknown coding field")
            val value = field.allowedValues[choice]
                ?: throw IllegalArgumentException("Unsupported field value")
            val original = out[field.byteOffset].toInt() and 0xff
            out[field.byteOffset] = ((original and field.mask.inv()) or value).toByte()
        }
        val changes = originalBytes.indices.mapNotNull { i ->
            val before = originalBytes[i].toInt() and 0xff
            val after = out[i].toInt() and 0xff
            if (before == after) null else CodingByteChange(i, before, after)
        }
        require(changes.isNotEmpty()) { "Selection causes no byte changes" }
        return OfflineCodingPreview(sha256(originalBytes), sha256(out), changes, out, profile.profileId)
    }
}
