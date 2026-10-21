package com.dan.bmwforgex.core

import java.util.Locale
import kotlin.math.roundToInt

/** Pure Kotlin protocol core. Deliberately no Android, network or vehicle-writing dependencies. */
object ObdCore {
    fun hexBytes(raw: String): List<Int> {
        val cleaned = raw.uppercase(Locale.ROOT).replace(Regex("SEARCHING\\.{0,3}|BUS INIT|STOPPED|NO DATA|UNABLE TO CONNECT|CAN ERROR|ERROR|\\?"), "")
        val result = mutableListOf<Int>()
        for (line in cleaned.split('\n', '\r', '>')) {
            val trimmed = line.trim().replace(Regex("^[0-9A-F]:\\s*"), "")
            if (trimmed.isEmpty()) continue
            val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
            for (token in tokens) {
                if (token.matches(Regex("[0-9A-F]{3}"))) continue // 11-bit CAN header
                if (token.matches(Regex("[0-9A-F]{8}")) && !token.startsWith("41") && !token.startsWith("43") && !token.startsWith("47") && !token.startsWith("49") && !token.startsWith("4A") && !token.startsWith("01")) continue // 29-bit CAN header
                if (!token.matches(Regex("[0-9A-F]+")) || token.length % 2 != 0) continue
                token.chunked(2).forEach { result += it.toInt(16) }
            }
        }
        return result
    }

    /** Works for both ATS0 compact ELM responses and spaced ELM responses, with optional headers. */
    fun payload(raw: String, mode: Int, pid: Int? = null): List<Int>? {
        val bytes = hexBytes(raw)
        val response = mode + 0x40
        for (i in bytes.indices) {
            if (bytes[i] != response) continue
            if (pid != null) {
                if (i + 1 >= bytes.size || bytes[i + 1] != pid) continue
                return bytes.drop(i + 2)
            }
            return bytes.drop(i + 1)
        }
        return null
    }

    fun supportedPids(raw: String, base: Int): Set<Int> {
        val data = payload(raw, 0x01, base)?.take(4) ?: return emptySet()
        if (data.size < 4) return emptySet()
        return (0 until 32).filter { offset ->
            (data[offset / 8] and (0x80 ushr (offset % 8))) != 0
        }.map { base + 1 + it }.toSet()
    }

    fun dtcs(raw: String, mode: Int = 0x03): List<String> {
        val response = mode + 0x40
        // Process ECU replies separately. Concatenating two CAN responses corrupts code pairing.
        return raw.split('\r', '\n', '>').flatMap { line ->
            val bytes = hexBytes(line)
            val start = bytes.indexOf(response)
            if (start < 0) emptyList() else bytes.drop(start + 1).chunked(2)
                .filter { it.size == 2 && (it[0] != 0 || it[1] != 0) }
                .map { pair ->
                    val first = pair[0]; val second = pair[1]
                    val family = "PCBU"[first ushr 6]
                    "${family}${(first ushr 4) and 3}${first and 0x0F}${second.toString(16).padStart(2, '0')}".uppercase(Locale.ROOT)
                }
        }.distinct()
    }

    /** Mode 09 VIN from typical ELM327 CAF responses; returns null if segmented data are incomplete. */
    fun vin(raw: String): String? {
        val lines = raw.uppercase(Locale.ROOT).split('\n', '\r', '>')
        val all = mutableListOf<Int>()
        for (line in lines) {
            val bytes = hexBytes(line)
            val marker = (0 until (bytes.size - 2)).firstOrNull { bytes[it] == 0x49 && bytes[it + 1] == 0x02 }
            if (marker != null) {
                all += bytes.drop(marker + 3) // skip 49 02 frame index
            } else if (all.isNotEmpty()) {
                // ELM ISO-TP continuation lines often start 21, 22 etc.
                val idx = bytes.indexOfFirst { it in 0x21..0x2F }
                if (idx >= 0) all += bytes.drop(idx + 1)
                else if (line.trim().matches(Regex("^[1-9A-F]:.*", RegexOption.IGNORE_CASE))) all += bytes
            }
        }
        val value = all.filter { it in 0x30..0x39 || it in 0x41..0x5A }
            .map(Int::toChar).joinToString("").take(17)
        return value.takeIf { it.length == 17 && it.all { ch -> ch !in "IOQ" } }
    }

    fun readiness(raw: String): Readiness? {
        val d = payload(raw, 0x01, 0x01) ?: return null
        if (d.size < 4) return null
        return Readiness(mil = d[0] and 0x80 != 0, storedCount = d[0] and 0x7F,
            sparkIgnition = d[1] and 0x08 == 0,
            continuousIncomplete = d[1] and 0x70,
            noncontinuousSupported = d[2], noncontinuousIncomplete = d[3])
    }
}

data class Readiness(val mil: Boolean, val storedCount: Int, val sparkIgnition: Boolean,
                     val continuousIncomplete: Int, val noncontinuousSupported: Int,
                     val noncontinuousIncomplete: Int)

