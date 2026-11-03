# ECU diagnostics and coding research: Vehicle Forge X

Last researched: 3 November 2026 (NZ). **This is an implementation brief, not a list of vehicles we have successfully coded.**

## Executive findings

**An ECU diagnostic trouble code (DTC) is not a coding parameter.** Reading P0301 is standards-based emissions diagnostics on a compatible car. Changing a comfort feature or an ECU calibration requires ECU-specific configuration, software identity, transport, access and recovery. Do not sell "editing trouble codes"; sell a clearly defined, validated configuration procedure for the user's particular ECU/software combination.

| Level | What is useful | Protocol / data | Implemented now |
| --- | --- | --- | --- |
| A | Live readings, generic stored/pending/permanent DTCs, readiness and freeze frames | SAE J1979 / ELM327 on supported vehicles | Yes, in source |
| B | Brand/module DTC discovery and diagnostics | UDS service 0x19, UDS 0x22, KWP variants, ISO-TP and OEM databases | Offline UDS 0x19 and 0x22 response decoding only |
| C | Comfort/body coding | Exact ECU coding map, manufacturer data, verified session/transport and backups | Plan + synthetic sandbox only |
| D | Firmware updates/engine remaps | OEM calibration package, pass-through protocol, power recovery, regulation and expertise | No. Separate high-risk product line, not a v0.x feature |

## Shared technical layers

1. **Bluetooth Classic ELM327 / Vgate SPP** is appropriate as a low-cost entry path to generic OBD-II. It is not an open-ended high-reliability ECU flashing gateway. The current app's `VgateSppTransport` supports that read path only.
2. **ISO 15765-2 / ISO-TP** adds segmentation, flow control and physical ECU addressing to CAN for larger messages. The current offline UDS reply decoder is **not** an ISO-TP transport. Full ISO-TP needs an implementation with bounded buffers, negative responses, blocksize/STmin, timeouts, and real bench validation. Linux reference: https://docs.kernel.org/networking/iso15765-2.html
3. **ISO 14229 UDS** services include 0x19 ReadDTCInformation, 0x22 ReadDataByIdentifier, 0x2E WriteDataByIdentifier, 0x10 diagnostic session control and 0x27 security access. Existence of a service number does **not** establish a parameter map, vehicle support or permission to use it. MIT educational reference (not copied): https://github.com/pylessard/python-udsoncan
4. **ASAM ODX / PDX (MCD-2 D)** specifies data needed to interpret variant coding, protocol parameters, supported diagnostic requests, ECU variants and service workflows. Some data may be licensed/proprietary; the file standard does not license manufacturer files: https://www.asam.net/standards/detail/mcd-2-d/
5. **SAE J2534** standardises PC/pass-through interfaces while OEM software still dictates ECU programming. Reflash is different from a typical OBD read. https://saemobilus.sae.org/standards/j2534-1_5_00-recommended-practice-pass-thru-vehicle-programming
6. **ASAM databases / DBC** are helpful for reading CAN signals, but a CAN DBC is not automatically an ECU coding map. opendbc describes its own safety and model-fingerprinting approach: https://github.com/commaai/opendbc ; review each file's license before importing.
7. **OBDb** offers community diagnostic signal and DTC definitions grouped by vehicle. It reports predominantly CC BY-SA licensing; check individual contributions and attribution obligations. Do not bulk-copy data into the MIT application without a separate compatibility review: https://github.com/obdb

## Manufacturer paths: research versus actual application support

| Family | Documented relevant route | Biggest prerequisites | Vehicle Forge X today |
| --- | --- | --- | --- |
| BMW / MINI | BMW AOS ISTA (F/G), ISTA/P (E), ICOM or compatible J2534; specific older coding workflows use EDIABAS/DATEN, ENET/K+DCAN | Exact chassis/software, legitimate OEM data, correct interface and session/backup logic | BMW F10 candidate references, generic OBD only |
| VW / Audi / Skoda / SEAT / Cupra | ODIS and ODX; recent vehicles often protect configuration with SFD / SFD2 | Licensed workshop access, OEM-backed protection authorisation, ECU software mapping | Generic OBD only |
| Ford and related Mazda generations | Manufacturer as-built/module configuration data and generation-specific diagnostic platforms | Read exact as-built before changes; module hardware/software and adapter compatibility | Generic OBD only |
| Toyota / Lexus | Toyota Techstream and manufacturer-provided validated J2534 programming path | Professional TIS subscription, calibration files, validated pass-through device | Generic OBD only |
| Honda / Acura | Honda i-HDS diagnostics and ECU reprogramming; J2534 VCI | Official software subscription and supported VCI; immobiliser has further legal access prerequisites | Generic OBD only |
| Nissan, Subaru, Hyundai/Kia, Mercedes-Benz and other makes | Different manufacturer diagnostics, gateway, models and security policies | OEM-specific legal data and hardware discovery | Generic OBD only; no verified coding maps |

