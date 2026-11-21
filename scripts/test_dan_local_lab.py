"""Offline regression checks for the BlackArch local AI lab harness."""
import unittest
import dan_local_lab as lab

class LabSafetyTests(unittest.TestCase):
    def test_redacts_identifiers_but_keeps_faults(self):
        value = "VIN WBAFW11C0DE000012 MAC 00:11:22:33:44:55 P0299 api_key=secret"
        safe = lab.scrub(value)
        self.assertNotIn("WBAFW11C0DE000012", safe)
        self.assertNotIn("00:11:22:33:44:55", safe)
        self.assertNotIn("secret", safe)
        self.assertIn("P0299", safe)

    def test_patch_allowlist_prevents_vehicle_writes(self):
        self.assertTrue(lab.allowed_patch_path("app/src/main/java/com/dan/bmwforgex/core/ObdCore.kt"))
        self.assertTrue(lab.allowed_patch_path("app/src/test/java/com/dan/bmwforgex/ObdCoreTest.kt"))
        self.assertFalse(lab.allowed_patch_path("app/src/main/java/com/dan/bmwforgex/core/CodingModel.kt"))
        self.assertFalse(lab.allowed_patch_path("app/src/main/java/com/dan/bmwforgex/core/VehicleCommandPolicy.kt"))
        self.assertFalse(lab.allowed_patch_path("app/src/main/java/com/dan/bmwforgex/data/Transport.kt"))
        self.assertFalse(lab.allowed_patch_path("../README.md"))

    def test_debug_package_isolated_from_release(self):
        self.assertEqual(lab.PACKAGE, "com.dan.bmwforgex.dev")
        self.assertEqual(lab.ACTIVITY,
                         "com.dan.bmwforgex.dev/com.dan.bmwforgex.MainActivity")

    def test_patch_parsing(self):
        patch = "analysis\ndiff --git a/core_smoke_test.kt b/core_smoke_test.kt\n--- a/core_smoke_test.kt\n+++ b/core_smoke_test.kt\n@@ -1 +1 @@\n-old\n+new\n"
        self.assertTrue(lab.extract_patch(patch).startswith("diff --git a/core_smoke_test.kt"))
        self.assertEqual(lab.extract_patch("no change"), "")

    def test_ollama_endpoint_is_loopback_only(self):
        import inspect
        source = inspect.getsource(lab.ollama)
        self.assertIn("http://127.0.0.1:11434", source)
        self.assertNotIn("api.openai.com", source)

if __name__ == "__main__":
    unittest.main()
