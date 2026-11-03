package com.dan.bmwforgex.core

import java.util.Locale

/** Free tier: produce a privacy-conscious summary of generic SAE OBD-II DTCs.
 * This is not a manufacturer-wide scan, a repair verdict, or a change to ECU state.
 */
object DiagnosticExport {
    private val groups = listOf("Stored", "Pending", "Permanent")
    private val codePattern = Regex("[PCBU][0-3][0-9A-F]{3}")

    fun summary(
        codes: Map<String, List<String>>,
        readiness: Readiness?,
        simulator: Boolean,
        recordedAtMs: Long
    ): String = buildString {
        appendLine("VEHICLE FORGE X - GENERIC OBD-II FAULT REPORT")
        appendLine(if (simulator) "SOURCE: SYNTHETIC DEMO - NOT VEHICLE DATA" else "SOURCE: OBD-II ENGINE/EMISSIONS SESSION")
        appendLine("Timestamp UTC (epoch ms): $recordedAtMs")
        appendLine("Scope: SAE 03/07/0A only. Not an all-module ECU scan.")
        appendLine("Vehicle VIN, plate, adapter MAC and raw transcripts intentionally excluded.")
        if (readiness == null) appendLine("Readiness: unavailable")
        else {
            appendLine("MIL: ${if (readiness.mil) "ON" else "OFF"}")
            appendLine("ECU-reported DTC count: ${readiness.storedCount}")
            appendLine("Incomplete monitor flags: ${readiness.continuousIncomplete}/${readiness.noncontinuousIncomplete}")
        }
        groups.forEach { group ->
            val entries = codes[group].orEmpty().map { it.uppercase(Locale.ROOT) }
                .filter { codePattern.matches(it) }.distinct().sorted()
            appendLine("$group faults (${entries.size}):")
            if (entries.isEmpty()) appendLine("  None reported in this session")
            entries.forEach { code ->
                val info = FaultKnowledge.forCode(code)
                appendLine("  $code | ${info.title} | ${info.severity}")
                appendLine("    First checks: ${info.firstChecks}")
            }
        }
        appendLine("Never clear a code merely to conceal a fault. Confirm repairs with fresh scans.")
    }
}

/**
 * Offline decoding of *already reassembled* ISO 14229 0x19 0x02 responses.
 * Not a CAN/ISO-TP driver and never initiates an ECU command.
 *
 * Positive payload: 59 02 <DTCStatusAvailabilityMask> (<DTC 3 bytes> <status byte>)*.
 */
data class UdsDtcEntry(val dtc: Int, val status: Int) {
    val hex: String get() = "%06X".format(Locale.ROOT, dtc)
    val active: Boolean get() = status and 0x01 != 0
    val pending: Boolean get() = status and 0x04 != 0
    val confirmed: Boolean get() = status and 0x08 != 0
}

sealed class UdsDtcResult {
    data class Report(
        val availableStatusMask: Int,
        val dtcs: List<UdsDtcEntry>,
        val truncated: Boolean
    ) : UdsDtcResult()
    data class Negative(val code: Int) : UdsDtcResult()
    data object Unrecognised : UdsDtcResult()
}

object UdsDtcDecoder {
    fun parsePayload(raw: String): UdsDtcResult {
        // Bounded and explicit: the provided text must be a complete UDS application
        // payload, NOT an unassembled multi-frame CAN dump.
        if (raw.length > 8192) return UdsDtcResult.Unrecognised
        val normalised = raw.trim().replace(Regex("\\s+"), "")
        if (normalised.isEmpty() || normalised.length % 2 != 0 ||
            !normalised.matches(Regex("[0-9a-fA-F]+"))) return UdsDtcResult.Unrecognised
        val bytes = normalised.chunked(2).map { it.toInt(16) }
        if (bytes.size >= 3 && bytes[0] == 0x7F && bytes[1] == 0x19) {
            return UdsDtcResult.Negative(bytes[2])
        }
        if (bytes.size < 3 || bytes[0] != 0x59 || bytes[1] != 0x02)
            return UdsDtcResult.Unrecognised
        val entries = bytes.drop(3)
        val dtcs = entries.chunked(4).filter { it.size == 4 }
            .map { d -> UdsDtcEntry(d[0] shl 16 or (d[1] shl 8) or d[2], d[3]) }
        return UdsDtcResult.Report(bytes[2], dtcs, entries.size % 4 != 0)
    }
}
