# Vehicle Forge X: multi-brand architecture and coding safety

## Status (v0.2 beta)

- **Implemented:** SAE OBD-II compatible live engine PIDs (where advertised), basic DTC readout, VIN attempts, freeze frame, CSV export, native Android Bluetooth Classic SPP and deterministic no-hardware simulator.
- **Implemented:** multi-brand coding *planning* UI for 23 makes and 8 candidate comfort/body/instrument features. Choose vehicle make, view missing evidence, share a VIN-masked plan, and simulate coding against synthetic bytes.
- **Implemented in core:** offline UDS ReadDataByIdentifier (0x22) positive/negative response parser for manually supplied responses.
- **Not implemented:** physical UDS requests to arbitrary ECU addresses, ECU coding-data backup, real 0x2E WriteDataByIdentifier or 0x27 SecurityAccess, routine control, module-specific diagnostics, flashing, proprietary coding databases, battery registration, supported brand/feature compatibility matrix.

**No real vehicle write capability is enabled.** The generic ELM/SAE OBD engine and the new coding sandbox are intentionally isolated.

## Why coding is per ECU, not universal

ISO 14229 specifies services and message shapes, not the complete OEM application-layer meaning of every parameter. For example:

| Item | Protocol | Status |
| --- | --- | --- |
| Read standard engine PID | SAE J1979 Mode 01 | Existing working source implementation; adapter/device validation still required |
| Read stored/pending/permanent generic faults | SAE Mode 03/07/0A | Existing implementation |
| VIN | SAE Mode 09 | Existing best-effort parser |
| UDS read by DID | 0x22 | Offline response decoding only |
| UDS write by DID | 0x2E | **Locked**; no configured write requests |
| UDS diagnostic sessions | 0x10 | **Not sent** |
| UDS security access | 0x27 | **Not sent**; no bypasses/seed-key code |

An ECU-specific coding capability requires **real** data definitions for exact make/model/year/module/software and a verified physical interface. Example candidate preferences include comfort locking, mirrors, lighting, climate, cluster units and start/stop preference; the catalogue deliberately does not assert that any individual vehicle supports them.

## Required driver interface before real coding

1. Establish vehicle identity from actual ECU and software (not simply an entered make/plate).
2. Acquire legitimately sourced and properly licensed parameter definition and command encoding for that firmware.
3. Target an individual known ECU with supported ISO-TP transport, adequate latency and segmentation.
4. Read original coding, persist verified encrypted/exportable backup and record its digest.
5. Verify legitimate access, parked/engine-off and a stable external power supply appropriate to that vehicle.
6. Obtain a specific, explicit confirmation of the byte diff and module address; reject unknown bytes and unsupported safety systems.
7. Write once, await explicit positive response, read back, compare, and follow OEM-approved recovery process on mismatch.
8. Validate each driver in a dedicated hardware-in-the-loop fixture before a production release.

The existing `VerifiedCodingBackend` interface and `CodingTransaction` safety gate are retained for a future tested per-ECU driver. No new backend implements physical writes.

## Tests

```sh
./TEST_CORE.sh
./gradlew testDebugUnitTest assembleDebug
```

CI runs the isolated JVM checks and Android tests/build. The isolated tests use synthetic vehicle data, not a real car. The Android package ID intentionally remains `com.dan.bmwforgex` for compatibility with the existing Android project; this does **not** mean the new UI is BMW-only.

## Sources and protocol context

- https://uds.readthedocs.io/en/latest/pages/knowledge_base/did.html
- https://uds.readthedocs.io/en/stable/pages/knowledge_base/service.html
- https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf
- https://developer.android.com/develop

Only work with vehicles you own or are explicitly authorised to test. No interactive device use while driving.
