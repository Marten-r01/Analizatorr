package analizator.frontend

fun formatFrontendDouble(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val raw = rounded.toString()

    return if (raw.contains('.')) {
        val fractional = raw.substringAfter('.')
        when (fractional.length) {
            0 -> raw + "00"
            1 -> raw + "0"
            else -> raw
        }
    } else {
        "$raw.00"
    }
}

fun formatFrontendTimestamp(epochMillis: Long): String {
    return epochMillis.toString()
}