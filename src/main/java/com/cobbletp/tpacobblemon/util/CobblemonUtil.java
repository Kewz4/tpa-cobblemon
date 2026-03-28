package com.cobbletp.tpacobblemon.util;

import com.cobbletp.tpacobblemon.TpaCobblemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

/**
 * Cobblemon API wrapper using reflection so the mod compiles (and loads) even if
 * Cobblemon is absent.  At runtime on a Cobblemon server all calls will succeed
 * normally; on a server without Cobblemon all {@code find*} methods return null.
 *
 * Tested against Cobblemon 1.6.x (Fabric, 1.21.1).
 */
public final class CobblemonUtil {

    private CobblemonUtil() {}

    // Cached reflection handles – resolved once on first call
    private static volatile boolean resolved = false;
    private static Object cobblemonInstance;
    private static Method getStorage;
    private static Method getParty;
    private static Method isFainted;
    private static Method getTypes;
    private static Method getDisplayName;
    private static Method getString;
    private static Method getTypeName;

    /**
     * Attempts to load all required Cobblemon reflection targets.
     *
     * @return {@code true} if Cobblemon is present and all methods were found.
     */
    /**
     * Tries to load a class by name using several class loaders in order:
     * our own loader (KnotClassLoader), the thread context loader, and the
     * system loader.  This covers every realistic Fabric deployment layout.
     */
    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader[] loaders = {
            CobblemonUtil.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try { return Class.forName(name, true, cl); }
            catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(name);
    }

    private static boolean init() {
        if (resolved) return cobblemonInstance != null;
        resolved = true;
        try {
            Class<?> cobblemonCls     = loadClass("com.cobblemon.mod.common.Cobblemon");
            Class<?> storageCls       = loadClass("com.cobblemon.mod.common.api.storage.StorageManager");
            Class<?> pokemonCls       = loadClass("com.cobblemon.mod.common.pokemon.Pokemon");
            Class<?> elementalTypeCls = loadClass("com.cobblemon.mod.common.api.types.ElementalType");

            // Resolve all methods before touching cobblemonInstance so that
            // a partial failure leaves cobblemonInstance null and init() returns
            // false on subsequent calls rather than silently NPE-ing.
            Object instance = cobblemonCls.getField("INSTANCE").get(null);
            getStorage     = cobblemonCls.getMethod("getStorage");
            getParty       = storageCls.getMethod("getParty", ServerPlayerEntity.class);
            isFainted      = pokemonCls.getMethod("isFainted");
            getTypes       = pokemonCls.getMethod("getTypes");
            getDisplayName = pokemonCls.getMethod("getDisplayName");
            getString      = loadClass("net.minecraft.text.MutableText").getMethod("getString");
            getTypeName    = elementalTypeCls.getMethod("getName");
            cobblemonInstance = instance; // only set after everything succeeded

            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Cobblemon API linked successfully.");
            return true;
        } catch (ClassNotFoundException e) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Cobblemon class not found: '{}' – TPA will be unavailable.", e.getMessage());
        } catch (Exception e) {
            TpaCobblemon.LOGGER.error("[TPA Cobblemon] Failed to link Cobblemon API: {}", e.toString());
        }
        // Reset so the next attempt (e.g. on first /tpa) can retry.
        resolved = false;
        return false;
    }

    /**
     * Called once at server-start to eagerly link the Cobblemon API and emit
     * detailed diagnostic output.  Logs every step so failures are immediately
     * visible in the server log without needing to run a command first.
     */
    public static void diagnose() {
        TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- Cobblemon link diagnostic ---");

        // Reset so diagnose() always does a fresh attempt.
        resolved = false;
        cobblemonInstance = null;

        String[] classes = {
            "com.cobblemon.mod.common.Cobblemon",
            "com.cobblemon.mod.common.api.storage.StorageManager",
            "com.cobblemon.mod.common.pokemon.Pokemon",
            "com.cobblemon.mod.common.api.types.ElementalType",
            "net.minecraft.text.MutableText"
        };
        boolean allClassesFound = true;
        for (String cls : classes) {
            try {
                loadClass(cls);
                TpaCobblemon.LOGGER.info("[TPA Cobblemon]   [OK] class found: {}", cls);
            } catch (ClassNotFoundException e) {
                TpaCobblemon.LOGGER.warn("[TPA Cobblemon]   [MISSING] class not found: {}", cls);
                allClassesFound = false;
            }
        }

        if (!allClassesFound) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] One or more Cobblemon classes are missing.");
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Make sure Cobblemon is installed and the jar is in the mods folder.");
            TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- end diagnostic ---");
            return;
        }

        // All classes found – now attempt full link.
        if (init()) {
            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Full link successful – party type detection is active.");
        } else {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Classes found but API link failed (see errors above).");
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Check that your Cobblemon version matches the expected API.");
        }
        TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- end diagnostic ---");
    }

    /**
     * Finds the first non-fainted Pokémon of the given elemental type in the
     * player's party.
     *
     * @param typeName case-insensitive Cobblemon type name, e.g. {@code "psychic"}
     *                 or {@code "flying"}.
     * @return display name of the matching Pokémon (e.g. {@code "Ralts"}),
     *         or {@code null} if none found (or Cobblemon is not installed).
     */
    public static String findPokemonOfType(ServerPlayerEntity player, String typeName) {
        if (!init()) return null;
        try {
            Object storage = getStorage.invoke(cobblemonInstance);
            Object party   = getParty.invoke(storage, player);
            if (party == null) return null;

            for (Object pokemon : (Iterable<?>) party) {
                if (pokemon == null) continue;
                if ((boolean) isFainted.invoke(pokemon)) continue;

                for (Object type : (Iterable<?>) getTypes.invoke(pokemon)) {
                    String name = (String) getTypeName.invoke(type);
                    if (typeName.equalsIgnoreCase(name)) {
                        Object displayName = getDisplayName.invoke(pokemon);
                        return (String) getString.invoke(displayName);
                    }
                }
            }
        } catch (Exception e) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Error reading Cobblemon party: " + e);
        }
        return null;
    }

    /** Finds the first non-fainted Psychic-type Pokémon in the player's party. */
    public static String findPsychicPokemon(ServerPlayerEntity player) {
        return findPokemonOfType(player, "psychic");
    }

    /** Finds the first non-fainted Flying-type Pokémon in the player's party. */
    public static String findFlyingPokemon(ServerPlayerEntity player) {
        return findPokemonOfType(player, "flying");
    }

    /** @return {@code true} if the player has at least one non-fainted Psychic-type Pokémon. */
    public static boolean hasPsychicPokemon(ServerPlayerEntity player) {
        return findPsychicPokemon(player) != null;
    }
}
