package com.dan.bmwforgex.core

data class FaultInfo(val title: String, val severity: String, val meaning: String,
                     val firstChecks: String, val caution: String = "Treat the code as a diagnostic clue, not proof that a particular part failed.")
object FaultKnowledge {
    private val records = mapOf(
        "P0012" to FaultInfo("Intake cam timing retarded", "Investigate", "Commanded and actual intake cam timing disagree.", "Verify oil level/condition, cam timing data and relevant solenoids. On applicable BMW engines investigate VANOS faults."),
        "P0015" to FaultInfo("Exhaust cam timing retarded", "Investigate", "Exhaust camshaft timing is more retarded than expected.", "Oil level/viscosity, timing actuators, cam sensors and supply pressure."),
        "P0087" to FaultInfo("Fuel rail pressure too low", "Urgent", "Fuel delivery pressure is below the commanded target under operating conditions.", "Record rail pressure during the event; inspect feed pump, filter, fuel quality, supply and sensor readings.", "Loss of power or stalling can occur. Avoid heavy load until diagnosed."),
        "P0101" to FaultInfo("Mass air flow range/performance", "Investigate", "Airflow does not agree with the ECU's expectation.", "Look for intake leaks, air filter restrictions and MAF contamination; compare MAP, RPM, load."),
        "P0113" to FaultInfo("Intake air temperature circuit high", "Investigate", "The sensor signal appears electrically high, often interpreted as implausibly cold air.", "Check connector, wiring and IAT reading against ambient temperature."),
        "P0130" to FaultInfo("Oxygen sensor circuit bank 1 sensor 1", "Investigate", "Front oxygen feedback is outside the expected electrical range.", "Check exhaust leaks, sensor heater, wiring and mixture before replacing a sensor."),
        "P0171" to FaultInfo("System too lean, bank 1", "Investigate", "The ECU has reached its correction limit while trying to add fuel.", "Inspect unmetered air/vacuum leaks, PCV, intake boots, MAF plausibility, low-pressure fuel and trim at idle vs load."),
        "P0174" to FaultInfo("System too lean, bank 2", "Investigate", "Bank 2 mixture requires excessive positive fuel correction.", "Compare both banks, intake leaks, PCV and fuel pressure."),
        "P0299" to FaultInfo("Turbo underboost", "Urgent", "Measured boost is lower than desired during turbo operation.", "Compare MAP with ambient, inspect charge pipes/intercooler joints, boost control, vacuum and wastegate operation.", "Avoid hard acceleration when power is limited or there is unusual turbo noise."),
        "P0300" to FaultInfo("Random or multiple misfire", "Urgent", "Misfires are detected across multiple cylinders.", "Review freeze frame, coil/plug condition for petrol engines, injector and fuel supply data, compression and intake leaks.", "A flashing check-engine light can indicate catalyst-damaging misfires. Stop driving safely."),
        "P0301" to FaultInfo("Cylinder 1 misfire", "Urgent", "Combustion stability is poor in cylinder 1.", "Check ignition components on petrol engines, injector contribution, compression and freeze-frame conditions."),
        "P0302" to FaultInfo("Cylinder 2 misfire", "Urgent", "Combustion stability is poor in cylinder 2.", "Check ignition/fuel delivery, compression and relevant wiring."),
        "P0303" to FaultInfo("Cylinder 3 misfire", "Urgent", "Combustion stability is poor in cylinder 3.", "Check ignition/fuel delivery, compression and relevant wiring."),
        "P0304" to FaultInfo("Cylinder 4 misfire", "Urgent", "Combustion stability is poor in cylinder 4.", "Check ignition/fuel delivery, compression and relevant wiring."),
        "P0305" to FaultInfo("Cylinder 5 misfire", "Urgent", "Combustion stability is poor in cylinder 5.", "Check ignition/fuel delivery, compression and relevant wiring."),
        "P0306" to FaultInfo("Cylinder 6 misfire", "Urgent", "Combustion stability is poor in cylinder 6.", "Check ignition/fuel delivery, compression and relevant wiring."),
        "P0420" to FaultInfo("Catalyst efficiency below threshold", "Investigate", "Post-catalyst oxygen observations suggest low catalyst efficiency.", "Resolve misfires/mixture faults first, check exhaust leaks and sensor performance before catalyst replacement."),
        "P0401" to FaultInfo("EGR flow insufficient", "Investigate", "Exhaust gas recirculation flow appears lower than requested.", "Inspect EGR valve, passages, command vs feedback and intake deposits; configuration varies by engine."),
        "P0442" to FaultInfo("Small evaporative leak", "Monitor", "EVAP system detected a small leak.", "Check fuel cap seal, tank vent and purge plumbing; verify with an appropriate smoke test."),
        "P0500" to FaultInfo("Vehicle speed sensor", "Investigate", "Vehicle speed information is missing or implausible.", "Inspect wheel speed and vehicle speed live data and ABS-related faults."),
        "P0562" to FaultInfo("System voltage low", "Urgent", "Measured electrical supply fell below the expected range.", "Compare battery at rest, alternator output, grounds, battery current sensor and voltage during start."),
        "P2002" to FaultInfo("Diesel particulate filter efficiency", "Investigate", "DPF filtering performance is considered below expected threshold.", "Check differential pressure, exhaust temperature and repeated interrupted regenerations with manufacturer tools.", "Do not force regeneration without confirming exhaust and fire safety conditions."),
        "P2453" to FaultInfo("DPF pressure sensor plausibility", "Investigate", "Reported differential pressure is inconsistent with expected behaviour.", "Inspect pressure hoses, soot blockages, wiring and sensor plausibility."),
        "U0100" to FaultInfo("Lost communication with engine ECU", "Urgent", "Another control unit reports a missing engine ECU message.", "Check battery/charging voltage, bus communications, grounds and corresponding module histories."),
        "U0121" to FaultInfo("Lost communication with ABS/DSC", "Urgent", "Messages from brake stability control are missing.", "Check DSC supply, CAN integrity, related network faults and module power."),
        "P0133" to FaultInfo("Oxygen sensor slow response", "Investigate", "The upstream oxygen sensor reacts slower than the ECU expects.", "Check exhaust leaks, aging sensor, wiring and mixture control behaviour.")
    )
    fun forCode(code: String): FaultInfo = records[code.uppercase()] ?: FaultInfo(
        "Manufacturer/standard fault ${code.uppercase()}", "Identify",
        "This code is not in the bundled offline knowledge base. Its exact meaning may depend on manufacturer and ECU.",
        "Retrieve the ECU identifier, freeze-frame details and a manufacturer-specific definition before diagnosing."
    )
}

