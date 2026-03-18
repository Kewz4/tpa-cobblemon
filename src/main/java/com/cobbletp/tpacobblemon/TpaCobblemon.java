package com.cobbletp.tpacobblemon;

import com.cobbletp.tpacobblemon.command.TpaAcceptCommand;
import com.cobbletp.tpacobblemon.command.TpaCancelCommand;
import com.cobbletp.tpacobblemon.command.TpaCommand;
import com.cobbletp.tpacobblemon.command.TpaDenyCommand;
import com.cobbletp.tpacobblemon.manager.TpaRequestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpaCobblemon implements ModInitializer {

    public static final String MOD_ID = "tpacobblemon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[TPA Cobblemon] Initializing...");

        // Register all commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TpaCommand.register(dispatcher);
            TpaAcceptCommand.register(dispatcher);
            TpaDenyCommand.register(dispatcher);
            TpaCancelCommand.register(dispatcher);
        });

        // Tick-based cleanup of expired requests (runs every second = 20 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                TpaRequestManager.getInstance().cleanupExpired(server);
            }
        });

        LOGGER.info("[TPA Cobblemon] Ready! Use /tpa <player> to teleport (requires a Psychic Pokémon).");
    }
}
