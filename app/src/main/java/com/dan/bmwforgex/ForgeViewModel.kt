package com.dan.bmwforgex

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan.bmwforgex.core.*
import com.dan.bmwforgex.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/** App holds only read-only vehicle protocol paths in v0.1. */
data class Datum(val value: Double, val atMs: Long)
data class Trace(val time: Long, val command: String, val response: String, val millis: Long)
data class Sample(val atMs: Long, val pid: Int, val reading: Double, val raw: String)
data class ForgeState(
    val state: String = "Offline", val simulator: Boolean = false, val connected: Boolean = false,
    val name: String = "Not connected", val error: String = "", val notice: String = "",
    val readings: Map<Int, Datum> = emptyMap(),
    val trends: Map<Int, List<Double>> = emptyMap(),
    val supported: Set<Int> = emptySet(),
    val dtcs: Map<String, List<String>> = emptyMap(),
    val freeze: Map<Int, Double> = emptyMap(),
    val readiness: Readiness? = null,
    val vin: String = "", val adapterInfo: String = "", val protocol: String = "",
    val battery: String = "—", val latency: Long = 0,
    val trace: List<Trace> = emptyList(),
    val plate: String = "", val model: String = "", val year: String = "", val engine: String = "Unknown",
    val lookUpStatus: String = "",
    val recording: Boolean = false, val recordCount: Int = 0,
    val busy: Boolean = false
)

class ForgeViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = app.getSharedPreferences("garage_local", 0)
    private val _ui = MutableStateFlow(ForgeState(
        plate = preferences.getString("plate", "") ?: "",
        model = preferences.getString("model", "") ?: "",
        year = preferences.getString("year", "") ?: "",
        engine = preferences.getString("engine", "Unknown") ?: "Unknown"
    ))
    val ui = _ui.asStateFlow()
    private var active: ObdTransport? = null
    private var polling: Job? = null
    private var connection: Job? = null
    private val recorded = mutableListOf<Sample>()
    private var pollingCycle = 0

    fun saveGarage(plate: String, model: String, year: String, engine: String) {
        val p = plate.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "").take(8)
        preferences.edit().putString("plate", p).putString("model", model.take(70))
            .putString("year", year.take(6)).putString("engine", engine.take(45)).apply()
        _ui.update { it.copy(plate = p, model = model, year = year, engine = engine,
            notice = "Vehicle profile saved on this device. Model not ECU-verified.") }
    }

    fun lookupPlate(plate: String, apiKey: String) = viewModelScope.launch {
        _ui.update { it.copy(lookUpStatus = "Querying CarJam basic vehicle record…") }
        try {
            val car = PlateLookup.basic(plate, apiKey)
            saveGarage(car.plate, "${car.make} ${car.model}", car.year, _ui.value.engine)
            _ui.update { it.copy(lookUpStatus = "CarJam: ${car.rawSummary}. Confirm against ECU VIN.") }
        } catch (e: Exception) {
            _ui.update { it.copy(lookUpStatus = "Lookup unavailable: ${e.message}") }
        }
    }

    fun startDemo() = connect(DemoTransport(), isDemo = true)
    fun connectDevice(device: BluetoothDevice) {
        val adapter = bluetoothAdapter(getApplication())
        if (adapter == null) { _ui.update { it.copy(error = "No Bluetooth adapter detected") }; return }
        connect(VgateSppTransport(adapter, device), isDemo = false)
    }

    private fun connect(transport: ObdTransport, isDemo: Boolean) {
        connection?.cancel()
        connection = viewModelScope.launch {
            polling?.cancel()
            runCatching { active?.close() }
            active = null
            _ui.value = _ui.value.copy(state = "Connecting", connected = false, simulator = isDemo,
                busy = true, error = "", notice = "", readings = emptyMap(), trends = emptyMap(),
                supported = emptySet(), dtcs = emptyMap(), freeze = emptyMap(), vin = "", recordCount = 0,
                trace = emptyList(), recording = false, name = transport.label)
            recorded.clear()
            try {
                transport.open()
                active = transport
                val adapterInfo = runCommand("ATI").trim().take(70)
                val protocol = runCommand("ATDP").trim().take(80)
                _ui.update { it.copy(state = if (isDemo) "DEMO" else "CONNECTED", connected = true,
                    busy = true, adapterInfo = adapterInfo, protocol = protocol) }
                // Standard PID bitmap discovery, not assumptions about engine support.
                val capabilities = mutableSetOf<Int>()
                for (base in listOf(0x00, 0x20, 0x40)) {
                    val response = runCommand("01%02X".format(base))
                    capabilities += ObdCore.supportedPids(response, base)
                    if (base + 0x20 !in capabilities) break
                }
                _ui.update { it.copy(supported = capabilities, notice = "${capabilities.size} SAE PIDs advertised by ECU") }
                val vin = ObdCore.vin(runCommand("0902"))
                if (vin != null) _ui.update { it.copy(vin = vin) }
                scanCodesInternal()
                _ui.update { it.copy(busy = false) }
                pollingCycle = 0
                startPolling()
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) {
                _ui.update { it.copy(state = "Offline", connected = false, busy = false,
                    error = "Connection failed: ${e.message}") }
                runCatching { transport.close() }
                active = null
            }
        }
    }

    private suspend fun runCommand(command: String): String {
        val transport = active ?: throw IllegalStateException("No OBD session")
        val t = System.nanoTime()
        val raw = transport.command(command)
        val ms = (System.nanoTime() - t) / 1_000_000
        val response = raw.take(2400).replace("\u0000", "")
        _ui.update { s -> s.copy(latency = ms,
            trace = (s.trace + Trace(System.currentTimeMillis(),command,response,ms)).takeLast(140)) }
        return raw
    }

    private fun startPolling() {
        polling?.cancel()
        polling = viewModelScope.launch {
            val supported = _ui.value.supported
            val seq = Sensors.priority.filter { it in supported && it in Sensors.byPid }
            if (seq.isEmpty()) { _ui.update { it.copy(notice = "No supported dashboard PIDs found. Check raw diagnostics.") }; return@launch }
            var slot = 0
            while (true) {
                val pid = if (slot % 3 == 0 && 0x0C in seq) 0x0C else seq[slot % seq.size]
                slot++
                try {
                    val sensor = Sensors.byPid.getValue(pid)
                    val raw = runCommand(sensor.command)
                    val reading = sensor.decode(raw)
                    if (reading != null) {
                        val now = System.currentTimeMillis()
                        _ui.update { s ->
                            s.copy(readings = s.readings + (pid to Datum(reading, now)),
                                trends = s.trends + (pid to ((s.trends[pid] ?: emptyList()) + reading).takeLast(55)))
                        }
                        if (_ui.value.recording && recorded.size < 100_000) {
                            recorded += Sample(now,pid,reading,raw.trim().take(200))
                            _ui.update { it.copy(recordCount = recorded.size) }
                        }
                    }
                    if (slot % 20 == 0) {
                        val voltage = runCommand("ATRV")
                        val match = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*V",RegexOption.IGNORE_CASE).find(voltage)
                        if (match != null) _ui.update { it.copy(battery = "${match.groupValues[1]} V") }
                    }
                    pollingCycle++
                    delay(if (_ui.value.simulator) 110 else 75)
                } catch (c: CancellationException) { throw c }
                catch (e: Exception) {
                    _ui.update { it.copy(error = "Polling: ${e.message}", state = "Error") }
                    delay(800)
                }
            }
        }
    }

    fun scanCodes() = viewModelScope.launch {
        if (active == null) return@launch
        polling?.cancel()
        _ui.update { it.copy(busy = true, error = "") }
        try { scanCodesInternal() }
        catch (e: Exception) { _ui.update { it.copy(error = "Scan failed: ${e.message}") } }
        finally { _ui.update { it.copy(busy = false) }; startPolling() }
    }

    fun scanFreezeFrame() = viewModelScope.launch {
        if (active == null) return@launch
        polling?.cancel()
        _ui.update { it.copy(busy = true, error = "") }
        val values = linkedMapOf<Int,Double>()
        try {
            for (pid in listOf(0x0C, 0x05, 0x0B, 0x0F, 0x10, 0x06, 0x07)) {
                val response = runCommand(FreezeFrame.command(pid))
                FreezeFrame.decode(response, pid)?.let { values[pid] = it }
            }
            _ui.update { it.copy(freeze = values, notice = "Mode 02 returned ${values.size} freeze-frame readings") }
        } catch (e: Exception) { _ui.update { it.copy(error = "Freeze frame failed: ${e.message}") } }
        finally { _ui.update { it.copy(busy = false) }; startPolling() }
    }

    private suspend fun scanCodesInternal() {
        val outputs = linkedMapOf<String,List<String>>()
        for ((cmd, caption, mode) in listOf(Triple("03","Stored",0x03),
                Triple("07","Pending",0x07),Triple("0A","Permanent",0x0A))) {
            outputs[caption] = ObdCore.dtcs(runCommand(cmd), mode)
        }
        val readiness = ObdCore.readiness(runCommand("0101"))
        _ui.update { it.copy(dtcs = outputs, readiness = readiness) }
    }

    fun readOnlyCommand(request: String) = viewModelScope.launch {
        val cmd = request.uppercase(Locale.ROOT).replace(" ", "")
        // The transport independently enforces its own allowlist as a second barrier.
        if (!VehicleCommandPolicy.allowedLabRead(cmd)) {
            _ui.update { it.copy(notice = "Blocked: command is outside read-only allowlist") }
            return@launch
        }
        if (active == null) { _ui.update { it.copy(notice = "Connect adapter or simulator first") }; return@launch }
        polling?.cancel()
        try { val raw = runCommand(cmd); _ui.update { it.copy(notice = "$cmd → ${raw.trim().take(240)}") } }
        catch (e: Exception) { _ui.update { it.copy(error = "Raw read: ${e.message}") } }
        finally { startPolling() }
    }

    fun toggleRecord() {
        if (!_ui.value.connected) { _ui.update { it.copy(notice = "Connect before recording") }; return }
        if (!_ui.value.recording) recorded.clear()
        _ui.update { it.copy(recording = !it.recording, recordCount = recorded.size,
            notice = if (!it.recording) "Recording PID samples locally" else "Recording stopped; export CSV from dashboard") }
    }

    fun csv(): String = buildString {
        appendLine("time_utc_epoch_ms,pid_hex,sensor,measurement,unit,raw_response")
        for (sample in recorded) {
            val s = Sensors.byPid[sample.pid] ?: continue
            val escaped = sample.raw.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
            appendLine("${sample.atMs},${"%02X".format(sample.pid)},\"${s.label}\",${sample.reading},${s.unit},\"$escaped\"")
        }
    }

    fun disconnect() {
        connection?.cancel()
        polling?.cancel()
        val old = active
        active = null
        _ui.update { it.copy(state = "Offline", connected = false, recording = false,
            notice = "Vehicle session ended. Local recorded data retained until next connection.") }
        viewModelScope.launch { runCatching { old?.close() } }
    }

    override fun onCleared() {
        polling?.cancel(); connection?.cancel()
        val transport = active
        active = null
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { runCatching { transport?.close() } }
        super.onCleared()
    }
}
