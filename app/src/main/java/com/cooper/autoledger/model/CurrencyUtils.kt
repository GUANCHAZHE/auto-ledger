package com.cooper.autoledger.model

import java.math.BigDecimal

object CurrencyUtils {
    private val AMOUNT_PATTERN = Regex("""^(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d{1,2})?$""")
    private val CURRENCY_PREFIX_PATTERN = Regex("""^(?:RMB|CNY|[¥￥])\s*""", RegexOption.IGNORE_CASE)
    private val CURRENCY_SUFFIX_PATTERN = Regex("""\s*(?:元|圆|RMB|CNY)$""", RegexOption.IGNORE_CASE)

    /**
     * Formats an integer amount in cents into a plain decimal yuan string (e.g. 1850L -> "18.50").
     */
    fun formatYuan(amountCents: Long): String {
        return BigDecimal.valueOf(amountCents, 2).toPlainString()
    }

    /**
     * Parses a raw yuan amount string (supporting optional +/-, ¥/￥/RMB/CNY/元, and thousand commas)
     * into exact integer cents, or null if invalid.
     */
    fun parseYuanToCents(raw: String): Long? {
        var working = raw.replace('\u00A0', ' ').trim()
        if (working.isEmpty()) return null

        var negative = false
        var signSeen = false

        if (working.startsWith("-") || working.startsWith("−")) {
            negative = true
            signSeen = true
            working = working.substring(1).trim()
        } else if (working.startsWith("+")) {
            signSeen = true
            working = working.substring(1).trim()
        }

        working = CURRENCY_PREFIX_PATTERN.replaceFirst(working, "").trim()

        if (!signSeen) {
            if (working.startsWith("-") || working.startsWith("−")) {
                negative = true
                working = working.substring(1).trim()
            } else if (working.startsWith("+")) {
                working = working.substring(1).trim()
            }
        }

        working = CURRENCY_SUFFIX_PATTERN.replaceFirst(working, "").trim()
        if (working.isEmpty() || !AMOUNT_PATTERN.matches(working)) {
            return null
        }

        val normalized = working.replace(",", "")
        return try {
            val unsignedCents = BigDecimal(normalized).movePointRight(2).longValueExact()
            if (negative && unsignedCents != 0L) {
                Math.negateExact(unsignedCents)
            } else {
                unsignedCents
            }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }
}

fun formatYuan(amountCents: Long): String = CurrencyUtils.formatYuan(amountCents)

fun parseYuanToCents(raw: String): Long? = CurrencyUtils.parseYuanToCents(raw)
