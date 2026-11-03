package com.dan.bmwforgex.core

import java.util.Locale

/**
 * Read-only protocol boundary for the existing ELM327 Bluetooth transport.
 * Future ECU coding must use a separate audited physical transport, never
 * broaden this allowlist to pass arbitrary UDS/KWP/raw AT commands.
 */
object VehicleCommandPolicy {
    private val standardReads = Regex("(?:01[0-9A-F]{2}|02[0-9A-F]{2}00|03|07|0A|0902|0904|0906)")
    private val labReads = Regex("(?:01[0-9A-F]{2}|02[0-9A-F]{2}00|03|07|0A|0902|0904|0906|ATI|ATRV|ATDP)")
    private val internalAdapter = setOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0",
        "ATSP0", "ATCAF1", "ATAT1", "ATST64", "ATI", "ATRV", "ATDP")

    fun canonical(input: String): String =
        input.uppercase(Locale.ROOT).replace(Regex("\\s+"), "")

    /** Commands explicitly offered by the user's Lab screen. */
    fun allowedLabRead(raw: String): Boolean =
        canonical(raw).let { it.length <= 10 && labReads.matches(it) }

    /** Additional private ELM init commands plus all normal user SAE reads. */
    fun allowedAdapterRequest(raw: String): Boolean {
        val cmd = canonical(raw)
        return cmd.length <= 10 && (standardReads.matches(cmd) || cmd in internalAdapter)
    }
}
