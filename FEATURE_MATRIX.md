# Feature coverage matrix (audited 22 October 2026)

Legend: **IMPLEMENTED** = present in source (device unverified); **PARTIAL** = subset/limited scope; **PLANNED** = not present. Comparator feature summaries come from public feature pages, not reverse-engineered binaries.

| Function | Typical source app focus | BMW Forge X 0.1 | Notes |
|---|---|---|---|
| Android native interface | All | IMPLEMENTED | Compose, not WebView |
| Vgate Bluetooth Classic SPP | All via supported adapters | IMPLEMENTED | Real handshake not bench tested; BLE absent |
| Live generic engine PIDs | BimmerLink / bimmer-tool | IMPLEMENTED | 22 formulas, advertised bitmap, trends |
| ECU-specific BMW sensor PIDs | BimmerLink / bimmer-tool | PLANNED | Manufacturer DID mapping/ENET |
| Custom gauge + engineering units | BimmerLink and others | PARTIAL | Read-only preset cards, expandable explanations |
| Timestamped data logs | bimmer-tool | IMPLEMENTED | CSV, raw replies, current-session buffer |
| Stored/pending/permanent OBD codes | BimmerLink / bimmer-tool | IMPLEMENTED | Legislated OBD scope only |
| All BMW module fault codes | BimmerLink / bimmer-tool | PLANNED | Must invent no F10 module capabilities |
| Plain-language fault triage | Variable | PARTIAL | 26 bundled fault explanations + threshold hints |
| Readiness monitor flags | OBD apps | PARTIAL | Basic bitmasks, not detailed monitor interpretation |
| Freeze frame | OBD apps | PARTIAL | Mode 02, frame 0 selected sensors |
| VIN from ECU | OBD apps | PARTIAL | Mode 09, common ELM formats |
| NZ plate-to-model | Not a core BMW product feature | PARTIAL | Manual + optional developer API |
| Battery registration | BimmerLink / bimmer-tool | PLANNED | Read-only v0.1 cannot register batteries |
| Battery chemistry/capacity coding | BimmerCode | PLANNED | Needs specific ECU datasets and backup/restore |
| Service interval reset | BimmerLink / bimmer-tool | PLANNED | Requires qualified ECU action |
| DPF status and forced regeneration | BimmerLink / bimmer-tool | PLANNED | Manufacturer DIDs and active routines |
| Short circuit reset / electronic handbrake | BimmerLink | PLANNED | Safety-critical writes, not guessed |
| Injector correction | bimmer-tool | PLANNED | Diesel DDE parameter mapping |
| BMW FDL coding / expert mode | BimmerCode | PLANNED | E-Sys class functionality, not built |
| Coding backups + undo | BimmerCode | PLANNED | Mandatory before enabling writes |
| BMW ENET over Ethernet | E-Sys / advanced projects | PLANNED | Native driver to be built |
| BLE GATT adapters | Some OBD apps | PLANNED | Model-specific UART GATT implementation |
| Traffic transaction journal | Engineering enhancement | IMPLEMENTED | Rolling 140 records, read-only terminal |
| Pattern-based explanation | Engineering enhancement | PARTIAL | Basic deterministic rules, no learned diagnosis |
| Offline no-hardware demo | Various FOSS OBD apps | IMPLEMENTED | Clearly synthetic |

This table intentionally does not award points for planned functionality. OBD2 does not imply universal BMW access.
