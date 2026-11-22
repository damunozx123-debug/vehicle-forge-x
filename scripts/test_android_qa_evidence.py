"""Reproducible offline regression tests for Android QA log redaction."""
import unittest
from android_qa_evidence import redact


class QaEvidenceTests(unittest.TestCase):
    def test_identifiers_and_credentials_are_removed(self):
        raw = (
            "VIN WBAFW11C0DE000012 MAC 00:11:22:33:44:55 "
            "plate=ABC123 registration: DEF789 "
            "dan@example.com 192.168.86.21 "
            "api_key=token123 password:hunter2 "
            "authorization:Bearer secret987 "
            "phone 02041716695\n"
        )
        safe = redact(raw)
        for secret in (
            "WBAFW11C0DE000012", "00:11:22:33:44:55",
            "ABC123", "DEF789", "dan@example.com", "192.168.86.21",
            "token123", "hunter2", "secret987", "02041716695"
        ):
            self.assertNotIn(secret, safe)
        self.assertIn("[VIN REDACTED]", safe)

    def test_crash_diagnostics_and_generic_codes_survive(self):
        raw = (
            "11-22 18:00:00.000 123 123 E AndroidRuntime: FATAL EXCEPTION: main\n"
            "java.lang.IllegalStateException: report unavailable\n"
            "P0301 stored engine DTC\n"
            "Bluetooth permission denied\n"
        )
        safe = redact(raw)
        self.assertIn("FATAL EXCEPTION", safe)
        self.assertIn("IllegalStateException", safe)
        self.assertIn("P0301", safe)
        self.assertIn("Bluetooth permission denied", safe)

    def test_redaction_is_idempotent(self):
        original = "user=dan@example.com access_token=abc123"
        once = redact(original)
        self.assertEqual(once, redact(once))


if __name__ == "__main__":
    unittest.main()
