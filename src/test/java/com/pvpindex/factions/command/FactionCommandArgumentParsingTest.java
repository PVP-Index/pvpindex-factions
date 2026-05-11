package com.pvpindex.factions.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FactionCommand argument parsing")
class FactionCommandArgumentParsingTest {

    private static final class ProbeCommand extends FactionCommand {
        private ProbeCommand() {
            super("probe");
        }

        private FactionCommand.ParsedCommandArgs parse(final List<String> args, final Set<String> valuedOptions) {
            return parseArguments(args, valuedOptions);
        }
    }

    @Test
    @DisplayName("parses positional args and --key=value options")
    void parsesPositionalsAndEqualsOptions() {
        final ProbeCommand cmd = new ProbeCommand();
        final FactionCommand.ParsedCommandArgs parsed = cmd.parse(List.of("once", "--size=3"), Set.of("size"));

        assertTrue(!parsed.hasError());
        assertEquals(List.of("once"), parsed.positionalArgs());
        assertEquals("3", parsed.optionValue("size"));
    }

    @Test
    @DisplayName("parses --key value options")
    void parsesSpaceSeparatedOptions() {
        final ProbeCommand cmd = new ProbeCommand();
        final FactionCommand.ParsedCommandArgs parsed = cmd.parse(List.of("--size", "4"), Set.of("size"));

        assertTrue(!parsed.hasError());
        assertEquals("4", parsed.optionValue("size"));
    }

    @Test
    @DisplayName("returns error for unknown option")
    void errorsForUnknownOption() {
        final ProbeCommand cmd = new ProbeCommand();
        final FactionCommand.ParsedCommandArgs parsed = cmd.parse(List.of("--nope=1"), Set.of("size"));

        assertTrue(parsed.hasError());
        assertTrue(parsed.error().contains("Unknown option"));
    }

    @Test
    @DisplayName("returns error for missing option value")
    void errorsForMissingOptionValue() {
        final ProbeCommand cmd = new ProbeCommand();
        final FactionCommand.ParsedCommandArgs parsed = cmd.parse(List.of("--size"), Set.of("size"));

        assertTrue(parsed.hasError());
        assertTrue(parsed.error().contains("Missing value"));
    }
}
