package com.dan.bmwforgex.core

/** SAE J1979 Mode 02, freeze frame 0. This is a limited global-OBD snapshot. */
object FreezeFrame {
    fun command(pid: Int, frame: Int = 0): String = "02%02X%02X".format(pid, frame)
    fun decode(raw: String, pid: Int, frame: Int = 0): Double? {
        val bytes = ObdCore.payload(raw, 0x02, pid) ?: return null
        if (bytes.firstOrNull() != frame) return null
        return Sensors.byPid[pid]?.let { it.calculate(bytes.drop(1)) }
    }
}
