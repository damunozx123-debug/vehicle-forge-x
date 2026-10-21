package com.dan.bmwforgex.core

data class Insight(val level: String, val label: String, val explanation: String)

/** Contextual, conservative suggestions. Not an automatic diagnosis. */
object Insights {
    fun from(readings: Map<Int, Double>, readiness: Readiness? = null): List<Insight> {
        val results = mutableListOf<Insight>()
        val rpm = readings[0x0C]
        val voltage = readings[0x42]
        val coolant = readings[0x05]
        val oil = readings[0x5C]
        val map = readings[0x0B]
        val baro = readings[0x33]
        val short = readings[0x06]
        val long = readings[0x07]
        if (voltage != null && rpm != null && rpm > 650 && voltage < 12.2)
            results += Insight("Check", "Charging voltage appears low", "The ECU reports ${Sensors.format(voltage)} V with engine running. Confirm voltage using a meter before diagnosing alternator, belt, battery or grounds.")
        if (coolant != null && coolant > 112)
            results += Insight("Urgent", "Elevated coolant temperature", "ECU reports ${Sensors.format(coolant)}°C. Check for overheating symptoms and avoid sustained load. Temperature thresholds vary by engine and operating mode.")
        if (oil != null && oil > 135)
            results += Insight("Check", "High oil temperature", "Oil is ${Sensors.format(oil)}°C. Verify measurement and operating conditions. Do not interpret as a specific component fault.")
        if (short != null && long != null && short + long > 18)
            results += Insight("Investigate", "Positive combined fuel correction", "Short + long fuel trims are ${Sensors.format(short + long)}%. Compare at idle and steady cruise. Intake leaks, MAF plausibility and fuel supply are possible checks, not confirmed causes.")
        if (map != null && baro != null && rpm != null && rpm > 1200 && map - baro > 35)
            results += Insight("Info", "Positive manifold gauge pressure", "MAP minus ambient pressure is approximately ${Sensors.format(map-baro)} kPa. The result is a rough estimate, not a commanded-vs-actual turbo diagnostic.")
        if (readiness?.mil == true)
            results += Insight("Investigate", "Malfunction indicator reported", "ECU says the MIL is on. Read stored and pending faults and capture operating conditions before repairs.")
        if (results.isEmpty()) results += Insight("Info", "No simple threshold flags", "This does not prove the engine is healthy. Monitor trends and interpret conditions with the vehicle's actual engine variant.")
        return results
    }
}
