package com.cobbletp.tpacobblemon.command;

import com.cobbletp.tpacobblemon.TpaRequest;
import com.cobbletp.tpacobblemon.manager.TpaRequestManager;
import com.cobbletp.tpacobblemon.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * /tpadeny
 * Denies the pending incoming TPA request for this player.
 * Can also be triggered by clicking the [Deny] button in chat.
 */
public class TpaDenyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpadeny").executes(TpaDenyCommand::execute));
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

        manager.remove(request);

        // Confirm to the denier
        target.sendMessage(MessageUtil.youDenied(request.requesterName));

        // Notify requester if still online
        ServerPlayerEntity requester = source.getServer()
                .getPlayerManager().getPlayer(request.requesterUuid);
        if (requester != null) {
            requester.sendMessage(MessageUtil.requestDenied(target.getName().getString()));
        }

        return 1;
    }
}
