package com.dan.bmwforgex

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dan.bmwforgex.core.*
import com.dan.bmwforgex.data.bondedAdapters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val Background = Color(0xFF07111D)
private val Surface = Color(0xFF102033)
private val Surface2 = Color(0xFF162A40)
private val TextMain = Color(0xFFE8F2FF)
private val SubText = Color(0xFF9BB1C7)
private val Aqua = Color(0xFF45CCEA)
private val Lime = Color(0xFF99E6BB)
private val Amber = Color(0xFFFFC57E)
private val Danger = Color(0xFFFF8790)

class MainActivity : ComponentActivity() {
    private lateinit var model: ForgeViewModel
    private var pendingCsv = ""
    private val saveCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val saved = runCatching {
                requireNotNull(contentResolver.openOutputStream(uri)) { "Cannot open export destination" }
                    .use { it.write(pendingCsv.toByteArray(Charsets.UTF_8)) }
            }
            android.widget.Toast.makeText(this,
                if (saved.isSuccess) "Diagnostic CSV exported" else "Export failed: ${saved.exceptionOrNull()?.message}",
                android.widget.Toast.LENGTH_LONG).show()
            pendingCsv = ""
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[ForgeViewModel::class.java]
        window.statusBarColor = android.graphics.Color.rgb(7, 17, 29)
        window.navigationBarColor = android.graphics.Color.rgb(7, 17, 29)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Aqua, secondary = Lime,
                background = Background, surface = Surface, onSurface = TextMain, onBackground = TextMain)) {
                ForgeApp(model) {
                    pendingCsv = model.csv()
                    saveCsv.launch("bmw-forge-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.csv")
                }
            }
        }
    }
}

@Composable
private fun ForgeApp(vm: ForgeViewModel, export: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BMW FORGE", fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                Text("X  /  VEHICLE INTELLIGENCE", fontSize = 10.sp, color = Aqua, letterSpacing = 1.7.sp)
            }
            StatusPill(if (ui.simulator && ui.connected) "SIMULATOR" else ui.state.uppercase(),
                if (!ui.connected) SubText else if (ui.simulator) Amber else Lime)
        }
        if (ui.error.isNotBlank()) Banner(ui.error, Danger)
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> Dashboard(ui, vm, export)
                1 -> Diagnostics(ui, vm)
                2 -> Garage(ui, vm)
                3 -> CodingWorkspace(ui)
                else -> Engineering(ui, vm)
            }
        }
        NavigationBar(containerColor = Surface, contentColor = TextMain) {
            listOf("◉" to "Live", "⚠" to "Faults", "▣" to "Garage", "◇" to "Coding", "⌘" to "Lab").forEachIndexed { i, entry ->
                NavigationBarItem(selected = i == tab, onClick = { tab = i },
                    icon = { Text(entry.first, fontSize = 21.sp) }, label = { Text(entry.second) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Aqua,
                        selectedTextColor = Aqua, unselectedTextColor = SubText,
                        indicatorColor = Surface2, unselectedIconColor = SubText))
            }
        }
    }
}

@Composable
private fun Dashboard(ui: ForgeState, vm: ForgeViewModel, export: () -> Unit) {
    val ids = Sensors.priority.distinct().filter { it in ui.supported }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp,end = 16.dp,bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("LIVE ENGINE", "Adaptive polling • ${ui.latency} ms response")
            HeroRpm(ui)
        }
        item { Connection(ui, vm) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Action(if (ui.recording) "■ Stop record" else "● Record CSV", Modifier.weight(1f),
                    enabled = ui.connected, primary = ui.recording, onClick = vm::toggleRecord)
                Action("⇩ Export (${ui.recordCount})", Modifier.weight(1f),
                    enabled = ui.recordCount > 0, onClick = export)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallFeature("POWER", ui.battery, Modifier.weight(1f))
                SmallFeature("ECU PIDs", ui.supported.size.toString(), Modifier.weight(1f))
                SmallFeature("PROTOCOL", if (ui.connected) "OBD-II" else "—", Modifier.weight(1f))
            }
        }
        if (ui.connected) item {
            SectionTitle("DIAGNOSTIC INTELLIGENCE", "Conservative, on-device pattern checks")
            val recent = ui.readings.mapValues { it.value.value }
            Insights.from(recent, ui.readiness).take(3).forEach { insight ->
                Panel(Modifier.padding(bottom=7.dp)) {
                    Text("${insight.level.uppercase()} · ${insight.label}",
                        color=if (insight.level=="Urgent") Danger else Lime,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(insight.explanation,fontSize=11.sp,color=SubText)
                }
            }
        }
        item { SectionTitle("SENSOR MATRIX", if (ui.connected) "Only ECU-advertised sensors" else "Connect to discover actual PIDs") }
        if (ids.isEmpty()) item { InfoCard("NO LIVE PID DATA", "Connect a paired Vgate adapter or start the simulator. Unsupported sensors are never displayed as real readings.") }
        items(ids.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (id in pair) MetricCard(Sensors.byPid.getValue(id), ui, Modifier.weight(1f))
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            if (ui.notice.isNotBlank()) Banner(ui.notice, Aqua)
            InfoCard("READING THE DATA", "Samples are polled sequentially through one ELM327 command channel. Graphs show recent samples, not simultaneous high-speed CAN captures. This version never writes to a vehicle ECU.")
        }
    }
}

