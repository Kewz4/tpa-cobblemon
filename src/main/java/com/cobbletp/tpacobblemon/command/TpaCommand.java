package com.cobbletp.tpacobblemon.command;

import com.cobbletp.tpacobblemon.TpaConfig;
import com.cobbletp.tpacobblemon.TpaRequest;
import com.cobbletp.tpacobblemon.manager.TpaRequestManager;
import com.cobbletp.tpacobblemon.util.CobblemonUtil;
import com.cobbletp.tpacobblemon.util.MessageUtil;
import com.cobbletp.tpacobblemon.util.XpUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * /tpa <player>
 * Sends a teleport request to another online player.
 *
 * Requirements:
 *  1. Sender must not be on cooldown.
 *  2. Sender must have at least one non-fainted Psychic-type OR Flying-type
 *     Pokémon in their party.
 *  3. Sender must have enough XP to cover the distance-based cost.
 *
 * Cost rules:
 *  - Psychic-type → standard cost  (multiplier 1.0)
 *  - Flying-type  → 50 % surcharge (multiplier from config)
 *  - Both types   → Psychic wins (cheaper); player is informed + shown a tip.
 */
public class TpaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tpa")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(TpaCommand::execute))
        );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();

        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity requester = source.getPlayerOrThrow();

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        // ── Self-teleport check ──────────────────────────────────────────────
        if (requester.getUuid().equals(target.getUuid())) {
            requester.sendMessage(MessageUtil.cannotTpaToSelf());
            return 0;
        }

        TpaRequestManager manager = TpaRequestManager.getInstance();

        // ── Cooldown check ───────────────────────────────────────────────────
        if (manager.hasCooldown(requester.getUuid())) {
            requester.sendMessage(MessageUtil.onCooldown(manager.cooldownSecondsRemaining(requester.getUuid())));
            return 0;
        }

        // ── Pokémon check ────────────────────────────────────────────────────
        String psychicPokemon = CobblemonUtil.findPsychicPokemon(requester);
        String flyingPokemon  = CobblemonUtil.findFlyingPokemon(requester);

        if (psychicPokemon == null && flyingPokemon == null) {
            requester.sendMessage(MessageUtil.noPokemon());
            return 0;
        }

        // ── Determine which type to use ──────────────────────────────────────
        // Psychic is always cheaper; prefer it when the player has both.
        final boolean usingFlying  = (psychicPokemon == null);
        final boolean hasBothTypes = (psychicPokemon != null && flyingPokemon != null);
        final String  pokemonName  = usingFlying ? flyingPokemon : psychicPokemon;
        final double  multiplier   = usingFlying ? TpaConfig.get().flyingCostMultiplier : 1.0;

        // ── XP cost calculation ──────────────────────────────────────────────
        boolean crossDimension = !requester.getWorld().getRegistryKey()
                .equals(target.getWorld().getRegistryKey());

        // Always compute raw XZ distance – meaningful even cross-dimension.
        Vec3d rp = requester.getPos();
        Vec3d tp = target.getPos();
        double dx = rp.x - tp.x;
        double dz = rp.z - tp.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        int xpCost = XpUtil.calculateCost(horizontalDistance, crossDimension, multiplier);

        // ── Pre-flight XP check ──────────────────────────────────────────────
        int currentXp = XpUtil.getTotalXp(requester);
        if (currentXp < xpCost) {
            requester.sendMessage(MessageUtil.insufficientXp(xpCost, currentXp));
            return 0;
        }

        // ── Register request ─────────────────────────────────────────────────
        TpaRequest request = new TpaRequest(
                requester.getUuid(), requester.getName().getString(),
                target.getUuid(),    target.getName().getString(),
                xpCost
        );
        manager.addRequest(request, source.getServer());

        // ── Feedback ─────────────────────────────────────────────────────────
        if (hasBothTypes) {
            requester.sendMessage(MessageUtil.usingPsychicOverFlying(psychicPokemon, flyingPokemon));
        } else if (usingFlying) {
            requester.sendMessage(MessageUtil.flyingPokemonFound(pokemonName));
        } else {
            requester.sendMessage(MessageUtil.psychicPokemonFound(pokemonName));
        }

        requester.sendMessage(MessageUtil.requestSent(
                target.getName().getString(), xpCost, TpaRequest.TIMEOUT_MS / 1000L));

        target.sendMessage(MessageUtil.incomingRequest(
                requester.getName().getString(), xpCost, TpaRequest.TIMEOUT_MS / 1000L));

        return 1;
    }
}
