package com.pvpindex.factions.api;

import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.pvpindex.factions.service.WarpServiceImpl;
import org.bukkit.plugin.Plugin;

/**
 * Lifecycle contract for the optional TeamsAPI integration.
 *
 * <p>This interface intentionally contains no TeamsAPI imports so that it can be
 * referenced from bootstrap code without triggering a {@link NoClassDefFoundError}
 * when TeamsAPI is absent. The concrete implementation
 * ({@link TeamsApiRegistrarImpl}) is loaded exclusively via
 * {@code Class.forName()} after TeamsAPI has been confirmed present.
 */
public interface TeamsApiRegistrar {

    /**
     * Create and register all TeamsAPI provider adapters.
     *
     * @return {@code true} if every adapter was registered successfully
     */
    boolean register(Plugin plugin, FactionServiceImpl factionImpl,
            InviteServiceImpl inviteImpl, WarpServiceImpl warpImpl);

    /**
     * Unregister all previously registered TeamsAPI adapters.
     * Safe to call even if {@link #register} was never called or failed.
     */
    void unregister();
}
