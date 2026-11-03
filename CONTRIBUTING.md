# Contribute to Vehicle Forge X

**Public MIT source, free diagnostics for life, and research toward validated multi-brand ECU configuration.**

Please contribute engineering evidence rather than unsupported claims that every car works. The easiest starting points are [vehicle compatibility reports](https://github.com/damunozx123-debug/vehicle-forge-x/issues/new/choose), reproducible adapter failures, offline DTC definitions, test vectors, installation fixes and documentation.

## Pull requests

1. Open a focused GitHub issue describing the bug, vehicle family or change.
2. Fork the repository, work on a branch, and open a pull request to `main`.
3. Run the portable JVM checks: `./TEST_CORE.sh`. On Android Studio/SDK 37 also run `./gradlew testDebugUnitTest assembleDebug`.
4. Add or modify tests with synthetic DTC/UDS frames and explicit malformed/negative responses. Never require a specific private vehicle to run CI.
5. State which of these is evidenced: **source present, CI verified, emulator verified, parked vehicle verified, bench ECU read verified, write/restore verified**. These are separate stages.
6. Keep vehicle control operations disabled. Any physical write path requires a distinct design/PR, independent safety review and hardware-in-the-loop evidence.

## Compatibility testing

Use the GitHub **Vehicle compatibility report** template. Record make/model/year, engine class, adapter chipset/firmware, Android device/OS, steps, observed data and error messages, redacting all identifiers. No actual VIN, number plate, adapter MAC address, auth credential, vehicle owner or customer information may enter a public report.

## Coding parameter provenance

Never submit copyrighted OEM service files, flash images, proprietary seed-key algorithms, private security credentials, or imported databases whose commercial redistribution rights are unclear. A manufacturer forum post alone does not establish a safe coding profile. Real profiles require exact ECU hardware/software identity, legal source, original coding backup, allowed parameter bits and evidence of repeatable read-back and recovery.

Third-party source licenses differ: **AndrOBD/OBDForge GPL** and **ncsx PolyForm Noncommercial** code must not be copied into this MIT product merely because it's on GitHub. OBDb definitions can carry CC BY-SA obligations, which require separate review. The current app's source files were developed separately.

## Free versus Pro ownership

- All contributions merged into the **MIT core** remain available under MIT. The free diagnostic reader, documentation and core tests will remain publicly accessible.
- A paid Pro tier may later offer independently sourced, maintained and bench-verified coding profile content, integrations and support. **No licence in this repo grants us exclusive resale rights to unlicensed manufacturer data.**
- Before submitting data intended for paid or exclusively licensed packages, contact the maintainer to establish contributor rights and an appropriate agreement. Do not assume core MIT contributions become exclusive private content.

## Safety

Use only vehicles you own or have explicit authority to test. Never operate the UI while driving. Real ECU write and flash functionality is **not implemented** and should not be enabled without a verified protocol stack, OEM data, bench tests, backed-up original image and an independently audited recovery path.

See [deep ECU research](docs/ECU_CODING_RESEARCH_2026.md), [installation](docs/INSTALL_ANDROID.md), [safety](SECURITY.md) and [MIT license](LICENSE).
