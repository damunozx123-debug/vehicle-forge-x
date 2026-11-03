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
