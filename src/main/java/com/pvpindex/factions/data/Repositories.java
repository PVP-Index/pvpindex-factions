package com.pvpindex.factions.data;

import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.pvpindex.factions.data.repository.BankTransactionRepository;
import com.pvpindex.factions.data.repository.BoardRepository;
import com.pvpindex.factions.data.repository.FactionInboxRepository;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.InvitationRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.data.repository.PowerHistoryRepository;
import com.pvpindex.factions.data.repository.RankRepository;
import com.pvpindex.factions.data.repository.WarpRepository;

/**
 * Convenience container that holds all repository instances.
 *
 * <p>Constructed once in {@code PvPIndexFactions#onEnable()} after the
 * {@link DatabaseManager} is initialized, then passed to every service that
 * needs persistence access.
 */
public class Repositories {

    private final FactionRepository factions;
    private final PlayerRepository players;
    private final BoardRepository board;
    private final WarpRepository warps;
    private final InvitationRepository invitations;
    private final RankRepository ranks;
    private final BankTransactionRepository bankTransactions;
    private final PowerHistoryRepository powerHistory;
    private final FactionInboxRepository inbox;

    public Repositories(final DataSourceJdbcStore store) {
        this.factions = new FactionRepository(store);
        this.players = new PlayerRepository(store);
        this.board = new BoardRepository(store);
        this.warps = new WarpRepository(store);
        this.invitations = new InvitationRepository(store);
        this.ranks = new RankRepository(store);
        this.bankTransactions = new BankTransactionRepository(store);
        this.powerHistory = new PowerHistoryRepository(store);
        this.inbox = new FactionInboxRepository(store);
    }

    public FactionRepository factions() {
        return factions;
    }

    public PlayerRepository players() {
        return players;
    }

    public BoardRepository board() {
        return board;
    }

    public WarpRepository warps() {
        return warps;
    }

    public InvitationRepository invitations() {
        return invitations;
    }

    public RankRepository ranks() {
        return ranks;
    }

    public BankTransactionRepository bankTransactions() {
        return bankTransactions;
    }

    public PowerHistoryRepository powerHistory() {
        return powerHistory;
    }

    public FactionInboxRepository inbox() {
        return inbox;
    }
}
