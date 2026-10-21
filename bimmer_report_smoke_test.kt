import com.dan.bmwforgex.core.*

fun bimmerReportSmoke() {
    val early = """
2026-10-11 19:04:08
5'/F10/2011/Auto / MSV90
MOTOR (!) [Engine]
20A901 engine cooling system, low-power operation
ACC [Adaptive cruise control]
No response from ECU
AIRBAG [Airbags]
No faults
CIC (!) [Car Infotainment Computer]
E1C448 No description available
FZD (!) [Functions centre roof]
801A3A No description available
801A4A No description available
ICMQL (!) [Integrated chassis management]
480114 No description available
D01A7D No description available
D015CA No description available
D01628 No description available
IHKA (!) [Air conditioning]
8011CE No description available
JBE (!) [Junction box]
030195 No description available
PDC [Park distance control]
No faults
REM (!) [Rear electronic module]
800FB7 No description available
800F92 No description available
ZGW (!) [Central gateway]
CD0425 No description available
""".trimIndent()
    val late = """
2026-10-11 19:28:32
5'/F10/2011/Auto / MSV90
MOTOR (!) [Engine]
20A901 coolant pump
CIC [Car Infotainment Computer]
No faults
FZD [Functions centre roof]
No faults
ICMQL (!) [Integrated chassis management]
D01A7D No description available
D015CA No description available
D01628 No description available
IHKA [Air conditioning]
No faults
JBE (!) [Junction box]
030195 No description available
PDC (!) [Park distance control]
8011CE No description available
REM [Rear electronic module]
No faults
ZGW (!) [Central gateway]
CD0425 No description available
""".trimIndent()
    val a = BimmerReportParser.parse(early)
    val b = BimmerReportParser.parse(late)
    check(a.ecuFamily == "MSV90")
    check(a.date == "2026-10-11 19:04:08")
    check(a.faultCount == 13 && a.faultModules == 8)
    check(a.noResponseCount == 1 && a.cleanModules == 2)
    check(b.faultCount == 7 && b.faultModules == 5)
    val delta = BimmerScanComparator.compare(a,b)
    check(delta.retained.size == 6) { "Expected 6 persistent module-specific faults" }
    check(delta.disappeared.size == 6) { "Expected 6 disappeared faults" }
    check(delta.appeared.isEmpty()) { "No genuinely new codes expected" }
    check(delta.attributionChanged == listOf(ModuleMovedCode("8011CE","IHKA","PDC")))
    check(delta.retained.any { it.module == "MOTOR" && it.code == "20A901" })
    check(delta.disappeared.any { it.module == "CIC" && it.code == "E1C448" })
    check(delta.disappeared.count { it.module == "REM" } == 2)
    check(BmwFaultAdviceLibrary.describe("MOTOR","20A901").severity == "HIGH")
    check(!BmwFaultAdviceLibrary.describe("PDC","8011CE").verifiedMeaning)
    check(BimmerReportParser.parse("empty").faultCount == 0)
    println("15 bimmer-tool two-snapshot comparison and safety assertions passed")
}
