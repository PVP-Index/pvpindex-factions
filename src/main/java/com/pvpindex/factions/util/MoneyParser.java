package com.pvpindex.factions.util;

import java.util.Locale;
import java.util.OptionalDouble;

/** Parses shorthand money inputs like 1k, 6m, 2b, and 10t. */
public final class MoneyParser {

    private MoneyParser() {
    }

    public static OptionalDouble parse(final String input) {
        if (input == null) {
            return OptionalDouble.empty();
        }
        final String trimmed = input.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return OptionalDouble.empty();
        }
        final char last = trimmed.charAt(trimmed.length() - 1);
        final double multiplier = switch (last) {
            case 'k' -> 1_000d;
            case 'm' -> 1_000_000d;
            case 'b' -> 1_000_000_000d;
            case 't' -> 1_000_000_000_000d;
            default -> 1d;
        };
        final String numeric = multiplier == 1d ? trimmed : trimmed.substring(0, trimmed.length() - 1);
        try {
            return OptionalDouble.of(Double.parseDouble(numeric) * multiplier);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}

