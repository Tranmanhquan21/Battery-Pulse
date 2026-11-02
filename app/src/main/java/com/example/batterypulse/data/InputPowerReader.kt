package com.example.batterypulse.data

import java.io.File

internal object InputPowerReader {
    private val roots = listOf(
        File("/sys/class/power_supply"),
        File("/sys/devices/virtual/power_supply")
    )

    fun readWatts(): Float? {
        val supplies = roots.flatMap { root ->
            runCatching { root.listFiles().orEmpty().toList() }.getOrDefault(emptyList())
        }.distinctBy { it.absolutePath }

        val direct = supplies.asSequence()
            .filter { it.name != "battery" }
            .mapNotNull { supply -> readPower(supply, "power_now") ?: readPower(supply, "input_power") }
            .firstOrNull { it in 0.1f..150f }
        if (direct != null) return direct

        return supplies.asSequence()
            .filter { it.name != "battery" }
            .mapNotNull { supply ->
                val voltage = readNumber(supply, "input_voltage") ?: readNumber(supply, "voltage_now")
                val current = readNumber(supply, "input_current") ?: readNumber(supply, "current_now")
                if (voltage == null || current == null) null
                else normalizeVoltage(voltage) * normalizeCurrent(current)
            }
            .firstOrNull { it in 0.1f..150f }
    }

    private fun readPower(directory: File, name: String): Float? {
        val raw = readNumber(directory, name) ?: return null
        return when {
            raw > 1_000_000f -> raw / 1_000_000f
            raw > 1_000f -> raw / 1_000f
            else -> raw
        }
    }

    private fun readNumber(directory: File, name: String): Float? =
        runCatching { File(directory, name).readText().trim().toFloatOrNull() }.getOrNull()

    private fun normalizeVoltage(value: Float): Float = when {
        value > 100_000f -> value / 1_000_000f
        value > 1_000f -> value / 1_000f
        else -> value
    }

    private fun normalizeCurrent(value: Float): Float = when {
        value > 100_000f -> value / 1_000_000f
        value > 100f -> value / 1_000f
        else -> value
    }
}