data class Sensor(val pid: Int, val label: String, val unit: String, val low: Double, val high: Double,
                  val explanation: String, val calculate: (List<Int>) -> Double?) {
    val command: String get() = "01${pid.toString(16).padStart(2, '0')}".uppercase(Locale.ROOT)
    fun decode(raw: String): Double? = ObdCore.payload(raw, 0x01, pid)?.let(calculate)
}

object Sensors {
    private fun s(pid: Int, title: String, unit: String, min: Double, max: Double, hint: String,
                  fn: (List<Int>) -> Double?): Sensor = Sensor(pid, title, unit, min, max, hint, fn)
    private fun one(b: List<Int>) = b.getOrNull(0)?.toDouble()
    private fun pair(b: List<Int>) = if (b.size >= 2) (256 * b[0] + b[1]).toDouble() else null
    val all = listOf(
        s(0x04,"Engine load","%",0.0,100.0,"Calculated load is an ECU estimate, not a direct measurement of power.") { one(it)?.div(2.55) },
        s(0x05,"Coolant","°C",-40.0,160.0,"Coolant temperature helps assess thermostat operation and engine warm-up.") { one(it)?.minus(40) },
        s(0x06,"Short fuel trim","%",-100.0,100.0,"Fast oxygen-feedback correction. Positive means adding fuel; watch alongside long-term trim.") { one(it)?.let { v -> (v-128)*100/128 } },
        s(0x07,"Long fuel trim","%",-100.0,100.0,"Slow adaptation. Large positive values can be consistent with unmetered air or insufficient fuel delivery.") { one(it)?.let { v -> (v-128)*100/128 } },
        s(0x0B,"Intake pressure","kPa",0.0,255.0,"Absolute manifold pressure, NOT boost gauge pressure. Subtract ambient pressure to estimate boost where appropriate.") { one(it) },
        s(0x0C,"Engine RPM","rpm",0.0,8000.0,"Crankshaft speed. Compare against load, smoothness and measured fuel delivery.") { pair(it)?.div(4) },
        s(0x0D,"Vehicle speed","km/h",0.0,260.0,"Vehicle speed from ECU. Never interact with app while driving.") { one(it) },
        s(0x0E,"Ignition timing","°",-64.0,64.0,"Ignition advance relative to top dead centre; varies with load, fuel and ECU strategies.") { one(it)?.let { v -> v/2-64 } },
        s(0x0F,"Intake air","°C",-40.0,120.0,"Hot intake air can reduce power and influence ignition timing.") { one(it)?.minus(40) },
        s(0x10,"Air mass","g/s",0.0,400.0,"MAF reading supports air leak and airflow diagnostics; interpret with RPM and load.") { pair(it)?.div(100) },
        s(0x11,"Throttle","%",0.0,100.0,"Throttle plate signal can differ from accelerator pedal position.") { one(it)?.div(2.55) },
        s(0x1F,"Run time","s",0.0,100000.0,"Engine run time since start. Helpful for cold-start log alignment.") { pair(it) },
        s(0x23,"Fuel rail pressure","kPa",0.0,655350.0,"Direct-injection fuel rail reading if ECU exposes SAE PID 23. NOT available on every engine.") { pair(it)?.times(10) },
        s(0x2F,"Fuel level","%",0.0,100.0,"ECU estimated tank level, only if supported.") { one(it)?.div(2.55) },
        s(0x33,"Ambient pressure","kPa",0.0,255.0,"Useful reference for interpreting manifold absolute pressure.") { one(it) },
        s(0x42,"Control module voltage","V",0.0,20.0,"Voltage at the ECU rather than adapter connector; can reveal supply sag.") { pair(it)?.div(1000) },
        s(0x43,"Absolute load","%",0.0,260.0,"Absolute load can exceed 100% on boosted engines.") { pair(it)?.times(100)?.div(255) },
        s(0x44,"Fuel equivalence","φ",0.0,2.0,"Commanded fuel/air equivalence ratio (phi). 1 is stoichiometric; above 1 is richer, below 1 is leaner. Lambda is its reciprocal.") { pair(it)?.div(32768) },
        s(0x46,"Ambient air","°C",-40.0,100.0,"Outside-air temperature reported by ECU.") { one(it)?.minus(40) },
        s(0x49,"Pedal position","%",0.0,100.0,"Driver demand; throttle response and torque request are not identical.") { one(it)?.div(2.55) },
        s(0x5C,"Engine oil","°C",-40.0,210.0,"Oil temperature is especially useful on the F10; SAE PID 5C is optional.") { one(it)?.minus(40) },
        s(0x5E,"Fuel rate","L/h",0.0,200.0,"ECU reported fuel consumption if available.") { pair(it)?.div(20) }
    )
    val byPid = all.associateBy { it.pid }
    val priority = listOf(0x0C,0x05,0x5C,0x42,0x0B,0x10,0x06,0x07,0x0D,0x11,0x23,0x0F,0x49)
    fun format(value: Double?): String = if (value == null) "—" else when {
        value >= 100 || value <= -100 -> value.roundToInt().toString()
        else -> "%.1f".format(Locale.US, value)
    }
}
