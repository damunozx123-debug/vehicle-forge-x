import com.dan.bmwforgex.core.*

fun diagnosticExportSmoke() {
    val out = DiagnosticExport.summary(mapOf("Stored" to listOf("P0301", "P0301")), null, true, 1780000000000L)
    check(out.contains("Stored faults (1):"))
    check(out.contains("SYNTHETIC DEMO"))
    check(!out.contains("12345678901234567"))
    check(UdsDtcDecoder.parsePayload("59 02 FF 12 34 56 09") ==
        UdsDtcResult.Report(0xFF, listOf(UdsDtcEntry(0x123456, 9)), false))
    check(UdsDtcDecoder.parsePayload("7F 19 31") == UdsDtcResult.Negative(0x31))
    check((UdsDtcDecoder.parsePayload("5902FF1234") as UdsDtcResult.Report).truncated)
    check(UdsDtcDecoder.parsePayload("FISH") == UdsDtcResult.Unrecognised)
}
