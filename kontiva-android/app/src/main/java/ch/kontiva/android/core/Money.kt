package ch.kontiva.android.core

import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

/**
 * An amount of Swiss money, stored exactly as integer **Rappen** (1 CHF = 100 Rp).
 * Never represented as a floating-point number — all arithmetic is Long Rappen;
 * CHF strings are produced for presentation only. 1:1 with the iOS `Money`.
 */
@Serializable
data class Money(val rappen: Long) : Comparable<Money> {

    val isZero: Boolean get() = rappen == 0L
    val isNegative: Boolean get() = rappen < 0L
    val isPositive: Boolean get() = rappen > 0L
    val wholeFrancs: Long get() = rappen / 100
    val rappenComponent: Long get() = (rappen % 100).absoluteValue

    operator fun plus(other: Money) = Money(rappen + other.rappen)
    operator fun minus(other: Money) = Money(rappen - other.rappen)
    operator fun unaryMinus() = Money(-rappen)
    override fun compareTo(other: Money) = rappen.compareTo(other.rappen)

    /** Multiply by an integer count (stays exact). */
    fun scaled(by: Long) = Money(rappen * by)

    /** Whole-number percentage of [base] using integer math (0 if base is zero). */
    fun percentOf(base: Money): Int {
        if (base.rappen == 0L) return 0
        return ((rappen * 100) / base.rappen).toInt()
    }

    /** Integer division into equal shares, truncating toward zero. */
    fun divided(by: Long): Money {
        require(by != 0L) { "cannot divide Money by zero" }
        return Money(rappen / by)
    }

    /** Swiss-style CHF string, e.g. `CHF 1'234.50`, `-CHF 50.00`. Integer-only. */
    fun formattedCHF(showSymbol: Boolean = true): String {
        val negative = rappen < 0
        val magnitude = if (rappen == Long.MIN_VALUE) Long.MAX_VALUE else rappen.absoluteValue
        val francs = magnitude / 100
        val rp = magnitude % 100
        var result = "${groupThousands(francs)}.${rp.toString().padStart(2, '0')}"
        if (showSymbol) result = "CHF $result"
        if (negative) result = "-$result"
        return result
    }

    companion object {
        val zero = Money(0)

        /** Group a non-negative integer with Swiss apostrophes: 1234567 → 1'234'567. */
        fun groupThousands(value: Long): String {
            val digits = value.toString()
            if (digits.length <= 3) return digits
            val sb = StringBuilder()
            var count = 0
            for (ch in digits.reversed()) {
                if (count != 0 && count % 3 == 0) sb.append('\'')
                sb.append(ch)
                count++
            }
            return sb.reverse().toString()
        }

        /**
         * Parse Swiss-style input into exact Rappen without using Double. Accepts an
         * optional CHF marker, apostrophe/space thousands separators, '.' or ',' as
         * the decimal separator, optional leading sign, and 0–2 fractional digits.
         * Returns null on malformed input (strict by design — no silent rounding).
         */
        fun parse(raw: String): Money? {
            var s = raw.trim()
            if (s.isEmpty()) return null
            for (token in listOf("CHF", "chf", "Chf", "Fr.", "fr.", "SFr.")) s = s.replace(token, "")
            s = s.trim()
            if (s.isEmpty()) return null

            var negative = false
            when {
                s.startsWith("-") -> { negative = true; s = s.substring(1) }
                s.startsWith("+") -> s = s.substring(1)
            }
            for (sep in listOf("'", "’", " ", " ", " ", " ")) s = s.replace(sep, "")
            if (s.isEmpty()) return null

            val lastDot = s.lastIndexOf('.')
            val lastComma = s.lastIndexOf(',')
            val decimalIndex = maxOf(lastDot, lastComma).takeIf { it >= 0 }

            val integerPart: String
            val fractionPart: String
            if (decimalIndex != null) {
                integerPart = s.substring(0, decimalIndex)
                fractionPart = s.substring(decimalIndex + 1)
            } else {
                integerPart = s
                fractionPart = ""
            }
            val intDigits = integerPart.ifEmpty { "0" }
            if (!intDigits.all { it.isDigit() }) return null
            if (!fractionPart.all { it.isDigit() }) return null
            if (fractionPart.length > 2) return null

            val francs = intDigits.toLongOrNull() ?: return null
            val rp = fractionPart.padEnd(2, '0').toLongOrNull() ?: return null
            val total = francs * 100 + rp
            return Money(if (negative) -total else total)
        }
    }
}

/** Exact sum of money values. */
fun Iterable<Money>.total(): Money = fold(Money.zero) { acc, m -> acc + m }
