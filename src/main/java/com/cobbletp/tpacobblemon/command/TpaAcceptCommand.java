package com.cobbletp.tpacobblemon.command;

import com.cobbletp.tpacobblemon.TpaRequest;
import com.cobbletp.tpacobblemon.manager.TpaRequestManager;
import com.cobbletp.tpacobblemon.util.MessageUtil;
import com.cobbletp.tpacobblemon.util.XpUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * /tpaaccept
 * Accepts the pending incoming TPA request for this player.
 * Can also be triggered by clicking the [Accept] button in chat.
 *
 * On acceptance:
 *  1. Re-validates that the requester is still online.
 *  2. Re-validates that the requester still has enough XP (may have changed).
 *  3. Deducts XP from the requester.
 *  4. Teleports the requester to the target (cross-dimension aware).
 *  5. Applies a cooldown on the requester.
 */
public class TpaAcceptCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpaaccept").executes(TpaAcceptCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();

        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity target = source.getPlayerOrThrow();

        TpaRequestManager manager = TpaRequestManager.getInstance();
        TpaRequest request = manager.getByTarget(target.getUuid());

        if (request == null) {
            target.sendMessage(MessageUtil.noIncomingRequest());
            return 0;
        }

        // ── Requester still online? ──────────────────────────────────────────
        ServerPlayerEntity requester = source.getServer()
                .getPlayerManager().getPlayer(request.requesterUuid);
        if (requester == null) {
            manager.remove(request);
            target.sendMessage(MessageUtil.requesterOffline(request.requesterName));
            return 0;
        }

        // ── Requester still has enough XP? ──────────────────────────────────
        int currentXp = XpUtil.getTotalXp(requester);
        if (currentXp < request.xpCost) {
            manager.remove(request);
            target.sendMessage(MessageUtil.requesterInsufficientXp(request.requesterName));
            requester.sendMessage(MessageUtil.insufficientXpOnAccept(request.xpCost, currentXp));
            return 0;
        }

        // ── Commit ───────────────────────────────────────────────────────────
        manager.remove(request);

        // Deduct XP from requester
        XpUtil.deductXp(requester, request.xpCost);

        // Teleport requester to target (handles cross-dimension)
        ServerWorld destWorld = (ServerWorld) target.getWorld();
        requester.teleport(
                destWorld,
                target.getX(), target.getY(), target.getZ(),
                target.getYaw(), target.getPitch()
        );

        // Start cooldown so the requester can't spam TPA
        manager.setCooldown(request.requesterUuid);

        // ── Feedback ─────────────────────────────────────────────────────────
        requester.sendMessage(MessageUtil.requestAccepted(target.getName().getString(), request.xpCost));
        target.sendMessage(MessageUtil.targetReceivedTeleport(requester.getName().getString()));

        return 1;
    }
}
