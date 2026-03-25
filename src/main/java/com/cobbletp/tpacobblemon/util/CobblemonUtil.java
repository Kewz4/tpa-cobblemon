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
    private static boolean init() {
        if (resolved) return cobblemonInstance != null;
        resolved = true;
        try {
            // Use the thread context class loader so Fabric's KnotClassLoader
            // is used, which has access to all loaded mod classes.
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> cobblemonCls     = Class.forName("com.cobblemon.mod.common.Cobblemon", true, cl);
            Class<?> storageCls       = Class.forName("com.cobblemon.mod.common.api.storage.StorageManager", true, cl);
            Class<?> pokemonCls       = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon", true, cl);
            Class<?> elementalTypeCls = Class.forName("com.cobblemon.mod.common.api.types.ElementalType", true, cl);

            // Resolve all methods before touching cobblemonInstance so that
            // a partial failure leaves cobblemonInstance null and init() returns
            // false on subsequent calls rather than silently NPE-ing.
            Object instance = cobblemonCls.getField("INSTANCE").get(null);
            getStorage     = cobblemonCls.getMethod("getStorage");
            getParty       = storageCls.getMethod("getParty", ServerPlayerEntity.class);
            isFainted      = pokemonCls.getMethod("isFainted");
            getTypes       = pokemonCls.getMethod("getTypes");
            getDisplayName = pokemonCls.getMethod("getDisplayName");
            getString      = Class.forName("net.minecraft.text.MutableText", true, cl).getMethod("getString");
            getTypeName    = elementalTypeCls.getMethod("getName");
            cobblemonInstance = instance; // only set after everything succeeded

            TpaCobblemon.LOGGER.info("[TPA Cobblemon] Cobblemon API linked successfully.");
            return true;
        } catch (ClassNotFoundException e) {
            TpaCobblemon.LOGGER.warn("[TPA Cobblemon] Cobblemon not found – TPA will be unavailable.");
        } catch (Exception e) {
            TpaCobblemon.LOGGER.error("[TPA Cobblemon] Failed to link Cobblemon API: " + e.getMessage());
        }
        return false;
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
