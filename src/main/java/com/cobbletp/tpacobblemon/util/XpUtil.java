package com.cobbletp.tpacobblemon.util;

import com.cobbletp.tpacobblemon.TpaConfig;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Utilities for working with player experience points.
 *
 * <p><b>XP cost formula</b> — all tunable values live in {@code config/tpacobblemon.json}:
 * <pre>
 *   raw  = baseCost + floor(distance × distMultiplier)
 *          [+ crossDimPenalty  when cross-dimension]
 *   cost = clamp(floor(raw × multiplier), 0, maxCostSame | maxCostCross)
 * </pre>
 *
 * <p>Default multipliers:
 * <ul>
 *   <li>Psychic-type Pokémon → multiplier 1.0 (standard cost)</li>
 *   <li>Flying-type Pokémon  → multiplier 1.5 (50 % more expensive)</li>
 * </ul>
 *
 * <p>Default example costs (Psychic, same dimension):
 * <ul>
 *   <li>0 blocks      →    20 XP  (≈ level  3)</li>
 *   <li>500 blocks    →    95 XP  (≈ level  7)</li>
 *   <li>1 000 blocks  →   170 XP  (≈ level 10)</li>
 *   <li>10 000 blocks → 1 520 XP  (≈ level 32)</li>
 *   <li>50 000 blocks → 7 520 XP  (≈ level 56)</li>
 *   <li>cap (same)    →15 345 XP  (= level 75)</li>
 *   <li>cap (cross)   →52 220 XP  (= level 125)</li>
 * </ul>
 */
public final class XpUtil {

    private XpUtil() {}

    // ──────────────────────────────── Cost calculation ───────────────────────

    /**
     * Calculates the XP cost for a teleport using the standard (Psychic) multiplier.
     *
     * @param horizontalDistance 2-D (XZ-plane) raw block distance between the two players.
     * @param crossDimension     {@code true} if the players are in different worlds.
     */
    public static int calculateCost(double horizontalDistance, boolean crossDimension) {
        return calculateCost(horizontalDistance, crossDimension, 1.0);
    }

    /**
     * Calculates the XP cost for a teleport with a custom cost multiplier.
     *
     * <p>The multiplier is applied to the raw cost <em>before</em> capping, so
     * Flying-type users ({@code multiplier = flyingCostMultiplier}) reach the
     * cap at shorter distances than Psychic-type users.
     *
     * @param horizontalDistance 2-D (XZ-plane) raw block distance between the two players.
     * @param crossDimension     {@code true} if the players are in different worlds.
     * @param multiplier         cost multiplier (1.0 = standard, 1.5 = Flying penalty, etc.).
     */
    public static int calculateCost(double horizontalDistance, boolean crossDimension, double multiplier) {
        TpaConfig cfg = TpaConfig.get();
        int raw = cfg.baseCost + (int) (horizontalDistance * cfg.distMultiplier);
        if (crossDimension) raw += cfg.crossDimPenalty;
        int cost = (int) (raw * multiplier);
        int cap  = crossDimension ? cfg.maxCostCross : cfg.maxCostSame;
        return Math.min(cost, cap);
    }

    // ──────────────────────────────── XP helpers ─────────────────────────────

    /**
     * Computes the player's total accumulated XP from their current level and progress.
     * Minecraft's {@code totalExperience} field drifts; this recalculates it accurately.
     */
    public static int getTotalXp(PlayerEntity player) {
        int level      = player.experienceLevel;
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
