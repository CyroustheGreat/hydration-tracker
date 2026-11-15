package com.hydrationtracker.domain

import java.math.BigDecimal
import java.math.RoundingMode

object Volume {
    /** US fluid ounce, same factor the web app uses. */
    val ML_PER_FL_OZ: BigDecimal = BigDecimal("29.5735")

    fun formatAmount(ml: Int, unit: VolumeUnit): String = when (unit) {
        VolumeUnit.ML -> "$ml ml"
        VolumeUnit.OZ -> "${ounces(ml).toPlainString()} oz"
    }

    fun editString(ml: Int, unit: VolumeUnit): String = when (unit) {
        VolumeUnit.ML -> ml.toString()
        VolumeUnit.OZ -> ounces(ml).toPlainString()
    }

    fun ounces(ml: Int): BigDecimal =
        BigDecimal(ml).divide(ML_PER_FL_OZ, 1, RoundingMode.HALF_UP)

    /**
     * Parses a user-entered amount in the active unit and returns millilitres.
     * Blank, zero, and negative input is rejected.
     */
    fun parseToMl(raw: String, unit: VolumeUnit): Int? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val value = try {
            BigDecimal(text)
        } catch (_: NumberFormatException) {
            return null
        }
        if (value.signum() <= 0) return null
        val ml = when (unit) {
            VolumeUnit.ML -> value.setScale(0, RoundingMode.HALF_UP)
            VolumeUnit.OZ -> value.multiply(ML_PER_FL_OZ).setScale(0, RoundingMode.HALF_UP)
        }
        if (ml > BigDecimal(100_000)) return null
        return ml.intValueExact()
    }
}
