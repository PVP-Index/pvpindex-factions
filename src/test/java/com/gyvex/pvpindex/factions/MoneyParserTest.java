package com.gyvex.pvpindex.factions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gyvex.pvpindex.factions.util.MoneyParser;
import org.junit.jupiter.api.Test;

class MoneyParserTest {

    @Test
    void parsesSuffixes() {
        assertEquals(1_000d, MoneyParser.parse("1k").orElseThrow());
        assertEquals(6_000_000d, MoneyParser.parse("6m").orElseThrow());
        assertEquals(2_000_000_000d, MoneyParser.parse("2b").orElseThrow());
        assertEquals(10_000_000_000_000d, MoneyParser.parse("10t").orElseThrow());
    }

    @Test
    void parsesPlainNumbers() {
        assertEquals(125.5d, MoneyParser.parse("125.5").orElseThrow());
    }

    @Test
    void rejectsInvalid() {
        assertTrue(MoneyParser.parse("abcx").isEmpty());
        assertTrue(MoneyParser.parse("").isEmpty());
    }
}

