package com.cobbletp.tpacobblemon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent configuration for TPA Cobblemon.
 *
 * <p>Loaded from (and saved to) {@code config/tpacobblemon.json} on the first
 * server start.  Edit the file and restart (or reload) to apply changes.
 *
 * <h3>XP cost formula</h3>
 * <pre>
 *   raw          = baseCost + floor(distance × distMultiplier)
 *                  [+ crossDimPenalty  if cross-dimension]
 *   psychic cost = clamp(raw,          0, maxCostSame / maxCostCross)
 *   flying  cost = clamp(raw × flyingCostMultiplier, 0, maxCostSame / maxCostCross)
 * </pre>
 */
public final class TpaConfig {

    // ── XP cost knobs ────────────────────────────────────────────────────────

    /** Flat XP cost added to every teleport (minimum cost). */
    public int baseCost = 20;

    /** XP added per block of horizontal (XZ) distance. */
    public double distMultiplier = 0.15;

    /** Extra XP penalty when the two players are in different dimensions. */
    public int crossDimPenalty = 2_500;

    /**
     * Hard cap for same-dimension teleports.
     * Default = {@code xpForLevel(75)} = 15 345 XP.
     */
    public int maxCostSame = 15_345;

    /**
     * Hard cap for cross-dimension teleports.
     * Default = {@code xpForLevel(125)} = 52 220 XP.
     */
    public int maxCostCross = 52_220;

    /**
     * Cost multiplier applied when the teleport is channelled by a Flying-type
     * Pokémon instead of a Psychic-type.  1.5 = 50 % more expensive.
     */
    public double flyingCostMultiplier = 1.5;

    // ── Singleton + I/O ─────────────────────────────────────────────────────

    private static TpaConfig instance;

    private TpaConfig() {}

    public static TpaConfig get() {
        return instance;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("tpacobblemon.json");
    }

    /**
     * Loads the config from disk.  If the file does not exist, writes the
     * defaults and uses them for this session.
     */
    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                instance = GSON.fromJson(json, TpaConfig.class);
                TpaCobblemon.LOGGER.info("[TPA Cobblemon] Config loaded from {}", path);
                return;
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                TpaCobblemon.LOGGER.error("[TPA Cobblemon] Failed to read config, using defaults: {}", e.getMessage());
            }
        }
        // File missing or corrupt – write defaults
        instance = new TpaConfig();
        save();
    }

    /** Writes the current config to disk (creates the file if absent). */
    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Default config written to {}", path);
        } catch (IOException e) {
            TpaCobblemon.LOGGER.error("[TPA Cobblemon] Failed to write config: {}", e.getMessage());
        }
    }
}