@Composable
private fun HeroRpm(ui: ForgeState) {
    val value = ui.readings[0x0C]?.value
    val fraction = ((value ?: 0.0) / 7000).toFloat().coerceIn(0f,1f)
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(185.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(color = Color(0xFF294158), startAngle = 145f, sweepAngle = 250f,
                        useCenter = false, style = stroke)
                    drawArc(color = Aqua, startAngle = 145f, sweepAngle = 250f * fraction,
                        useCenter = false, style = stroke)
                    drawArc(color = Amber.copy(alpha = .65f), startAngle = 145f + 250f * .82f,
                        sweepAngle = 250f * .18f, useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Sensors.format(value), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("RPM", fontSize = 13.sp, letterSpacing = 2.sp, color = Aqua)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("ENGINE STATUS", color = SubText, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text(if (!ui.connected) "OFFLINE" else if (ui.simulator) "DEMO DATA" else "LIVE ECU",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (ui.connected && !ui.simulator) Lime else Amber)
                Spacer(Modifier.height(14.dp))
                Text("Coolant", color = SubText, fontSize = 12.sp)
                Text("${Sensors.format(ui.readings[0x05]?.value)} °C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Engine oil", color = SubText, fontSize = 12.sp)
                Text("${Sensors.format(ui.readings[0x5C]?.value)} °C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Connection(ui: ForgeState, vm: ForgeViewModel) {
    val ctx = LocalContext.current
    var paired by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    var expanded by remember { mutableStateOf(false) }
    var permissionNote by remember { mutableStateOf("") }
    val nearbyGranted = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    val allowed = { Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionNote = if (granted) "Permission granted. Refresh paired devices." else "Bluetooth permission required for Vgate access."
        if (granted) paired = runCatching { bondedAdapters(ctx) }.getOrDefault(emptyList())
    }
    Panel {
        Text("ADAPTER LINK", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ui.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.protocol.ifBlank { "Vgate / ELM327 Bluetooth Classic (SPP)" },
                    color = SubText, fontSize = 11.sp, maxLines = 2)
            }
            if (ui.connected) OutlinedButton(onClick = vm::disconnect) { Text("Disconnect",fontSize=12.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Nearby devices permission: " + if (nearbyGranted) "Granted" else "Not granted",
            color = if (nearbyGranted) Lime else Amber, fontSize = 11.sp)
        if (!nearbyGranted) {
            TextButton(onClick = { permission.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                Text("Request Bluetooth permission", fontSize = 12.sp)
            }
        }
        if (!ui.connected && !ui.busy) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("▶ Simulator", Modifier.weight(1f), onClick = vm::startDemo, primary = true)
                Action("▤ Paired Vgate", Modifier.weight(1f), onClick = {
                    expanded = !expanded
                    if (!allowed()) permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    else paired = runCatching { bondedAdapters(ctx) }.getOrDefault(emptyList())
                })
            }
        }
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (expanded && !ui.connected) {
            Spacer(Modifier.height(10.dp))
            Text("Pair adapter in Android settings first. Select a Bluetooth Classic device:", color = SubText, fontSize = 12.sp)
            for (device in paired) {
                TextButton(onClick = { vm.connectDevice(device); expanded = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect ${runCatching { device.name }.getOrNull() ?: "OBD adapter"} (${device.address})",fontSize=12.sp)
                }
            }
            if (paired.isEmpty()) Text("No paired devices returned. BLE-only devices require future GATT support.", color = Amber, fontSize = 12.sp)
            if (permissionNote.isNotBlank()) Text(permissionNote, color = SubText,fontSize=12.sp)
        }
    }
}

@Composable
private fun MetricCard(sensor: Sensor, ui: ForgeState, modifier: Modifier = Modifier) {
    val value = ui.readings[sensor.pid]?.value
    var expanded by remember { mutableStateOf(false) }
    Panel(modifier.clickable { expanded = !expanded }) {
        Text(sensor.label.uppercase(), fontSize = 10.sp, color = SubText, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Sensors.format(value), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(sensor.unit, fontSize = 11.sp, color = Aqua, modifier = Modifier.padding(bottom=5.dp))
        }
        Sparkline(ui.trends[sensor.pid] ?: emptyList(), Modifier.fillMaxWidth().height(28.dp))
        if (expanded) {
            Text(sensor.explanation, fontSize = 12.sp, color = SubText)
            Text("SAE PID 01%02X".format(sensor.pid), fontSize = 10.sp, color = Aqua)
        }
    }
}

@Composable
private fun Sparkline(numbers: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawLine(Color(0xFF355064), Offset(0f,size.height - 2f), Offset(size.width,size.height - 2f), strokeWidth=1f)
        if (numbers.size < 2) return@Canvas
        val lo = numbers.minOrNull() ?: 0.0
        val hi = numbers.maxOrNull() ?: 1.0
        val span = max(hi-lo, 0.05)
        val pts = numbers.mapIndexed { i, n -> Offset(i.toFloat()/(numbers.size-1)*size.width,
            (size.height-4f)*(1f-((n-lo)/span).toFloat())+2f) }
        pts.zipWithNext().forEach { (a,b) -> drawLine(Aqua,a,b,strokeWidth=2.5.dp.toPx(),cap=StrokeCap.Round) }
    }
}

@Composable
private fun Diagnostics(ui: ForgeState, vm: ForgeViewModel) {
    var beforeText by remember { mutableStateOf("") }
    var reportText by remember { mutableStateOf("") }
    var reportExpanded by remember { mutableStateOf(false) }
    val earlier = remember(beforeText) {
        if (beforeText.isBlank()) null else runCatching { BimmerReportParser.parse(beforeText) }.getOrNull()
    }
    val report = remember(reportText) {
        if (reportText.isBlank()) null else runCatching { BimmerReportParser.parse(reportText) }.getOrNull()
    }
    val delta = remember(earlier, report) {
        if (earlier != null && report != null &&
            earlier.modules.isNotEmpty() && report.modules.isNotEmpty())
            BimmerScanComparator.compare(earlier, report) else null
    }
    LazyColumn(contentPadding = PaddingValues(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Panel {
                Text("BMW MODULE REPORT IMPORT", color = Aqua, fontWeight = FontWeight.Bold)
                Text("Paste an existing bimmer-tool scan. Everything is interpreted offline; importing does not contact the car or clear faults.",
                    fontSize = 12.sp, color = SubText)
                Spacer(Modifier.height(8.dp))
                Action(if (reportExpanded) "Hide importer" else "Import bimmer-tool report", Modifier.fillMaxWidth()) {
                    reportExpanded = !reportExpanded
                }
                if (reportExpanded) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = beforeText, onValueChange = { beforeText = it.take(120_000) },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Earlier scan, optional") },
                        minLines = 3, maxLines = 7
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportText, onValueChange = { reportText = it.take(120_000) },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Current / after-clear scan") },
                        minLines = 5, maxLines = 10
                    )
                    if (reportText.isNotBlank() && report?.modules?.isEmpty() != false)
                        Text("No BMW modules recognised. Paste the original text export, including module headers.",
                            color = Amber, fontSize = 12.sp)
                }
                if (delta != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("SCAN COMPARISON: ${delta.earlierCount} → ${delta.laterCount} codes",
                        fontWeight = FontWeight.Bold, color = Aqua)
                    Text("Persisted: ${delta.retained.size} · Disappeared: ${delta.disappeared.size} · New: ${delta.appeared.size} · Changed module: ${delta.attributionChanged.size}",
                        fontSize = 12.sp, color = SubText)
                    delta.retained.forEach {
                        Text("PERSISTS  ${it.module} / ${it.code}", fontSize = 12.sp, color = Amber)
                    }
                    delta.attributionChanged.forEach {
                        Text("MODULE CONFLICT  ${it.code}: ${it.from} → ${it.to}",
                            fontSize = 12.sp, color = Amber)
                    }
                    if (delta.disappeared.isNotEmpty()) Text(
                        "Disappeared from memory: " + delta.disappeared.joinToString { "${it.module}:${it.code}" },
                        color = SubText, fontSize = 11.sp
                    )
                    Text("Clearing codes is not a repair. Rescan after an appropriate ignition/drive cycle and check which DTCs return.",
                        color = Amber, fontSize = 11.sp)
                }
                if (report != null && report.modules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("${report.faultCount} faults / ${report.faultModules} modules with faults / ${report.noResponseCount} no response",
                        fontWeight = FontWeight.Bold, color = TextMain, fontSize = 12.sp)
                    Text("${report.vehicle ?: "Unknown vehicle"}  ·  ${report.date ?: "Date missing"}",
                        color = SubText, fontSize = 11.sp)
                    if (report.noResponseCount > 0)
                        Text("No response does not prove failure. Some units may not be fitted; verify factory equipment.",
                            color = Amber, fontSize = 12.sp)
                }
            }
        }
        items(report?.modules?.filter { it.faults.isNotEmpty() }.orEmpty()) { module ->
            Panel {
                Text("${module.ecu} · ${module.description}", color = Aqua, fontWeight = FontWeight.Bold)
                module.faults.forEach { fault ->
                    val advice = BmwFaultAdviceLibrary.describe(module.ecu, fault.code)
                    Spacer(Modifier.height(8.dp))
                    Text("${fault.code} · ${advice.headline}", fontWeight = FontWeight.SemiBold,
                        color = if (advice.severity == "HIGH") Danger else Amber)
                    Text(advice.explanation, color = TextMain, fontSize = 12.sp)
                    Text("CHECK: ${advice.checks}", color = SubText, fontSize = 12.sp)
                    if (!advice.verifiedMeaning)
                        Text("Definition not verified for this ECU variant", color = Amber, fontSize = 10.sp)
                }
            }
        }
        item {
            SectionTitle("FAULT INTELLIGENCE", "Generic powertrain and emission diagnostics")
            Action("↻ Scan stored / pending / permanent", Modifier.fillMaxWidth(),enabled = ui.connected && !ui.busy,
                primary = true,onClick=vm::scanCodes)
        }
        item {
            Action("↻ Read freeze-frame snapshot",Modifier.fillMaxWidth(),enabled = ui.connected && !ui.busy,
                onClick=vm::scanFreezeFrame)
            if (ui.freeze.isNotEmpty()) Panel {
                Text("FREEZE FRAME · ECU SNAPSHOT",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Aqua)
                ui.freeze.forEach { (pid, value) ->
                    val sensor = Sensors.byPid[pid]
                    Text("${sensor?.label ?: "PID $pid"}: ${Sensors.format(value)} ${sensor?.unit.orEmpty()}",fontSize=12.sp)
                }
            }
        }
        item {
            Panel {
                Text("READINESS & MIL", fontWeight=FontWeight.Bold, color=Aqua)
                val r = ui.readiness
                Spacer(Modifier.height(8.dp))
                Text(if (r == null) "No monitor data yet" else "MIL: ${if (r.mil) "ON" else "OFF"}  ·  ECU-reported DTC count: ${r.storedCount}")
                Text("Incomplete continuous monitor bits: ${r?.continuousIncomplete ?: "—"}",fontSize=12.sp,color=SubText)
                Text("Incomplete non-continuous monitor mask: ${r?.noncontinuousIncomplete ?: "—"}",fontSize=12.sp,color=SubText)
                Text("This is a raw readiness summary; it is not a NZ WOF compliance decision.",fontSize=11.sp,color=SubText)
            }
        }
        items(listOf("Stored","Pending","Permanent")) { category ->
            val codes = ui.dtcs[category].orEmpty()
            Panel {
                Text("${category.uppercase()}  ·  ${codes.size}", fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Aqua)
                Spacer(Modifier.height(10.dp))
                if (codes.isEmpty()) Text(if (ui.connected) "No reported $category generic codes" else "Connect to scan", color=SubText,fontSize=12.sp)
                for (code in codes) FaultCard(code)
            }
        }
        item { InfoCard("IMPORTANT LIMITATION", "Standard OBD Mode 03/07/0A cannot inventory BMW DSC, EGS, airbag, body or gateway faults. An ECU-wide BMW scan requires manufacturer protocols over a validated transport such as ENET.") }
    }
}

@Composable
private fun FaultCard(code: String) {
    val info = FaultKnowledge.forCode(code)
    var expanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().padding(bottom=8.dp).background(Surface2,RoundedCornerShape(12.dp))
        .clickable { expanded=!expanded }.padding(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(code, color=Aqua,fontSize=19.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(info.title,fontSize=13.sp,fontWeight=FontWeight.Medium)
                Text(info.severity.uppercase(),fontSize=10.sp,color=if (info.severity=="Urgent") Danger else Amber)
            }
            Text(if (expanded) "−" else "+",fontSize=20.sp)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(info.meaning,color=TextMain,fontSize=12.sp)
            Spacer(Modifier.height(7.dp))
            Text("FIRST CHECKS",fontSize=10.sp,color=Lime,fontWeight=FontWeight.Bold)
            Text(info.firstChecks,color=SubText,fontSize=12.sp)
            Text(info.caution,color=Amber,fontSize=11.sp,modifier=Modifier.padding(top=7.dp))
        }
    }
}

