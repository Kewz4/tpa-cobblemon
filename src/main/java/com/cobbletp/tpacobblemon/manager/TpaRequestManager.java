package com.cobbletp.tpacobblemon.manager;

import com.cobbletp.tpacobblemon.TpaRequest;
import com.cobbletp.tpacobblemon.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton that tracks all pending TPA requests and per-player cooldowns.
 *
 * Design constraints (prevents spam / edge cases):
 * - A player can only have ONE outgoing request at a time.
 *   Sending a new one cancels the previous automatically.
 * - A player can only have ONE incoming request at a time.
 *   If a second arrives while one is pending, the older one is cancelled for the target.
 * - After a successful teleport the REQUESTER enters a 30-second cooldown.
 */
public class TpaRequestManager {

    private static final TpaRequestManager INSTANCE = new TpaRequestManager();

    /** How long (ms) a player must wait after a successful teleport before sending another request. */
    public static final long COOLDOWN_MS = 30_000L;

    // keyed by target UUID  → the request someone sent TO that target
    private final Map<UUID, TpaRequest> byTarget    = new HashMap<>();
    // keyed by requester UUID → the request that player sent
    private final Map<UUID, TpaRequest> byRequester = new HashMap<>();
    // keyed by requester UUID → epoch-ms when their cooldown ends
    private final Map<UUID, Long>       cooldowns   = new HashMap<>();

    private TpaRequestManager() {}

    public static TpaRequestManager getInstance() {
        return INSTANCE;
    }

    // ──────────────────────────────── Cooldown ───────────────────────────────

    public boolean hasCooldown(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /** Remaining cooldown in whole seconds (0 if none). */
    public long cooldownSecondsRemaining(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        if (expiry == null) return 0L;
        return Math.max(0L, (expiry - System.currentTimeMillis()) / 1000L);
    }

    /** Start the post-teleport cooldown for a requester. */
    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS);
    }

    // ──────────────────────────────── Request lifecycle ──────────────────────

    /**
     * Registers a new TPA request.
     * If the requester already has an outgoing request it is cancelled first.
     * If the target already has an incoming request it is replaced (old requester notified).
     */
    public void addRequest(TpaRequest request, MinecraftServer server) {
        // Cancel any existing outgoing request from the same requester
        TpaRequest prevOut = byRequester.get(request.requesterUuid);
        if (prevOut != null) {
            byTarget.remove(prevOut.targetUuid);
            // Notify previous target that the request was cancelled
            ServerPlayerEntity prevTarget = server.getPlayerManager().getPlayer(prevOut.targetUuid);
            if (prevTarget != null) {
                prevTarget.sendMessage(MessageUtil.requestCancelledByRequester(request.requesterName));
            }
        }

        // Cancel any existing incoming request aimed at the same target
        TpaRequest prevIn = byTarget.get(request.targetUuid);
        if (prevIn != null) {
            byRequester.remove(prevIn.requesterUuid);
            // Notify the old requester their request was replaced
            ServerPlayerEntity prevRequester = server.getPlayerManager().getPlayer(prevIn.requesterUuid);
            if (prevRequester != null) {
                prevRequester.sendMessage(MessageUtil.requestReplacedBySomeoneElse(prevIn.targetName));
            }
        }

        byTarget.put(request.targetUuid, request);
        byRequester.put(request.requesterUuid, request);
    }

    /**
     * Returns the pending incoming request for {@code targetUuid}, or {@code null}
     * if there is none (or the request has expired — it is removed automatically).
     */
    public TpaRequest getByTarget(UUID targetUuid) {
        TpaRequest req = byTarget.get(targetUuid);
        if (req != null && req.isExpired()) {
            remove(req);
            return null;
        }
        return req;
    }

    /**
     * Returns the pending outgoing request from {@code requesterUuid}, or {@code null}
     * if there is none (or the request has expired — it is removed automatically).
     */
    public TpaRequest getByRequester(UUID requesterUuid) {
        TpaRequest req = byRequester.get(requesterUuid);
        if (req != null && req.isExpired()) {
            remove(req);
            return null;
        }
        return req;
    }

    /** Removes the request from both maps. */
    public void remove(TpaRequest request) {
        byTarget.remove(request.targetUuid);
        byRequester.remove(request.requesterUuid);
    }

    // ──────────────────────────────── Tick cleanup ───────────────────────────

    /**
     * Called every second from a server tick event.
     * Removes expired requests and notifies both parties.
     */
    public void cleanupExpired(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TpaRequest>> it = byRequester.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = it.next();
            TpaRequest req = entry.getValue();
            if (req.isExpired()) {
                byTarget.remove(req.targetUuid);
                it.remove();

                // Notify requester (if still online)
                ServerPlayerEntity requester = server.getPlayerManager().getPlayer(req.requesterUuid);
                if (requester != null) {
                    requester.sendMessage(MessageUtil.requestExpiredForRequester(req.targetName));
                }
                // Notify target (if still online)
                ServerPlayerEntity target = server.getPlayerManager().getPlayer(req.targetUuid);
                if (target != null) {
                    target.sendMessage(MessageUtil.requestExpiredForTarget(req.requesterName));
                }
            }
        }

        // Prune stale cooldown entries
        cooldowns.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
    }
}
