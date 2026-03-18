package com.cobbletp.tpacobblemon.util;

import com.cobbletp.tpacobblemon.TpaCobblemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

/**
 * Cobblemon API wrapper using reflection so the mod compiles (and loads) even if
 * Cobblemon is absent.  At runtime on a Cobblemon server all calls will succeed
 * normally; on a server without Cobblemon {@link #findPsychicPokemon} returns null.
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
            Class<?> cobblemonCls    = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Class<?> storageCls      = Class.forName("com.cobblemon.mod.common.api.storage.StorageManager");
            Class<?> pokemonCls      = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            Class<?> elementalTypeCls= Class.forName("com.cobblemon.mod.common.api.types.ElementalType");

            cobblemonInstance = cobblemonCls.getField("INSTANCE").get(null);
            getStorage    = cobblemonCls.getMethod("getStorage");
            getParty      = storageCls.getMethod("getParty", ServerPlayerEntity.class);
            isFainted     = pokemonCls.getMethod("isFainted");
            getTypes      = pokemonCls.getMethod("getTypes");
            getDisplayName= pokemonCls.getMethod("getDisplayName");
            getString     = Class.forName("net.minecraft.text.MutableText")
                                .getMethod("getString");
            getTypeName   = elementalTypeCls.getMethod("getName");

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
     * Finds the first non-fainted Psychic-type Pokémon in the player's party.
     *
     * @return the display name of the Psychic Pokémon (e.g. "Ralts"), or
     *         {@code null} if the player has none (or Cobblemon is not installed).
     */
    public static String findPsychicPokemon(ServerPlayerEntity player) {
        if (!init()) return null;
        try {
            Object storage = getStorage.invoke(cobblemonInstance);
            Object party   = getParty.invoke(storage, player);
            if (party == null) return null;

            for (Object pokemon : (Iterable<?>) party) {
                if (pokemon == null) continue;

                boolean fainted = (boolean) isFainted.invoke(pokemon);
                if (fainted) continue;

                Iterable<?> types = (Iterable<?>) getTypes.invoke(pokemon);
                for (Object type : types) {
                    String name = (String) getTypeName.invoke(type);
                    if ("psychic".equalsIgnoreCase(name)) {
                        Object displayName = getDisplayName.invoke(pokemon);
                        return (String) getString.invoke(displayName);
                    }
                }
            }
        } catch (Exception e) {
            TpaCobblemon.LOGGER.debug("[TPA Cobblemon] Error reading Cobblemon party: " + e.getMessage());
        }
        return null;
    }

    /**
     * @return {@code true} if the player has at least one non-fainted Psychic-type Pokémon.
     */
    public static boolean hasPsychicPokemon(ServerPlayerEntity player) {
        return findPsychicPokemon(player) != null;
    }
}
