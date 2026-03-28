package com.cobbletp.tpacobblemon.util;

import com.cobbletp.tpacobblemon.TpaCobblemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

/**
 * Cobblemon API wrapper using reflection so the mod compiles (and loads) even if
 * Cobblemon is absent.  At runtime on a Cobblemon server all calls will succeed
 * normally; on a server without Cobblemon all {@code find*} methods return null.
 */
public final class CobblemonUtil {

    private CobblemonUtil() {}

    // Cached reflection handles – resolved once on first call
    private static volatile boolean resolved = false;
    private static Object cobblemonInstance;
    private static Method getStorage;
    private static Method getParty;
    private static boolean getPartyTakesPlayer; // true = ServerPlayerEntity, false = UUID
    private static Method isFainted;
    private static Method getTypes;
    private static Method getSpecies;
    private static Method getTypeName;
    // getSpeciesName is resolved lazily from the actual Species object at runtime.
    private static Method getSpeciesName;

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
            Class<?> pokemonCls       = loadClass("com.cobblemon.mod.common.pokemon.Pokemon");
            Class<?> elementalTypeCls = loadClass("com.cobblemon.mod.common.api.types.ElementalType");

            Object instance = cobblemonCls.getField("INSTANCE").get(null);
            getStorage = cobblemonCls.getMethod("getStorage");

            // Find getParty on the actual storage object rather than by class name,
            // since StorageManager's package differs across Cobblemon versions.
            Object storageObj = getStorage.invoke(instance);
            try {
                getParty = storageObj.getClass().getMethod("getParty", ServerPlayerEntity.class);
                getPartyTakesPlayer = true;
            } catch (NoSuchMethodException e) {
                getParty = storageObj.getClass().getMethod("getParty", java.util.UUID.class);
                getPartyTakesPlayer = false;
            }

            isFainted  = pokemonCls.getMethod("isFainted");
            getTypes   = pokemonCls.getMethod("getTypes");
            getSpecies = pokemonCls.getMethod("getSpecies");
            // getSpeciesName resolved lazily from the actual Species object (avoids hardcoding class name).
            getTypeName = elementalTypeCls.getMethod("getName");
            cobblemonInstance = instance; // only set after everything succeeded

            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Cobblemon API linked successfully (getParty takes {}).",
                    getPartyTakesPlayer ? "ServerPlayerEntity" : "UUID");
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
     * detailed diagnostic output.
     */
    public static void diagnose() {
        TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- Cobblemon link diagnostic ---");

        // Reset so diagnose() always does a fresh attempt.
        resolved = false;
        cobblemonInstance = null;

        // These are the classes we still look up by name.
        String[] namedClasses = {
            "com.cobblemon.mod.common.Cobblemon",
            "com.cobblemon.mod.common.pokemon.Pokemon",
            "com.cobblemon.mod.common.api.types.ElementalType"
        };
        boolean allFound = true;
        for (String cls : namedClasses) {
            try {
                loadClass(cls);
                TpaCobblemon.LOGGER.info("[TPA Cobblemon]   [OK] class found: {}", cls);
            } catch (ClassNotFoundException e) {
                TpaCobblemon.LOGGER.warn("[TPA Cobblemon]   [MISSING] class not found: {}", cls);
                allFound = false;
            }
        }

        if (!allFound) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] One or more required classes are missing.");
            TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- end diagnostic ---");
            return;
        }

        if (init()) {
            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Full link successful – party type detection is active.");
        } else {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Classes found but API link failed (see errors above).");
        }
        TpaCobblemon.LOGGER.info("[TPA Cobblemon] --- end diagnostic ---");
    }

    /**
     * Finds the first non-fainted Pokémon of the given elemental type in the
     * player's party.
     *
     * @param typeName case-insensitive Cobblemon type name, e.g. {@code "psychic"}
     *                 or {@code "flying"}.
     * @return display name of the matching Pokémon, or {@code null} if none found.
     */
    public static String findPokemonOfType(ServerPlayerEntity player, String typeName) {
        if (!init()) return null;
        try {
            Object storage = getStorage.invoke(cobblemonInstance);
            Object party   = getPartyTakesPlayer
                    ? getParty.invoke(storage, player)
                    : getParty.invoke(storage, player.getUuid());
            if (party == null) return null;

            for (Object pokemon : (Iterable<?>) party) {
                if (pokemon == null) continue;
                if ((boolean) isFainted.invoke(pokemon)) continue;

                for (Object type : (Iterable<?>) getTypes.invoke(pokemon)) {
                    String name = (String) getTypeName.invoke(type);
                    if (typeName.equalsIgnoreCase(name)) {
                        Object species = getSpecies.invoke(pokemon);
                        // Resolve getName lazily from the actual Species class (avoids hardcoding it).
                        if (getSpeciesName == null) {
                            getSpeciesName = species.getClass().getMethod("getName");
                        }
                        return (String) getSpeciesName.invoke(species);
                    }
                }
            }
        } catch (Exception e) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Error reading Cobblemon party: {}", e.toString());
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
