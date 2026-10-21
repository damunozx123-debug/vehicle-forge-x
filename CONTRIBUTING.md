# Contributing

Keep changes reproducible, testable and explicit about unverified vehicle support.

1. For protocol parser changes, add synthetic frame fixtures and run `./TEST_CORE.sh`.
2. For Android changes, run `./gradlew testDebugUnitTest assembleDebug` with JDK 17 and SDK 37.
3. Never commit secrets, private vehicle records or generated binaries.
4. Distinguish SAE generic OBD-II, BMW-specific diagnostic read services, and ECU writes/coding in feature descriptions.
5. Keep write operations disabled until reviewed and independently tested on a bench ECU.

Current readiness: protocol core has local JVM tests; Android device, BLE transport, ENET and real F10 interoperability are not verified. Include hardware model, firmware, engine, model year and a sanitized trace when opening a compatibility issue.
