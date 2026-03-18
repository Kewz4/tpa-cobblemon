package com.cobbletp.tpacobblemon;

import java.util.UUID;

/**
 * Represents a pending TPA request from one player to another.
 */
public class TpaRequest {

    /** How long a request stays active before auto-expiring. */
    public static final long TIMEOUT_MS = 60_000L; // 60 seconds

    public final UUID requesterUuid;
    public final String requesterName;
    public final UUID targetUuid;
    public final String targetName;

    /** Pre-calculated XP cost at the time the request was created. */
    public final int xpCost;

    private final long expiresAt;

    public TpaRequest(UUID requesterUuid, String requesterName,
                      UUID targetUuid,   String targetName,
                      int xpCost) {
        this.requesterUuid = requesterUuid;
        this.requesterName = requesterName;
        this.targetUuid    = targetUuid;
        this.targetName    = targetName;
        this.xpCost        = xpCost;
        this.expiresAt     = System.currentTimeMillis() + TIMEOUT_MS;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    /** Seconds remaining before this request expires (never negative). */
    public long secondsRemaining() {
        return Math.max(0L, (expiresAt - System.currentTimeMillis()) / 1000L);
    }
}
