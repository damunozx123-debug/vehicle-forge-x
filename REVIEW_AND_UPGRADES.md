# Independent-minded review + release rules

Date: 22 October 2026 · Build: BMW Forge X v0.1 source

## Evaluation basis

**Executed:** Kotlin/JVM compilation of pure core modules and 23 passing deterministic protocol/reasoning checks. Reviewed source for permission scope, write paths, storage and API calls.

**Not executed:** Gradle Android compilation, phone/emulator launch, Bluetooth adapter link, BMW F10 readout, real CarJam API response, endurance measurements, accessibility/usability testing. There is **no APK** in this package. Results must not be mislabelled production-ready.

### Self-rating (0 to 10, deliberately strict)

| Area | Score | Why |
|---|---:|---|
| Overall demonstrable quality | **5.5** | Substantial original native prototype; absent Android binary and vehicle tests |
| Source architecture | 7.0 | Transport/core/VM/UI divided; one ViewModel is still too large |
| Actual protocol decoding | 6.5 | Real ISO 15031 Mode 01/02/03/07/09/0A subset, parser tests, but weak multi-frame edge handling |
| UI / information architecture | 7.0 | Gauge, trends, fault drilldowns, garage, lab; no device accessibility review |
| Education / fault interpretation | 7.0 | Offline code explanations and measured-context guidance with warnings; narrow code coverage |
| BMW F10 depth | **2.0** | Useful F10 reference/workflows, not yet OEM multi-module diagnosis |
| Vgate compatibility | 4.5 | Bluetooth SPP code but no real BM+ verification; no BLE |
| NZ plate identification | 4.0 | Manual support and a keyed CarJam integration; API schema and entitlements untested |
| Read-only safety | 8.0 | No 04 clearing, flash, security/actuation; still lacks bench stress tests and closed-state hardening |
| Reliability and observability | 5.0 | Command queue, timeouts, raw traces; no reconnect FSM, persistent buffer or cancellation tests |
| Production readiness | **2.0** | Not built as APK, no automotive validation, no crash/accessibility telemetry (offline) |

Scores measure **evidence**, not ambition. Calling this “better than BimmerCode” today would be dishonest.

## Findings and upgrades already processed during this build

1. **FIXED**: Compact ELM `410C1AF8` frames were incorrectly discarded as 29-bit CAN headers. Core decoder now recognises common compact and spaced responses. Regression tests pass.
2. **FIXED**: Multiple ECU reply lines were concatenated when decoding DTCs. DTC decode now operates per reply line and removes duplicates. Multi-ECU regression test passes.
3. **ADDED**: ISO-TP-style and numbered ELM Mode 09 VIN parsing tests, with explicit incomplete-VIN rejection.
4. **ADDED**: Mode 02, frame-zero snapshots and tests. Unsupported data remains absent rather than fabricated.
5. **ADDED**: Deterministic conservative alert engine with separate safety thresholds and regression tests.
6. **FIXED**: ViewModel onCleared now schedules transport closure so the socket does not remain intentionally active after owner destruction.
7. **ENFORCED**: Strict read-only command allowlist; no ECU writes or coding endpoints exist.

## Priority upgrades after device feedback

**P0: Verified Android product (first).** Get a real Android Studio APK and close build errors. Install on Android 16, confirm permissions, tested SPP adapter product ID, handshake, VIN parsing, PID bitmaps, raw response traces, scan and export. Collect *redacted* logs. On static tests alone compatibility is unknown.

**P1: Protocol robustness.** Add proper per-ECU ISO-TP state machine, sequence checks, segmentation handling for VIN and DTC, adapter capability detection and throttling, transport disconnect recovery, Bluetooth reconnect state machine, bounded read error backoff, and mock serial transcript replay. Add Room persistence and selectable chart rates.

