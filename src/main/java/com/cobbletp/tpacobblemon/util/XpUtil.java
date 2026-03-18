package com.cobbletp.tpacobblemon.util;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Utilities for working with player experience points.
 *
 * <p><b>XP cost formula</b> (all values in raw XP points):
 * <pre>
 *   base            = 50 XP  (minimum cost, prevents free spam)
 *   distanceCost    = distance (2-D horizontal, in blocks) × 0.10
 *   crossDimPenalty = +300 XP (nether/end teleports are expensive)
 *   total           = clamp(base + distanceCost [+ crossDimPenalty], 50, 800)
 * </pre>
 *
 * <p>Example costs at sea level:
 * <ul>
 *   <li>Same spot              →  50 XP  (≈ level 4)</li>
 *   <li>100 blocks             →  60 XP  (≈ level 5)</li>
 *   <li>500 blocks             → 100 XP  (≈ level 7)</li>
 *   <li>1 000 blocks           → 150 XP  (≈ level 9)</li>
 *   <li>3 000 blocks           → 350 XP  (≈ level 16)</li>
 *   <li>5 000 blocks           → 550 XP  (≈ level 20, capped)</li>
 *   <li>Cross-dimension nearby → 350 XP</li>
 *   <li>Cross-dimension + far  → 800 XP  (hard cap)</li>
 * </ul>
 */
public final class XpUtil {

    public static final int BASE_COST              = 50;
    public static final double DIST_MULTIPLIER     = 0.10;   // XP per block
    public static final int CROSS_DIM_PENALTY      = 300;
    public static final int MAX_COST               = 800;

    private XpUtil() {}

    // ──────────────────────────────── Cost calculation ───────────────────────

    /**
     * Calculates the XP cost for a teleport.
     *
     * @param horizontalDistance 2-D (XZ-plane) block distance between the two players;
     *                           ignored when {@code crossDimension} is {@code true}.
     * @param crossDimension     {@code true} if the players are in different worlds.
     */
    public static int calculateCost(double horizontalDistance, boolean crossDimension) {
        int cost = BASE_COST + (int) (horizontalDistance * DIST_MULTIPLIER);
        if (crossDimension) cost += CROSS_DIM_PENALTY;
        return Math.min(cost, MAX_COST);
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
