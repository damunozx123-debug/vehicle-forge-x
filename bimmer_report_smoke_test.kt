import com.dan.bmwforgex.core.*

fun bimmerReportSmoke() {
    val sample = """
2026-10-11 19:28:32
5'/F10/2011/Auto / MSV90
------------------------------------------------------------
MOTOR (!)  [Engine]
  20A901   engine cooling system, reduced pump output
------------------------------------------------------------
ACC  [Adaptive cruise control]
  No response from ECU
------------------------------------------------------------
EGS  [Gearbox]
  No faults
------------------------------------------------------------
ICMQL (!)  [Integrated chassis management]
  D01A7D No description available
  D015CA No description available
  D01628 No description available
------------------------------------------------------------
JBE (!)  [Junction box]
  030195 No description available
------------------------------------------------------------
PDC (!)  [Park distance control]
  8011CE No description available
------------------------------------------------------------
ZGW (!)  [Central gateway]
  CD0425 No description available
------------------------------------------------------------
----- bimmer-tool 3.4.5-L -----
""".trimIndent()
    val report = BimmerReportParser.parse(sample)
    check(report.ecuFamily == "MSV90")
    check(report.date == "2026-10-11 19:28:32")
    check(report.faultCount == 7) { "Expected seven DTCs, got ${report.faultCount}" }
    check(report.faultModules == 5)
    check(report.noResponseCount == 1)
    check(report.cleanModules == 1)
    check(report.modules.any { it.ecu == "ICMQL" && it.faults.size == 3 })
    check(BmwFaultAdviceLibrary.describe("MOTOR", "20A901").verifiedMeaning)
    check(BmwFaultAdviceLibrary.describe("MOTOR", "20A901").severity == "HIGH")
    check(!BmwFaultAdviceLibrary.describe("PDC", "8011CE").verifiedMeaning)
    check(!BmwFaultAdviceLibrary.describe("ICMQL", "D01A7D").verifiedMeaning)
    check(BimmerReportParser.parse("empty scanner output").faultCount == 0)
    println("12 bimmer-tool import and safety assertions passed")
}