@Composable
private fun Garage(ui: ForgeState, vm: ForgeViewModel) {
    var plate by remember(ui.plate) { mutableStateOf(ui.plate) }
    var model by remember(ui.model) { mutableStateOf(ui.model) }
    var year by remember(ui.year) { mutableStateOf(ui.year) }
    var engine by remember(ui.engine) { mutableStateOf(ui.engine) }
    var apiKey by remember { mutableStateOf("") } // deliberately never persisted
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("NZ VEHICLE GARAGE", "Manual + ECU VIN + optional licensed lookup")
        Panel {
            Text("REGISTRATION PROFILE", color=Aqua,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            ForgeInput("NZ number plate",plate,{plate=it.uppercase().take(8)})
            ForgeInput("Vehicle make / model",model,{model=it})
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ForgeInput("Year",year,{year=it}) }
                Box(Modifier.weight(1.5f)) { ForgeInput("Engine code",engine,{engine=it}) }
            }
            Spacer(Modifier.height(8.dp))
            Action("Save locally", Modifier.fillMaxWidth(),primary=true) { vm.saveGarage(plate,model,year,engine) }
            Text("ECU VIN: ${ui.vin.ifBlank { "Not reported yet" }}",fontSize=12.sp,color=SubText,modifier=Modifier.padding(top=10.dp))
        }
        Panel {
            Text("CARJAM API  ·  OPTIONAL",fontWeight=FontWeight.Bold,color=Aqua)
            Text("Vehicle attributes only. Requires your own CarJam developer key. Owner details are not queried.",fontSize=12.sp,color=SubText)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value=apiKey,onValueChange={apiKey=it},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),label={Text("API key (session only)")},modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            Action("Lookup NZ plate",Modifier.fillMaxWidth(),enabled=apiKey.isNotBlank() && plate.isNotBlank(),
                onClick={vm.lookupPlate(plate,apiKey)})
            if (ui.lookUpStatus.isNotBlank()) Text(ui.lookUpStatus,fontSize=12.sp,color=Amber)
        }
        SectionTitle("F10 SPECIALIST PROFILE", "Factory-specific readout roadmap")
        InfoCard("F10 / F11 IDENTIFICATION",BmwF10.overview)
        BmwF10.modules.forEach { (module, meaning) ->
            Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(11.dp)).padding(12.dp)) {
                Text(module,color=Aqua,fontWeight=FontWeight.Bold,fontSize=12.sp, modifier=Modifier.width(122.dp))
                Text(meaning,color=SubText,fontSize=12.sp)
            }
        }
    }
}

