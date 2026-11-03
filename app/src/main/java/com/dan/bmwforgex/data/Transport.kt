package com.dan.bmwforgex.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.dan.bmwforgex.core.ObdCore
import com.dan.bmwforgex.core.Sensors
import com.dan.bmwforgex.core.VehicleCommandPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.math.sin

interface ObdTransport {
    suspend fun open()
    suspend fun command(cmd: String, timeoutMs: Long = 2400L): String
    suspend fun close()
    val label: String
}

/** Android Classic Bluetooth Serial Port Profile. BLE-only adapters need a separate GATT transport. */
class VgateSppTransport(private val adapter: BluetoothAdapter, private val device: BluetoothDevice) : ObdTransport {
    override val label: String = device.name ?: device.address
    private var socket: BluetoothSocket? = null
    private val mutex = Mutex()
    @SuppressLint("MissingPermission")
    override suspend fun open() = withContext(Dispatchers.IO) {
        adapter.cancelDiscovery()
        val pending = device.createRfcommSocketToServiceRecord(
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        try {
            pending.connect()
            socket = pending
            listOf("ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0","ATCAF1","ATAT1","ATST64").forEachIndexed { index, cmd ->
                command(cmd, if (index == 0) 5000L else 2400L)
            }
            val response = command("0100", 5000)
            require(ObdCore.payload(response, 1, 0) != null) {
                "Adapter connected but no OBD-II ECU responded. Check vehicle ignition and protocol."
            }
        } catch (e: Exception) {
            runCatching { pending.close() }
            socket = null
            throw e
        }
    }

    override suspend fun command(cmd: String, timeoutMs: Long): String = mutex.withLock {
        require(VehicleCommandPolicy.allowedAdapterRequest(cmd)) {
            "Bluetooth OBD transport blocks unsupported or ECU-write commands"
        }
        withContext(Dispatchers.IO) {
            val s = socket ?: throw IOException("Bluetooth is not connected")
            val stream = s.inputStream
            // Stale bytes should not bleed into a new request.
            var drained = 0
            while (stream.available() > 0 && drained < 16384) { stream.read(); drained++ }
            s.outputStream.write((cmd.trim() + "\r").toByteArray(Charsets.US_ASCII))
            s.outputStream.flush()
            val started = System.nanoTime()
            val output = StringBuilder()
            while (true) {
                if ((System.nanoTime() - started) / 1_000_000 > timeoutMs) {
                    throw IOException("ELM timeout for $cmd after ${timeoutMs}ms; partial: ${output.take(70)}")
                }
                val available = stream.available()
                if (available == 0) { delay(12); continue }
                repeat(minOf(available, 512)) {
                    val b = stream.read()
                    if (b == -1) throw IOException("Adapter disconnected")
                    if (b == '>'.code) return@withContext output.toString()
                    output.append(b.toChar())
                    if (output.length > 16000) throw IOException("Response exceeds 16 KB")
                }
            }
            @Suppress("UNREACHABLE_CODE") ""
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching { socket?.close() }
            socket = null
        }
    }
}

/** Clearly marked deterministic simulation. Never interpreted as live vehicle data. */
class DemoTransport : ObdTransport {
    override val label = "SIMULATOR · no vehicle"
    private val began = System.nanoTime()
    private var connected = false
    private var counter = 0
    override suspend fun open() { connected = true }
    override suspend fun close() { connected = false }
    override suspend fun command(cmd: String, timeoutMs: Long): String {
        require(VehicleCommandPolicy.allowedAdapterRequest(cmd)) {
            "Simulator transport blocks unsupported or ECU-write commands"
        }
        check(connected) { "Simulator closed" }
        delay(22)
        counter++
        val t = (System.nanoTime() - began) / 1.0e9
        val current = 1600 + 640 * sin(t / 3.5)
        val temp = (72 + 16 * sin(t / 29)).toInt()
        val values = mapOf(
            0x04 to listOf(110), 0x05 to listOf(temp + 40), 0x06 to listOf(132), 0x07 to listOf(137),
            0x0B to listOf((100+12*sin(t/3)).toInt()),
            0x0C to listOf((current.toInt() * 4) / 256, (current.toInt() * 4) % 256),
            0x0D to listOf((43 + 11 * sin(t/7)).toInt()), 0x0E to listOf(146),
            0x0F to listOf(68), 0x10 to listOf(0x1A, 0xE0), 0x11 to listOf(65),
            0x1F to listOf(0, (counter / 10) % 256), 0x23 to listOf(0x12, 0x34),
            0x2F to listOf(192), 0x33 to listOf(101), 0x42 to listOf(0x37, 0x98),
            0x43 to listOf(1, 130), 0x44 to listOf(0x80, 0),
            0x46 to listOf(59), 0x49 to listOf(73), 0x5C to listOf(112), 0x5E to listOf(0, 0x7D)
        )
        val canonical = cmd.trim().uppercase()
        return when (canonical) {
            "ATI" -> "ELM327 DEMO v1.0\r>"
            "ATRV" -> "13.9V\r>"
            "ATDP" -> "ISO 15765-4 CAN (11 bit ID, 500 kbaud)\r>"
            "0100","0120","0140" -> {
                val base = canonical.substring(2).toInt(16)
                val bits = IntArray(4)
                Sensors.all.filter { it.pid in (base + 1)..(base + 32) }.forEach {
                    val offset = it.pid - base - 1
                    bits[offset / 8] = bits[offset / 8] or (0x80 ushr (offset % 8))
                }
                if (base < 0x40) bits[3] = bits[3] or 1 // next-range query
                "41${canonical.takeLast(2)}${bits.joinToString("") { "%02X".format(it) }}\r>"
            }
            "0101" -> "410100070000\r>"
            "03" -> "4301330000\r>" // demonstrative P0133
            "07", "0A" -> "${if (canonical == "07") "47" else "4A"}0000\r>"
            "0902" -> "4902015742414657313143304445303030303132\r>" // illustrative VIN, not registry data
            else -> {
                if (canonical.matches(Regex("02[0-9A-F]{2}00"))) {
                    val framePid = canonical.substring(2,4).toInt(16)
                    val valuesForPid = values[framePid]
                    if (valuesForPid != null) return "42${"%02X".format(framePid)}00${valuesForPid.joinToString("") { "%02X".format(it) }}\r>"
                }
                val pid = canonical.removePrefix("01").toIntOrNull(16)
                if (pid != null && pid in values)
                    "41${"%02X".format(pid)}${values[pid]!!.joinToString("") { "%02X".format(it.coerceIn(0,255)) }}\r>"
                else "NO DATA\r>"
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun bondedAdapters(context: Context): List<BluetoothDevice> {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return adapter.bondedDevices.sortedWith(compareBy({ !it.name.orEmpty().contains("link", true) }, { it.name.orEmpty() })).toList()
}

fun bluetoothAdapter(context: Context): BluetoothAdapter? =
    context.getSystemService(BluetoothManager::class.java)?.adapter