Primary manufacturer references:
- BMW AOS system requirements and ISTA programming: https://bmwtechinfo.bmwgroup.com/assets/system_requirements.pdf
- VW official erWin ODIS requirements: https://vwah.erwin-store.com/erwin/showCarDiagnostics.do
- Independent VW diagnostic protection experience (scope/versions vary): https://www.ross-tech.com/vcds/download/current.php
- Toyota TIS J2534 reprogramming, data and paid access: https://techinfo.toyota.com/techInfoPortal/appmanager/t3/ti?_nfpb=true&_pageLabel=ti_j2534_device
- Honda official i-HDS: https://techinfo.honda.com/rjanisis/pubs/web/RJAAI001_J2534.htm
- BMW older NCS research, **PolyForm Noncommercial: cannot freely copy into a commercial product**: https://github.com/emdzej/ncsx
- Existing open-source Android alternatives: https://github.com/fr3ts0n/AndrOBD (GPL-3.0) and https://github.com/edwardlthompson/OBDForge (GPL-3.0). Review architecture, do not copy incompatible code or labels.

**Important:** manufacturer support described in a third-party project does not prove Vehicle Forge X compatibility. Authentic compatibility evidence requires our own test log including car make, model, year, ECU name, hardware/software ID, adapter firmware and anonymous read-back results.

## Unlockable coding parameter workflow (future Pro)

A valid *entry* in a driver catalogue must have all of:
- exact vehicle platform and year/build variant
- VIN eligibility without retaining the full VIN in public logs
- ECU identifier(s), software/calibration ID and exact matching rule
- diagnostic transport and physical interface requirements
- parameter identity, datatype, legal value set, conditional byte mapping and independent source provenance
- original coding read, exported encrypted/secure backup and sha256 digest
- predicted byte-level diff with bounded fields; reject modifications outside that range
- approved safety class (non-safety comfort/body first), ignition/park/supply proof and user confirmation
- positive response, read-back consistency, documented restore route
- hardware-in-the-loop tests on the same ECU/firmware, including disconnect/power-loss and negative response behaviour

A licensing entitlement is **never** a substitute for any of those conditions. A paid user cannot enable untested ECU operations.

### First real coding target

1. Choose **one** exact ECU on a non-safety-related comfort system and a bench setup we control. Begin with read-only ECU identity and coding backup.
2. Prefer a platform where legitimately accessible definitions exist. Candidate investigations: BMW older F10 with licensed data + suitable ENET/diagnostic hardware, or a specific older Ford body module with legitimate as-built metadata. These are *research candidates*, not a claim that either is ready.
3. Implement an ECU *profile* with tests and proven baseline read data; only then introduce write encoding against an isolated bench ECU.
4. Require an independent test on a real authorised stationary vehicle before displaying "supported".

### ECU calibration / performance tuning is a separate risk and compliance stream

Replacing firmware, changing fuel/boost/ignition tables, disabling emissions systems or safety systems is NOT the same as coding user comfort preferences. Such operations can damage engines or controllers, violate rules or invalidate a WOF. New Zealand's NZTA inspection material specifically treats ECU re-chipping on turbo vehicles as requiring low-volume vehicle certification under relevant conditions: https://vehicleinspection.nzta.govt.nz/virms/in-service-wof-and-cof/general/miscellaneous-items/engine-and-transmission . Do not add arbitrary map editing, emissions bypass or immobiliser access to the beta.

## Public core and paid services

The existing repo is **MIT licensed and public**. Any recipient can reuse, modify and redistribute that MIT code, including commercially, subject to notice requirements. This does **not** prevent a business from charging for services, validation, curated profiles or hosted features. It does mean public source alone is not effective proprietary feature protection.

Suggested tiers:
- **Free forever:** generic emission fault scans, offline definitions, readiness, VIN privacy, limited live readings, CSV and sanitised share reports; community source code and reproducible builds.
- **Pro (future, only after actual driver completion):** tested make/model/ECU coding profiles, pre-change backups and restored snapshots, guarded comfort settings, compatibility checks and maintained support. Paid access is tied to authorised and validated *content/services*, not an imaginary universal UDS unlock.
- **Workshop (future):** private audit history, fleet/export management, supported interface integrations and professional training. No pricing or sale claim until a real Pro vehicle feature is demonstrated.

For a Google Play distributed digital subscription, current Google Play Billing Library 9.x requires store integration, server-side purchase verification/entitlement grant and acknowledgement. Never trust a user-editable client boolean for Pro. https://developer.android.com/google/play/billing/integrate and https://developer.android.com/google/play/billing/backend

Contributors must be credited; decide on DCO/CLA and any separate commercial data grants **before** accepting contributions that will be sold as exclusive Pro packs. Do not promise private ownership of data already contributed under MIT or other open licenses.

## Verification and next acceptance gates

1. [x] Public repo with basic free generic DTC diagnostic functionality.
2. [x] Local-first VIN-safe reports, standalone UDS reply parsing and synthetic coding planner.
3. [ ] Direct-install versioned GitHub beta APK signed and published by passing release CI.
4. [ ] A tested phone + Bluetooth Classic adapter against at least 2 different makes, with documented evidence.
5. [ ] One full manufacturer ECU *read-only* identity/status/DTC path verified from actual ISO-TP frames.
6. [ ] One licensed ECU profile and completely verified backup/read-back on a bench.
7. [ ] First write limited to a non-safety comfort parameter, with validated rollback on same ECU software.
8. [ ] Business/legal approval of Pro catalogue, payments, licensing and right to repair obligations.

### Priority order

**Direct install + free diagnostics reliability → real multi-make OBD testing → ISO-TP/UDS read-only hardware support → one bounded coding driver → monetisation.** Anything else is marketing outrunning engineering.
