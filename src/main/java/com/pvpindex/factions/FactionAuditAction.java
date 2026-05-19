package com.pvpindex.factions;

import java.util.Optional;

/** Categorized actions recorded in the faction audit log. */
public enum FactionAuditAction {

    CLAIM("claim"),
    UNCLAIM("unclaim"),
    RELATION_CHANGE("relation-change"),
    MEMBER_KICK("kick"),
    MEMBER_PROMOTE("promote"),
    MEMBER_DEMOTE("demote"),
    BANK_DEPOSIT("bank-deposit"),
    BANK_WITHDRAW("bank-withdraw"),
    BANK_TRANSFER("bank-transfer");

    private final String id;

    FactionAuditAction(final String id) {
        this.id = id;
    }

    /** Lower-case kebab-case identifier used in storage and command filters. */
    public String getId() {
        return id;
    }

    /**
     * Resolve a {@link FactionAuditAction} from its {@link #getId()} string.
     *
     * @param id case-insensitive action id
     * @return matching action, or empty if none matches
     */
    public static Optional<FactionAuditAction> fromId(final String id) {
        for (final FactionAuditAction action : values()) {
            if (action.id.equalsIgnoreCase(id)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    /** Comma-separated list of all valid IDs, used in error messages. */
    public static String validIds() {
        final StringBuilder sb = new StringBuilder();
        for (final FactionAuditAction action : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(action.id);
        }
        return sb.toString();
    }
}
