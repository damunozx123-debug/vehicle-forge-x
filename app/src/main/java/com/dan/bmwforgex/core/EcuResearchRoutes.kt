package com.dan.bmwforgex.core

/**
 * Manufacturer-specific research pointers, NOT evidence this app supports
 * a particular vehicle ECU or its coding security mechanism.
 */
data class EcuResearchRoute(
    val platform: String,
    val documentedTools: String,
    val physicalInterface: String,
    val codingDataRequirement: String,
    val accessWarning: String,
    val primaryReference: String
)

object EcuResearchRoutes {
    fun forMake(make: String): EcuResearchRoute = when (make) {
        "BMW", "MINI" -> EcuResearchRoute(
            "BMW / MINI (chassis/era-specific)", "BMW AOS ISTA; older E-series toolchain differs",
            "ICOM or manufacturer-approved J2534; some coding workflows use ENET/K+DCAN",
            "Correct chassis, ECU hardware/software, FA/VO or legally sourced DATEN/CAFD mapping",
            "Current app has only SAE engine reads plus an unverified F10 feature catalogue",
            "https://bmwtechinfo.bmwgroup.com/assets/system_requirements.pdf")
        "Audi", "Volkswagen", "Skoda", "SEAT", "Cupra" -> EcuResearchRoute(
            "VW Group", "ODIS Service (official workshop route)",
            "Approved vehicle communication interface",
            "ODX/parameter definitions for exact ECU software; official licence where required",
            "SFD/SFD2 can restrict writes; no token unlock is implemented",
            "https://vwah.erwin-store.com/erwin/showCarDiagnostics.do")
        "Ford", "Mazda" -> EcuResearchRoute(
            "Ford / Mazda (platform-dependent)", "OEM service platform and documented as-built configurations",
            "Correct OEM-approved cable/pass-through interface for the generation",
            "Original as-built/configuration readout and legally obtained parameter definitions",
            "Do not copy generic Ford bit definitions onto another model or module",
            "https://saemobilus.sae.org/standards/j2534-1_5_00-recommended-practice-pass-thru-vehicle-programming")
        "Toyota" -> EcuResearchRoute(
            "Toyota (Lexus family varies)", "Toyota TIS / Techstream / Calibration Update Wizard",
            "Toyota-validated SAE J2534 interface for applicable reprogramming",
            "Paid TIS access and exact ECU software/calibration file where required",
            "A generic Bluetooth ELM327 does not provide official Toyota programming",
            "https://techinfo.toyota.com/techInfoPortal/appmanager/t3/ti?_nfpb=true&_pageLabel=ti_j2534_device")
        "Honda" -> EcuResearchRoute(
            "Honda (Acura family varies)", "Honda i-HDS",
            "J2534-compliant VCI and supported Windows software",
            "Subscription, ECU calibration and manufacturer repair procedure",
            "Immobilizer/security functions have additional authorisation requirements",
            "https://techinfo.honda.com/rjanisis/pubs/web/RJAAI001_J2534.htm")
        "Mercedes-Benz" -> EcuResearchRoute(
            "Mercedes-Benz", "OEM diagnostic/engineering tool and legitimate workshop data",
            "OEM-specified pass-through/interface varies by generation",
            "Exact module ID, dataset, validated options and authorised gateway access",
            "No vehicle-wide module scan or coding support in this app",
            "https://www.asam.net/standards/detail/mcd-2-d/")
        else -> EcuResearchRoute(
            "Other make / platform-dependent", "Confirm manufacturer service documentation",
            "OBD-II Bluetooth reader for generic engine faults; other functions may require specific hardware",
            "Exact ECU identity, legal data source and profile validated on a bench",
            "This make selection is for research planning, not confirmed coding support",
            "https://www.asam.net/standards/detail/mcd-2-d/")
    }
}
