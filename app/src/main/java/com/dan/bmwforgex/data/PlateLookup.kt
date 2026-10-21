package com.dan.bmwforgex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** CarJam's documented, opt-in basic vehicle API. Never requests ownership data. */
object PlateLookup {
    data class PlateResult(val plate: String, val make: String, val model: String, val year: String,
                           val variant: String, val rawSummary: String)
    private fun findValue(json: JSONObject, key: String, depth: Int = 0): String? {
        if (depth > 4) return null
        val names = json.keys().asSequence().toList()
        val direct = names.firstOrNull { it.equals(key, ignoreCase = true) }
        if (direct != null) return json.opt(direct)?.toString()?.takeIf { it != "null" && it.isNotBlank() }
        for (name in names) {
            val child = json.optJSONObject(name) ?: continue
            findValue(child, key, depth + 1)?.let { return it }
        }
        return null
    }
    suspend fun basic(plate: String, userApiKey: String): PlateResult = withContext(Dispatchers.IO) {
        val normalized = plate.uppercase().replace(Regex("[^A-Z0-9]"), "").take(8)
        require(normalized.length in 2..8) { "Enter a valid NZ registration plate" }
        require(userApiKey.isNotBlank()) { "Enter your own CarJam API key" }
        val params = "plate=${URLEncoder.encode(normalized,"UTF-8")}&key=${URLEncoder.encode(userApiKey,"UTF-8")}&basic=1&owner=0&owners=0&f=json"
        val connection = (URL("https://www.carjam.co.nz/api/car/?$params").openConnection() as HttpURLConnection)
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("CarJam HTTP ${connection.responseCode}. Check API access and balance.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val make = findValue(json,"make") ?: "Unknown"
            val model = findValue(json,"model") ?: "Unknown"
            val year = findValue(json,"year_of_manufacture") ?: findValue(json,"year") ?: "?"
            val variant = findValue(json,"submodel") ?: findValue(json,"variant") ?: ""
            if (make == "Unknown" && model == "Unknown") throw IllegalStateException(
                "CarJam response received but model fields were not recognized. Review API contract/schema.")
            PlateResult(normalized, make, model, year, variant, "$year $make $model $variant".trim())
        } finally { connection.disconnect() }
    }
}
