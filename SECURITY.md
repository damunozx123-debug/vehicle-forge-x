# Security and vehicle safety - Vehicle Forge X

This is an experimental **read-only** diagnostic Android application. Real vehicle coding is disabled. Candidate configuration names and simulated ECU-byte changes are **not** authority to issue real coding commands.

## Scope

- Standard SAE OBD-II engine sensor/fault reads can be used only on vehicles the operator owns or is authorised to examine.
- No automatic vehicle ECU writes, diagnostic security bypass, flashing, routine control, battery registration or active safety-system adjustments are implemented.
- The code deliberately leaves the `VerifiedCodingBackend` interface without a production driver. A future real driver needs independently validated ECU/software data, explicit operator confirmation, a correct physical transport and hardware-in-the-loop testing.
- Do not interact with the app while driving. Use manufacturer guidance; even read-only polling can disrupt weak or abnormal ECUs.
- Never share real VINs, number plates, identifying module dumps, registration data, API keys or customer logs in public issues or screenshots.
- The optional NZ plate lookup depends on the user's own licensed credential; no key is provided by this repository. Input and output should remain private.

## Reporting

Use GitHub private vulnerability reporting if enabled or contact the maintainer privately. Do not publicly post proof-of-concept vehicle disruptions or sensitive identifiers. Treat non-trivial transport, parser or UI security changes as requiring tests.

## Public-release review

Before changing repository visibility, scan the entire Git history, issues, workflow logs and artifacts for tokens and personally identifying data. Changing visibility exposes all reachable content; deleting secrets only from the latest commit is insufficient. Rotate anything previously committed.

See [coding architecture](docs/MULTI_BRAND_CODING.md).
