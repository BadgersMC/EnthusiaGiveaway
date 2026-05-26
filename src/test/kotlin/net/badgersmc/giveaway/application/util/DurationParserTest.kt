package net.badgersmc.giveaway.application.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DurationParserTest {

    @Test fun `single unit values`() {
        assertEquals(45L, DurationParser.parse("45s"))
        assertEquals(60L * 30, DurationParser.parse("30m"))
        assertEquals(3600L, DurationParser.parse("1h"))
        assertEquals(86400L * 2, DurationParser.parse("2d"))
    }

    @Test fun `compound values`() {
        assertEquals(3600L + 30 * 60, DurationParser.parse("1h30m"))
        assertEquals(86400L + 3600 + 60 + 1, DurationParser.parse("1d1h1m1s"))
    }

    @Test fun `whitespace tolerated`() {
        assertEquals(3600L + 1800, DurationParser.parse("1h 30m"))
    }

    @Test fun `case insensitive`() {
        assertEquals(3600L, DurationParser.parse("1H"))
    }

    @Test fun `rejects garbage`() {
        assertNull(DurationParser.parse(""))
        assertNull(DurationParser.parse("hello"))
        assertNull(DurationParser.parse("123"))
        assertNull(DurationParser.parse("1h30"))
        assertNull(DurationParser.parse("0h"))
    }
}
