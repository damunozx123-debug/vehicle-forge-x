package com.dan.bmwforgex.core

/**
 * Offline, read-only parser for the text exported by bimmer-tool.
 * Import never executes diagnostic commands or writes to a connected vehicle.
 */
enum class ModuleResult { FAULTS, CLEAR, NO_RESPONSE, UNKNOWN }

data class ModuleScan(
    val ecu: String,
    val description: String,
    val result: ModuleResult,
    val faults: List<ReportedFault> = emptyList()
)
data class ReportedFault(val code: String, val scannerDescription: String)
data class BimmerReport(
    val date: String?,
    val vehicle: String?,
    val ecuFamily: String?,
    val modules: List<ModuleScan>
) {
    val faultCount: Int get() = modules.sumOf { it.faults.size }
    val faultModules: Int get() = modules.count { it.faults.isNotEmpty() }
    val noResponseCount: Int get() = modules.count { it.result == ModuleResult.NO_RESPONSE }
    val cleanModules: Int get() = modules.count { it.result == ModuleResult.CLEAR }
}

object BimmerReportParser {
    private val moduleHeader = Regex("""^([A-Z][A-Z0-9_-]*)\s*(?:\(!\))?\s*\[([^]]+)]\s*$""")
    private val hexFault = Regex("""^([0-9A-Fa-f]{6})(?:\s+(.+))?$""")
    private val stamped = Regex("""^\d{4}-\d\d-\d\d\s+\d\d:\d\d:\d\d$""")

    fun parse(raw: String): BimmerReport {
        require(raw.length <= 120_000) { "Report is too large (120 KB limit)" }
        var date: String? = null
        var vehicle: String? = null
        var ecuFamily: String? = null
        val modules = mutableListOf<ModuleScan>()
        var ecu: String? = null
        var description = ""
        var status = ModuleResult.UNKNOWN
        val codes = mutableListOf<ReportedFault>()

        fun flush() {
            if (ecu == null) return
            val result = if (codes.isNotEmpty()) ModuleResult.FAULTS else status
            modules += ModuleScan(ecu!!, description, result, codes.toList())
            codes.clear()
            ecu = null
            status = ModuleResult.UNKNOWN
        }

        for (line in raw.lineSequence()) {
            val cleaned = line.trim()
            if (cleaned.isEmpty() || cleaned.matches(Regex("[-]{4,}"))) continue
            if (date == null && stamped.matches(cleaned)) {
                date = cleaned
                continue
            }
            if (ecu == null && cleaned.contains("/F10/")) {
                vehicle = cleaned
                ecuFamily = cleaned.substringAfterLast('/').trim().takeIf { it.isNotBlank() }
                continue
            }
            val header = moduleHeader.matchEntire(cleaned)
            if (header != null) {
                flush()
                ecu = header.groupValues[1]
                description = header.groupValues[2]
                continue
            }
            if (ecu == null) continue
            when {
                cleaned.equals("No faults", true) -> status = ModuleResult.CLEAR
                cleaned.equals("No response from ECU", true) -> status = ModuleResult.NO_RESPONSE
                else -> {
                    val fault = hexFault.matchEntire(cleaned)
                    if (fault != null) codes += ReportedFault(
                        fault.groupValues[1].uppercase(),
                        fault.groupValues.getOrElse(2) { "" }
                    )
                }
            }
        }
        flush()
        return BimmerReport(date, vehicle, ecuFamily, modules)
    }
}

/**
 * Consciously module-scoped. The same numeric value may mean different things
 * in different BMW software variants. Do not silently reassign it.
 */
data class BmwFaultAdvice(
    val headline: String,
    val severity: String,
    val explanation: String,
    val checks: String,
    val verifiedMeaning: Boolean
)
object BmwFaultAdviceLibrary {
    fun describe(ecu: String, code: String): BmwFaultAdvice {
        val module = ecu.uppercase()
        return when {
            module in setOf("MOTOR", "DME", "DDE") && code == "20A901" ->
                BmwFaultAdvice("Electric coolant pump: dry-running/cavitation detected", "HIGH",
                    "The electric coolant pump reported cavitation, potentially from low coolant or trapped air. This does not by itself prove the pump must be replaced.",
                    "With engine cold, check coolant level and leaks. Inspect hoses and pump; have the circuit filled/bled using the correct BMW procedure. If warnings or overheating occur, stop driving. Do not open a hot cooling system.",
                    true)
            code == "8011CE" && module == "PDC" ->
                BmwFaultAdvice("Definition conflict: scanner says PDC", "VERIFY",
                    "This code is documented against another BMW ECU software variant (IHKA). The report associates it with PDC, so assigning a parking-sensor failure is unjustified.",
                    "Retrieve ECU software variant and original fault text with BMW ISTA or a validated module database. Check parking sensors only if supported by additional evidence.",
                    false)
            code == "030195" && module == "JBE" ->
                BmwFaultAdvice("Module-specific meaning not confirmed", "VERIFY",
                    "Some F-series documentation associates 030195 with window normalisation, but the referenced software/module is different from this report's JBE.",
                    "Check whether any window one-touch functions are inoperative, then verify exact JBE software and fault definition before adjusting components.",
                    false)
            else ->
                BmwFaultAdvice("BMW-specific code requires ECU variant", "UNKNOWN",
                    "No trustworthy module-specific definition is included in this offline build. A 6-digit code is not the same as a generic SAE P-code.",
                    "Retrieve original fault text, ECU variant, status (present/stored) and environmental conditions. Do not replace components solely from an unidentified code.",
                    false)
        }
    }
}