@Composable
private fun Engineering(ui: ForgeState, vm: ForgeViewModel) {
    var command by remember { mutableStateOf("010C") }
    var selectedSection by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Protocol","F10 workflows","Roadmap").forEachIndexed { i, name ->
                FilterChip(selected=selectedSection==i,onClick={selectedSection=i},label={Text(name,fontSize=11.sp)})
            }
        }
        when (selectedSection) {
            0 -> LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item {
                    SectionTitle("READ-ONLY LAB", "Transport packet journal and allowlisted probes")
                    Panel {
                        Text("ELM COMMAND",color=Aqua,fontWeight=FontWeight.Bold)
                        ForgeInput("Allowlisted: 010C / 0902 / ATRV",command,{command=it.uppercase()})
                        Spacer(Modifier.height(8.dp))
                        Action("Send read-only query",Modifier.fillMaxWidth(),enabled=ui.connected,
                            primary=true,onClick={vm.readOnlyCommand(command)})
                        Text("Writes, resets, coding, security access, and arbitrary AT headers are blocked.",color=Amber,fontSize=11.sp)
                    }
                    if (ui.notice.isNotBlank()) Banner(ui.notice,Aqua)
                    SectionTitle("BUS TRANSACTION JOURNAL", "Last ${ui.trace.size} commands (rolling)")
                }
                items(ui.trace.asReversed()) { trace ->
                    Panel {
                        Row(Modifier.fillMaxWidth()) {
                            Text(trace.command,fontWeight=FontWeight.Bold,color=Aqua,modifier=Modifier.weight(1f))
                            Text("${trace.millis} ms",fontSize=11.sp,color=SubText)
                        }
                        Text(trace.response.trim().ifBlank { "(empty)" },fontSize=11.sp,color=SubText,
                            maxLines=5,overflow=TextOverflow.Ellipsis)
                    }
                }
                item { InfoCard("EXTRACTION REALITY", "ELM327 command-response polling is not arbitrary raw CAN acquisition or direct BMW UDS gateway access. Dedicated ENET/UDS transports are separate future drivers.") }
            }
            1 -> LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item { SectionTitle("BMW F10 REPAIR PLAYBOOKS", "Interpret patterns, not isolated numbers") }
                items(BmwF10.workflows) { (name, tip) ->
                    Panel { Text(name,fontWeight=FontWeight.Bold,color=Aqua);Spacer(Modifier.height(6.dp));Text(tip,color=SubText,fontSize=13.sp) }
                }
            }
            else -> LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item { SectionTitle("ENGINEERING ROADMAP", "Not present in installed v0.1") }
                items(BmwF10.roadmap) { feature ->
                    Panel {
                        Text("□  $feature",color=TextMain,fontSize=13.sp)
                        Text("PLANNED · NOT VALIDATED",color=Amber,fontSize=10.sp)
                    }
                }
                item { InfoCard("SAFE CODING POLICY", "Before enabling BMW coding: identify exact ECU, read and verify backups, check charger voltage and ignition state, diff each proposed change, validate rollback, test on bench. Never attempt coding during motion.") }
            }
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom=6.dp)) {
        Text(title, fontSize=13.sp, fontWeight=FontWeight.Black, letterSpacing=1.6.sp, color=TextMain)
        Text(subtitle,fontSize=11.sp,color=SubText)
    }
}