object BmwF10 {
    val overview = "BMW F10 sedan (F11 touring) spans multiple petrol/diesel powertrains. Confirm year, VIN, engine and option codes before using engine-specific advice."
    val modules = listOf(
        "DME / DDE" to "Engine control: combustion, boost, fuel and emissions",
        "EGS" to "Automatic transmission: gearbox faults, adaptation and fluid temperature",
        "DSC" to "Braking / stability: wheel speeds and ABS faults",
        "CAS" to "Car Access System: start authorization and access",
        "ZGW" to "Central gateway for vehicle networks",
        "KOMBI" to "Instrument cluster and check-control messages",
        "HU_CIC / HU_NBT" to "iDrive head unit depends on year/options",
        "FRM / JBE" to "Body electronics and lighting functions, configuration dependent",
        "ACSM" to "Airbag and restraint system",
        "ICM / EPS" to "Chassis motion and steering, options dependent"
    )
    val workflows = listOf(
        "Cold start stumble" to "Log module voltage, RPM, fuel trims, coolant, intake temperature and rail pressure if supported. Compare cold and warm runs.",
        "Turbo response" to "Log MAP, ambient pressure, RPM, throttle and load. MAP is ABSOLUTE pressure and cannot alone determine boost control fault.",
        "Diesel DPF concern" to "Read standard emissions faults and readiness. Soot mass, differential pressure and regeneration status require validated BMW-specific DIDs.",
        "Battery change" to "Battery registration and capacity/type adaptation require verified BMW protocol support; NOT the same as clearing codes.",
        "Transmission rough shift" to "Check power supply and generic faults first. EGS module DTCs/adaptations require advanced BMW diagnostics over appropriate interface."
    )
    val roadmap = listOf(
        "BMW-wide ECU inventory over ENET with gateway discovery",
        "UDS 0x19 DTC reporting, statuses and freeze frame per ECU",
        "Validated live DIDs per exact ECU software version",
        "Battery registration with preconditions and vehicle backup",
        "Service resets by supported ECU variants",
        "FDL configuration diff, backups and reversible coding",
        "Battery charging verification and ignition-state interlocks for all writes",
        "Active tests, calibrations and adaptations only after vehicle-specific validation"
    )
}
