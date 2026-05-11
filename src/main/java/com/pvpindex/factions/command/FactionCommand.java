package com.pvpindex.factions.command;

import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base for every {@code /f} sub-command (and sub-sub-command).
 *
 * <h3>Authoring a leaf command</h3>
 * <pre>{@code
 * public final class CmdCreate extends FactionCommand {
 *     public CmdCreate(FactionsTeamsService teams) {
 *         super("create");
 *         setPermission("pvpindex.faction.create");
 *         setDescription("Create a new faction.");
 *         setRequiredArgs("<name>");
 *         setRequiresPlayer(true);
 *         this.teams = teams;
 *     }
 *     protected void perform(CommandContext ctx) { ... }
 * }
 * }</pre>
 *
 * <h3>Authoring a group command</h3>
 * <pre>{@code
 * public final class CmdBank extends FactionCommand {
 *     public CmdBank(FactionsTeamsService teams, EngineEconomy eco) {
 *         super("bank");
 *         setPermission("pvpindex.faction.bank");
 *         setDescription("Faction bank management.");
 *         setRequiresPlayer(true);
 *         addChild(new CmdBankDeposit(teams, eco));
 *         addChild(new CmdBankWithdraw(teams, eco));
 *     }
 * }
 * }</pre>
 *
 * <p>The dispatch order in {@link #execute(CommandContext)} is:
 * <ol>
 *   <li>Permission check</li>
 *   <li>Player requirement check</li>
 *   <li>Child routing (if children registered and {@code args[0]} matches)</li>
 *   <li>Required-arg count check</li>
 *   <li>{@link #perform(CommandContext)}</li>
 * </ol>
 */
public abstract class FactionCommand {

    // -------------------------------------------------------------------------
    // State (set in subclass constructors via protected setters)
    // -------------------------------------------------------------------------

    private final String name;
    private String permission = null;
    private String description = "";
    private List<String> requiredArgs = List.of();
    private List<String> optionalArgs = List.of();
    private boolean requiresPlayer = false;
    private String commandPath = "/f";
    private final ArrayList<String> aliases = new ArrayList<>();

    /**
     * Ordered child-command map. Order matches registration order which drives
     * both help display and usage-string generation.
     */
    private final LinkedHashMap<String, FactionCommand> children = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    protected FactionCommand(final String name) {
        this.name = name.toLowerCase();
    }

    // -------------------------------------------------------------------------
    // Configuration helpers (call only from subclass constructors)
    // -------------------------------------------------------------------------

    /** Set the required permission node ({@code null} = accessible by everyone). */
    protected final void setPermission(final String perm) {
        this.permission = perm;
    }

    /** Set the one-line description shown in {@code /f} help. */
    protected final void setDescription(final String desc) {
        this.description = desc;
    }

    /**
     * Declare required argument placeholder names in order, e.g.
     * {@code setRequiredArgs("<name>")} or {@code setRequiredArgs("<player>", "<amount>")}.
     */
    protected final void setRequiredArgs(final String... args) {
        this.requiredArgs = List.of(args);
    }

    /**
     * Declare optional argument placeholder names, e.g.
     * {@code setOptionalArgs("[factionName]")}.
     */
    protected final void setOptionalArgs(final String... args) {
        this.optionalArgs = List.of(args);
    }

    /** When {@code true}, the command is rejected with a message if the sender is not a player. */
    protected final void setRequiresPlayer(final boolean requires) {
        this.requiresPlayer = requires;
    }

    /**
     * Register optional alias names for this command (e.g. {@code "i"} for {@code "info"}).
     * Aliases are registered by {@link com.pvpindex.factions.registry.CommandRegistry}
     * alongside the primary name and included in tab-completion.
     */
    protected final void setAliases(final String... aliasNames) {
        aliases.addAll(List.of(aliasNames));
    }

    /** Return an unmodifiable view of the registered alias names. */
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * Register a child command.
     *
     * <p>Children are tried in {@link #execute(CommandContext)} <em>before</em>
     * falling through to {@link #perform(CommandContext)}, so group commands can
     * have a default action for unmatched first arguments.
     *
     * @param child child command to add
     */
    protected final void addChild(final FactionCommand child) {
        child.commandPath = this.commandPath + " " + this.name;
        children.put(child.getName().toLowerCase(), child);
    }

    // -------------------------------------------------------------------------
    // Override points
    // -------------------------------------------------------------------------

    /**
     * Command body. Only called once all guards have passed and no child
     * command matched the first argument.
     *
     * <p>The default implementation sends the usage string. Group commands that
     * support a default action (e.g. {@code /f warp <name>} for teleport) should
     * override this.
     *
     * @param ctx execution context — args are relative to this command
     */
    protected void perform(final CommandContext ctx) {
        MsgUtil.sendKey(ctx.getSender(), "general.invalid-args", "<red>Usage: {usage}", "usage", getUsage());
    }

    /**
     * Provide contextual tab-completions for the argument at position
     * {@code argIndex} (zero-based, relative to this command).
     *
     * <p>For commands that also have children, any results returned here when
     * {@code argIndex == 0} are <em>merged</em> with the accessible child names,
     * which lets group commands offer both sub-command names and dynamic values
     * (e.g. warp names alongside "set"/"delete") at the same position.
     *
     * <p>The base implementation returns an empty list.
     *
     * @param ctx      execution context
     * @param argIndex zero-based index of the argument currently being typed
     * @return un-filtered list of completion candidates
     */
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    /**
     * Full execution dispatch — permission → player → child routing →
     * arg-count → {@link #perform(CommandContext)}.
     *
     * @param ctx context whose {@link CommandContext#getArgs()} holds the
     *            tokens after this command's own name has been consumed
     */
    public final void execute(final CommandContext ctx) {
        if (permission != null && !ctx.getSender().hasPermission(permission)) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "general.no-permission",
                "<red>You do not have permission to run this command.");
            return;
        }
        if (requiresPlayer && !ctx.isPlayer()) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "general.player-only",
                "<red>This command can only be used by a player.");
            return;
        }
        if (!children.isEmpty() && !ctx.getArgs().isEmpty()) {
            final FactionCommand child = children.get(ctx.arg(0).toLowerCase());
            if (child != null) {
                child.execute(ctx.shift());
                return;
            }
        }
        if (ctx.getArgs().size() < requiredArgs.size()) {
            MsgUtil.sendKey(ctx.getSender(), "general.invalid-args", "<red>Usage: {usage}", "usage", getUsage());
            return;
        }
        perform(ctx);
    }

    /**
     * Tab-completion dispatch.
     *
     * <p>When the command has children and the user is completing the first
     * argument (args size ≤ 1), accessible child names are returned merged with
     * any results from {@link #complete(CommandContext, int) complete(ctx, 0)}.
     * When a specific child is fully typed, the call is delegated to that
     * child. Otherwise, {@link #complete(CommandContext, int)} drives the result.
     *
     * <p>Caller is responsible for stripping the sub-command name before
     * building the context (i.e. {@code ctx.getArgs()} must already be relative
     * to this command).
     *
     * @param ctx context whose {@link CommandContext#getArgs()} are relative to
     *            this command; last element is the partial token being typed
     * @return filtered, non-null list of completions
     */
    public final List<String> tabComplete(final CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        final String partial = args.isEmpty() ? "" : args.get(args.size() - 1).toLowerCase();

        if (!children.isEmpty()) {
            if (args.size() <= 1) {
                // Completing the first arg: child names merged with complete(ctx, 0)
                final Set<String> results = new LinkedHashSet<>();
                children.values().stream()
                    .distinct()
                    .filter(c -> c.getName().startsWith(partial)
                        && (c.getPermission() == null
                        || ctx.getSender().hasPermission(c.getPermission())))
                    .map(FactionCommand::getName)
                    .forEach(results::add);
                complete(ctx, 0).stream()
                    .filter(s -> s.toLowerCase().startsWith(partial))
                    .forEach(results::add);
                return new ArrayList<>(results);
            }
            // Route to matching child
            final FactionCommand child = children.get(args.get(0).toLowerCase());
            if (child != null) {
                return child.tabComplete(ctx.shift());
            }
            // No child matched: this command may still accept positional args.
            final int argIndex = Math.max(0, args.size() - 1);
            return complete(ctx, argIndex).stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }

        // Leaf command: delegate to complete()
        final int argIndex = Math.max(0, args.size() - 1);
        return complete(ctx, argIndex).stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Primary lower-case name of this command. */
    public String getName() {
        return name;
    }

    /** Required permission node, or {@code null} if accessible by everyone. */
    public String getPermission() {
        return permission;
    }

    /** One-line description for help listings. */
    public String getDescription() {
        return description;
    }

    /**
     * Build a usage string.
     *
     * <p>For group commands (with children) the format is
     * {@code /f <cmd> <child1|child2>}; for leaf commands it lists required
     * and optional argument placeholders.
     *
     * <p>Subclasses may override to hard-code the full path including any
     * parent command name (e.g. {@code "/f bank deposit <amount>"}).
     *
     * @return usage hint string
     */
    public String getUsage() {
        final StringBuilder sb = new StringBuilder(commandPath).append(' ').append(name);
        if (!children.isEmpty()) {
            final Set<String> unique = new LinkedHashSet<>();
            children.values().forEach(c -> unique.add(c.getName()));
            sb.append(" <").append(String.join("|", unique)).append('>');
        } else {
            requiredArgs.forEach(a -> sb.append(' ').append(a));
            optionalArgs.forEach(a -> sb.append(' ').append(a));
        }
        return sb.toString();
    }

    /**
     * Return an unmodifiable view of all registered direct children.
     *
     * @return immutable collection of child commands
     */
    public Collection<FactionCommand> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    // -------------------------------------------------------------------------
    // Protected helpers (available to subclasses in perform())
    // -------------------------------------------------------------------------

    /**
     * Cast the sender to {@link org.bukkit.entity.Player}, or send an error
     * and return {@code null}.
     *
     * <p>Only needed in commands that have {@code requiresPlayer = false} but
     * may still want to obtain the player in some code paths. Commands where
     * {@code setRequiresPlayer(true)} is called don't need this guard — the
     * base class already rejects non-players.
     *
     * @param ctx execution context
     * @return the player, or {@code null}
     */
    protected org.bukkit.entity.Player requirePlayer(final CommandContext ctx) {
        if (ctx.getSender() instanceof org.bukkit.entity.Player player) {
            return player;
        }
        MsgUtil.sendKey(
            ctx.getSender(),
            "general.player-only",
            "<red>This command can only be used by a player.");
        return null;
    }

    /**
     * Parse command arguments into positional arguments and long options.
     *
     * <p>Supported option forms:
     * <ul>
     *   <li>{@code --name=value}</li>
     *   <li>{@code --name value}</li>
     * </ul>
     *
     * @param rawArgs raw argument list
     * @param valuedOptionNames known long-option names that require values
     * @return parsed args or parse error
     */
    protected ParsedCommandArgs parseArguments(final List<String> rawArgs, final Set<String> valuedOptionNames) {
        final Set<String> valued = valuedOptionNames.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        final List<String> positional = new ArrayList<>();
        final Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            final String token = rawArgs.get(i);
            if (!token.startsWith("--")) {
                positional.add(token);
                continue;
            }

            final String body = token.substring(2);
            final int eqIndex = body.indexOf('=');
            String name = eqIndex >= 0 ? body.substring(0, eqIndex) : body;
            String value = eqIndex >= 0 ? body.substring(eqIndex + 1) : null;
            name = name.toLowerCase();
            if (!valued.contains(name)) {
                return ParsedCommandArgs.error("<red>Unknown option: <white>--" + name);
            }
            if (value == null) {
                if (i + 1 >= rawArgs.size() || rawArgs.get(i + 1).startsWith("--")) {
                    return ParsedCommandArgs.error("<red>Missing value for option: <white>--" + name);
                }
                value = rawArgs.get(++i);
            }
            if (value == null || value.trim().isEmpty()) {
                return ParsedCommandArgs.error("<red>Missing value for option: <white>--" + name);
            }
            options.put(name, value.trim());
        }
        return ParsedCommandArgs.success(positional, options);
    }

    protected static final class ParsedCommandArgs {
        private final List<String> positionalArgs;
        private final Map<String, String> valuedOptions;
        private final String error;

        private ParsedCommandArgs(
                final List<String> positionalArgs,
                final Map<String, String> valuedOptions,
                final String error) {
            this.positionalArgs = positionalArgs;
            this.valuedOptions = valuedOptions;
            this.error = error;
        }

        public static ParsedCommandArgs success(final List<String> positionalArgs, final Map<String, String> valuedOptions) {
            return new ParsedCommandArgs(List.copyOf(positionalArgs), Map.copyOf(valuedOptions), null);
        }

        public static ParsedCommandArgs error(final String error) {
            return new ParsedCommandArgs(List.of(), Map.of(), error);
        }

        public boolean hasError() {
            return error != null;
        }

        public String errorMessage() {
            return error;
        }

        public List<String> positionalArgs() {
            return positionalArgs;
        }

        public String optionValue(final String name) {
            return valuedOptions.get(name.toLowerCase());
        }
    }
}
