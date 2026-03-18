package com.cobbletp.tpacobblemon.util;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Utilities for working with player experience points.
 *
 * <p><b>XP cost formula</b> (all values in raw XP points):
 * <pre>
 *   base            = 50 XP  (minimum cost, prevents free spam)
 *   distanceCost    = distance (2-D horizontal, in blocks) × 0.30
 *   crossDimPenalty = +5 000 XP (cross-dimension penalty)
 *   total (same dim)  = clamp(base + distanceCost,                     50, 15 345)
 *   total (cross dim) = clamp(base + distanceCost + crossDimPenalty,   50, 52 220)
 * </pre>
 *
 * <p>Level caps (using vanilla XP table):
 * <ul>
 *   <li>Same dimension  → hard cap at {@code xpForLevel(75)}  = 15 345 XP</li>
 *   <li>Cross dimension → hard cap at {@code xpForLevel(125)} = 52 220 XP</li>
 * </ul>
 *
 * <p>Example costs (same dimension, server players spread ~50 000 blocks from spawn):
 * <ul>
 *   <li>Same spot / nearby (0 blocks)    →    20 XP  (≈ level  3)</li>
 *   <li>500 blocks                        →    95 XP  (≈ level  7)</li>
 *   <li>1 000 blocks                      →   170 XP  (≈ level 10)</li>
 *   <li>5 000 blocks                      →   770 XP  (≈ level 23)</li>
 *   <li>10 000 blocks                     → 1 520 XP  (≈ level 32)</li>
 *   <li>25 000 blocks                     → 3 770 XP  (≈ level 43)</li>
 *   <li>50 000 blocks (typical far)       → 7 520 XP  (≈ level 56)</li>
 *   <li>Cross-dim nearby                  → 2 520 XP  (≈ level 35)</li>
 *   <li>Cross-dim + 50 000 raw blocks     →10 020 XP  (≈ level 64)</li>
 *   <li>Cross-dim max                     →52 220 XP  (= level 125, cap)</li>
 * </ul>
 */
public final class XpUtil {

    /** Minimum cost – prevents free-spam at zero distance. */
    public static final int BASE_COST          = 20;
    /** Raw XP per block of horizontal distance. */
    public static final double DIST_MULTIPLIER = 0.15;
    /** Extra XP added whenever the two players are in different dimensions. */
    public static final int CROSS_DIM_PENALTY  = 2_500;
    /** Hard cap for same-dimension teleports (= xpForLevel(75)). */
    public static final int MAX_COST_SAME      = 15_345;
    /** Hard cap for cross-dimension teleports (= xpForLevel(125)). */
    public static final int MAX_COST_CROSS     = 52_220;

    private XpUtil() {}

    // ──────────────────────────────── Cost calculation ───────────────────────

    /**
     * Calculates the XP cost for a teleport.
     *
     * @param horizontalDistance 2-D (XZ-plane) raw block distance between the two players
     *                           (pass the actual distance even for cross-dimension teleports).
     * @param crossDimension     {@code true} if the players are in different worlds.
     */
    public static int calculateCost(double horizontalDistance, boolean crossDimension) {
        int cost = BASE_COST + (int) (horizontalDistance * DIST_MULTIPLIER);
        if (crossDimension) cost += CROSS_DIM_PENALTY;
        int cap = crossDimension ? MAX_COST_CROSS : MAX_COST_SAME;
        return Math.min(cost, cap);
    }

    // ──────────────────────────────── XP helpers ─────────────────────────────

    /**
     * Computes the player's total accumulated XP from their current level and progress.
     * Minecraft's {@code totalExperience} field drifts; this recalculates it accurately.
     */
    public static int getTotalXp(PlayerEntity player) {
        int level    = player.experienceLevel;
        float progress = player.experienceProgress;
        return xpForLevel(level) + Math.round(progress * xpToNextLevel(level));
    }

    /**
     * Total XP needed to reach {@code level} from zero.
     * Matches vanilla Minecraft experience tables.
     */
    public static int xpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /**
     * XP required to advance from {@code level} to {@code level + 1}.
     */
    public static int xpToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    // ──────────────────────────────── Deduction ──────────────────────────────

    /**
     * Attempts to deduct {@code amount} XP from the player.
     *
     * @return {@code true} if the player had enough XP and it was deducted;
     *         {@code false} if the player lacked sufficient XP (no change made).
     */
    public static boolean deductXp(PlayerEntity player, int amount) {
        if (getTotalXp(player) < amount) return false;
        player.addExperience(-amount);
        return true;
    }
}