@Composable private fun StatusPill(label: String, color: Color) {
    Row(Modifier.background(color.copy(alpha=.12f),RoundedCornerShape(28.dp))
        .border(1.dp,color.copy(alpha=.38f),RoundedCornerShape(28.dp)).padding(horizontal=10.dp,vertical=7.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color,CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize=10.sp,color=color,fontWeight=FontWeight.Bold)
    }
}

@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Surface,RoundedCornerShape(16.dp))
        .border(1.dp,Color(0xFF284158),RoundedCornerShape(16.dp)).padding(14.dp),content=content)
}

@Composable private fun InfoCard(title: String, contents: String) {
    Panel { Text(title,color=Aqua,fontSize=12.sp,fontWeight=FontWeight.Bold);
        Spacer(Modifier.height(6.dp));Text(contents,fontSize=12.sp,color=SubText) }
}

@Composable private fun Banner(message: String, color: Color) {
    Text(message,color=color,fontSize=12.sp,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp)
        .background(color.copy(alpha=.09f),RoundedCornerShape(9.dp)).padding(10.dp))
}

@Composable private fun SmallFeature(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Surface,RoundedCornerShape(12.dp)).padding(11.dp)) {
        Text(label,fontSize=10.sp,color=SubText)
        Text(value,fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun Action(title: String, modifier: Modifier = Modifier, enabled: Boolean = true,
                               primary: Boolean = false, onClick: () -> Unit) {
    if (primary) Button(onClick=onClick,enabled=enabled,modifier=modifier,contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp)) {
        Text(title,fontSize=12.sp,maxLines=1)
    } else OutlinedButton(onClick=onClick,enabled=enabled,modifier=modifier,contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp)) {
        Text(title,fontSize=12.sp,maxLines=1)
    }
}

