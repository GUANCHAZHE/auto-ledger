package com.cooper.autoledger.data

import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.formatYuan
import com.cooper.autoledger.model.parseYuanToCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun formatYuan_formatsPositiveAndSubYuanAmounts() {
        assertEquals("18.50", CurrencyUtils.formatYuan(1850L))
        assertEquals("0.00", CurrencyUtils.formatYuan(0L))
        assertEquals("0.01", CurrencyUtils.formatYuan(1L))
        assertEquals("0.09", CurrencyUtils.formatYuan(9L))
        assertEquals("0.50", CurrencyUtils.formatYuan(50L))
        assertEquals("1.00", CurrencyUtils.formatYuan(100L))
        assertEquals("123456.78", CurrencyUtils.formatYuan(12_345_678L))
    }

    @Test
    fun formatYuan_formatsNegativeAmountsAccurately() {
        assertEquals("-18.50", CurrencyUtils.formatYuan(-1850L))
        assertEquals("-0.05", CurrencyUtils.formatYuan(-5L))
        assertEquals("-100.00", formatYuan(-10_000L))
    }

    @Test
    fun parseYuanToCents_parsesStandardDecimalsAndIntegers() {
        assertEquals(1850L, CurrencyUtils.parseYuanToCents("18.50"))
        assertEquals(1850L, CurrencyUtils.parseYuanToCents("18.5"))
        assertEquals(1800L, CurrencyUtils.parseYuanToCents("18"))
        assertEquals(1L, CurrencyUtils.parseYuanToCents("0.01"))
        assertEquals(0L, CurrencyUtils.parseYuanToCents("0"))
        assertEquals(0L, CurrencyUtils.parseYuanToCents("0.00"))
    }

    @Test
    fun parseYuanToCents_avoidsFloatingPointPrecisionLoss() {
        assertEquals(1999L, CurrencyUtils.parseYuanToCents("19.99"))
        assertEquals(29L, CurrencyUtils.parseYuanToCents("0.29"))
        assertEquals(1855L, CurrencyUtils.parseYuanToCents("18.55"))
        assertEquals(99999999L, CurrencyUtils.parseYuanToCents("999999.99"))
    }

    @Test
    fun parseYuanToCents_supportsChineseCurrencySymbolsAndThousandCommas() {
        assertEquals(1850L, CurrencyUtils.parseYuanToCents("￥18.50"))
        assertEquals(1850L, CurrencyUtils.parseYuanToCents("¥ 18.50"))
        assertEquals(2500L, CurrencyUtils.parseYuanToCents("25.00元"))
        assertEquals(8800L, CurrencyUtils.parseYuanToCents("RMB 88.00"))
        assertEquals(1234L, CurrencyUtils.parseYuanToCents("cny12.34"))
        assertEquals(123456L, CurrencyUtils.parseYuanToCents("¥1,234.56"))
        assertEquals(123456789L, parseYuanToCents("1,234,567.89元"))
    }

    @Test
    fun parseYuanToCents_supportsSignedAmounts() {
        assertEquals(-3680L, CurrencyUtils.parseYuanToCents("-36.80"))
        assertEquals(-3680L, CurrencyUtils.parseYuanToCents("-¥36.80"))
        assertEquals(-3680L, CurrencyUtils.parseYuanToCents("¥-36.80"))
        assertEquals(8900L, CurrencyUtils.parseYuanToCents("+89.00"))
        assertEquals(8900L, CurrencyUtils.parseYuanToCents("+￥89.00"))
        assertEquals(0L, CurrencyUtils.parseYuanToCents("-0.00"))
    }

    @Test
    fun parseYuanToCents_rejectsMalformedInputs() {
        assertNull(CurrencyUtils.parseYuanToCents(""))
        assertNull(CurrencyUtils.parseYuanToCents("   "))
        assertNull(CurrencyUtils.parseYuanToCents("¥"))
        assertNull(CurrencyUtils.parseYuanToCents("-"))
        assertNull(CurrencyUtils.parseYuanToCents("+"))
        assertNull(CurrencyUtils.parseYuanToCents("abc"))
        assertNull(CurrencyUtils.parseYuanToCents("18."))
        assertNull(CurrencyUtils.parseYuanToCents(".50"))
        assertNull(CurrencyUtils.parseYuanToCents("18.555"))
        assertNull(CurrencyUtils.parseYuanToCents("12.3.4"))
        assertNull(CurrencyUtils.parseYuanToCents("12,34"))
    }

    @Test
    fun roundTrip_formatAndParseAreConsistent() {
        val samples = listOf(0L, 1L, 9L, 10L, 99L, 100L, 1850L, 99999L, -5L, -3680L)
        for (cents in samples) {
            val formatted = CurrencyUtils.formatYuan(cents)
            assertEquals(cents, CurrencyUtils.parseYuanToCents(formatted))
        }
    }
}
