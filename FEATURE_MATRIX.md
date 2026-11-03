# Vehicle Forge X - evidence-based feature matrix (v0.2)

**Implemented** means source is present, **CI verified** requires successful workflow checks, and **vehicle verified** requires specific adapter/ECU testing. These statuses are distinct.

| Capability | Status in source | Coverage |
| --- | --- | --- |
| Native Android Kotlin/Compose UI | Implemented | Android |
| Bluetooth Classic SPP ELM327 | Implemented | Compatible adapters; physical validation required |
| Generic SAE engine sensor polling | Implemented | Supported OBD-II vehicles/PIDs |
| Standard DTC 03/07/0A and readiness | Implemented | Emissions-related engine scope |
| Freeze frame / VIN / CSV export | Partial | Depends on actual ECU reply |
| BMW F10 reference and explanations | Retained | BMW-specific historical catalog |
| Multi-brand coding planner | Implemented | 23 make labels, no compatibility claim |
| Candidate comfort/body settings | Implemented | 8 proposed feature categories |
| VIN-masked coding plan sharing | Implemented | No full VIN in export |
| Coding simulator | Implemented | **Synthetic memory only** |
| UDS response decode for supplied DID data | Implemented in core | Offline parse; no vehicle request |
| Real ECU coding, security access, UDS writes | **Not implemented** | All vehicles; disabled |
| Verified OEM configuration maps | **Not included** | All makes |
| Module-specific transport, all-ECU scans | **Not implemented** | All makes |
| Backup, write, read-back/restore on car | **Not implemented** | Only a fail-closed transaction interface exists |
| Physical vehicle hardware-in-loop tests | **Not verified** | Must be documented per ECU/firmware |

See [multi-brand coding design](docs/MULTI_BRAND_CODING.md). A list of makes is not a matrix of verified compatibility.


## v0.3 beta engineering additions

| Capability | Source status | What is actually supported |
| --- | --- | --- |
| Privacy-safe free DTC report sharing | Implemented and Android/JVM tested | Generic SAE engine codes; full VIN and plate intentionally omitted |
| Offline UDS DTC 0x19 0x02 decoder | Implemented and Android/JVM tested | Complete *already reassembled* response payload only |
| Offline UDS DID 0x22 response parser | Implemented in core | Manually supplied payload only; does not contact ECU |
| Manufacturer-specific research guidance | Implemented in Coding UI | BMW, VW Group, Toyota, Honda, Ford/Mazda and generic fallback; **not** compatibility certification |
| Validated *offline* byte-diff preview | Implemented and unit tested | Explicit mask, offset, snapshot size and software ID; no actual ECU mapping provided |
| Bluetooth Classic SPP deny-by-default read policy | Implemented and unit tested | Blocked writes/reset/security/AT-header mutation through existing transport |
| Pro payments and server-issued entitlements | **Not implemented** | No paid subscriptions should be sold yet |
| Real module-specific read-only UDS/ISO-TP | **Not implemented** | Real hardware capture/reassembly still needed |
| Real ECU software version matched write/restore | **Not implemented** | Exact legal coding map + bench tests + supply check required |
| Emissions/powertrain tuning and flashing | **Not implemented** | High-risk, separately regulated and out of beta scope |

**Terminology rule:** "supports 23 makes" currently describes a planning selector, not verified OEM ECU access. Generic OBD coverage must be measured by actual vehicle and adapter reports, not a logo list.