@Composable private fun ForgeInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value=value,onValueChange=onChange,modifier=Modifier.fillMaxWidth(),
        label={Text(label,fontSize=12.sp)},singleLine=true)
}

/** Offline coding plan. Does not perform ECU writes. */
@Composable
private fun CodingWorkspace(ui: ForgeState) {
    var selectedId by remember { mutableStateOf(F10CodingCatalog.features.first().id) }
    val feature = F10CodingCatalog.features.first { it.id == selectedId }
    var chosen by remember(selectedId) { mutableStateOf(feature.choices.first()) }
    var checked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("BMW F10 CODING", "Feature selection · BMW-specific ECU programming roadmap")
        Banner("NO VEHICLE WRITES. Feature labels are real, but byte-level CAFD mappings are not verified.", Amber)
        Panel {
            Text("TRANSPORT READINESS", color = Aqua, fontWeight = FontWeight.Bold)
            Text("Adapter: " + ui.name, fontSize = 12.sp)
            Text("Session: " + (if (ui.simulator) "SIMULATOR" else ui.state), fontSize = 12.sp)
            Text("Vehicle VIN: " + ui.vin.ifBlank { "Not read yet" }, fontSize = 12.sp)
            Text("BMW coding-data read: NOT AVAILABLE", color = Amber, fontSize = 12.sp)
            Text("BMW coding-data write: LOCKED", color = Amber, fontSize = 12.sp)
        }
        SectionTitle("F10 CONFIGURATION CATALOG", "Select a feature and desired value")
        F10CodingCatalog.features.forEach { candidate ->
            val active = candidate.id == selectedId
            Column(Modifier.fillMaxWidth()
                .background(if (active) Surface2 else Surface, RoundedCornerShape(12.dp))
                .clickable { selectedId = candidate.id; checked = false }.padding(12.dp)) {
                Text((if (active) "◉ " else "○ ") + candidate.name,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("ECU: " + candidate.controlUnits.joinToString(" / "),
                    color = Aqua, fontSize = 11.sp)
                if (active) {
                    Text(candidate.explanation, color = SubText, fontSize = 12.sp)
                    Text("Needs: " + candidate.requirements, color = Amber, fontSize = 11.sp)
                }
            }
        }
        Panel {
            Text("CHANGE PREVIEW", fontWeight = FontWeight.Bold, color = Aqua)
            Text(feature.name, fontWeight = FontWeight.Bold)
            Text("Desired value:", color = SubText, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                feature.choices.forEach { option ->
                    FilterChip(selected = chosen == option,
                        onClick = { chosen = option; checked = false },
                        label = { Text(option, fontSize = 11.sp) })
                }
            }
            Text("Requested: " + chosen, color = Lime, fontSize = 12.sp)
            Text("Current coding value: UNKNOWN (no CAFD/NCD backup)",
                color = SubText, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Action("Check coding readiness", Modifier.fillMaxWidth(), primary = true) {
                checked = true
            }
            if (checked) {
                Spacer(Modifier.height(8.dp))
                Text("BLOCKED: No exact ECU mapping, independently verified write provider, original coding backup, stable supply check or read-back verification.",
                    color = Amber, fontSize = 12.sp)
            }
        }
        InfoCard("HOW REAL CODING WILL WORK",
            "Identify ECU and software version, read original coding bytes, save a verified backup, calculate a module-specific FDL diff, require parked vehicle and external power, write via the supported Vgate/BMW coding transport, read back, and confirm or restore the original. No generic UDS hex write is offered.")
    }
}