**P2: BMW F10 read-only OEM support.** Introduce a separate Android Ethernet ENET/HSFZ driver. Reuse concepts from the MIT N55 F10 dashboard; implement independently. Add source-licensed UDS transport and validated DID packs keyed to exact VIN/ECU software version. Inventory DME/DDE, EGS, DSC, CAS, ZGW, KOMBI, ACSM, ICM as available. Parse UDS 0x19 DTC statuses per ECU. Never apply generic DIDs to every module.

**P3: Product-grade capabilities.** Expand the code database, graph overlays and comparable cold-start runs; add condition-based diagnostic reasoning (fuel demand vs pressure, map vs baro, knock/ignition when available), guided inspection flow, VIN vehicle profile database, XML/JSON session export, optional offline local model with evidence-linked statements, BLE and USB adapters.

**P4: Carefully gated services and coding.** BMW-specific reset/registration routines, then FDL coding only after obtaining compatible documentation and building verified readback/backup/restore, bench tests, power supply and ignition interlocks. Do not copy BMW proprietary data from illicit bundles, guess control bytes or deploy flash without proven recovery.

## Rules for every future upgrade

1. **Evidence gate:** Mark each feature `PLANNED`, `IMPLEMENTED`, `UNIT TESTED`, `BENCH TESTED`, or `VEHICLE VERIFIED`. Never conflate statuses.
2. **Read-only first:** All new transports default to non-mutating operations. Vehicle writes require a separate reviewed permission and build variant.
3. **ECU-bound capability:** Manufacturer commands are valid only for an exact ECU family/software identifier and traceable source of truth.
4. **Rollback invariant:** Before changes, take checksum-verified readback or backups, show old/new diff, and test recovery on bench hardware.
5. **Supply and motion checks:** Mutating functions require parked vehicle, verified ignition mode, monitored stable battery supply and an abort path; never promise a universal voltage cutoff.
6. **Protocol fidelity:** Keep raw request/response traces, timeouts, ECU address and ISO-TP frame state for diagnosis. Redact VIN/plates in shared reports by default.
7. **Data honesty:** A value is labelled `ECU`, `ESTIMATED`, `DEMO` or `EXTERNAL REGISTRY`; don't treat estimated boost as a direct turbo sensor.
8. **Privacy:** No car/plate/location sharing without opt-in, no tracking SDK, no background data uploads. User API secrets stay on-device or in a credential-protected backend.
9. **Test threshold:** No release unless all JVM/parser tests pass, Android build and instrumentation tests pass, Bluetooth connection/disconnect fuzz cases pass, and acceptance tests on a supported adapter succeed.
10. **Safety priority:** Block workflows involving airbags, brake/steering programming or emissions-system forced routines until qualified testing and review. Driver must never operate dashboard while vehicle moves.
11. **Dependency compliance:** Honour MIT/GPL/AGPL licenses; integration choices should be explicit and legally compatible.
12. **F10 acceptance matrix:** Test on known year, engine, ECU software and adapter firmware; record unsupported configurations honestly.

## Minimum real-car acceptance run

- Ignition on, engine off: ELM init, `ATI`, `ATDP`, `ATRV`, `0100` logged; no resets/writes.
- Engine idle: 30 min stable PID polling; log RMS/cadence, disconnections, CPU/memory, thermal/battery usage.
- Verify RPM via instrument cluster and voltage by independent meter, coolant plausibility after warmup.
- Run read-only scans and compare returned stored/pending codes with ISTA or professional scan tool; do not clear codes.
- Validate CSV file integrity and exact sample count.
- Disconnect/reconnect 10× and handle adapter unplug gracefully.
- With explicit licensed CarJam API access, inspect real `basic=1` JSON schema and test the plate model mapping.
- Do not activate service/coding buttons until a separate bench validation milestone is complete.

## Source limitation

No OEM ECU DIDs, coding byte maps or BMW service functions are shipped. This is an architectural and practical boundary, not a UI omission disguised as functionality.
