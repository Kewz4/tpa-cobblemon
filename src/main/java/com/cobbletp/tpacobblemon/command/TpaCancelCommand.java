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
 * /tpacancel
 * Cancels the requester's own outgoing TPA request (if any).
 */
public class TpaCancelCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpacancel").executes(TpaCancelCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();

        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity requester = source.getPlayerOrThrow();

        TpaRequestManager manager = TpaRequestManager.getInstance();
        TpaRequest request = manager.getByRequester(requester.getUuid());

        if (request == null) {
            requester.sendMessage(MessageUtil.noOutgoingRequest());
            return 0;
        }

        manager.remove(request);

        // Confirm to requester
        requester.sendMessage(MessageUtil.requestCancelled(request.targetName));

        // Notify target if still online
        ServerPlayerEntity target = source.getServer()
                .getPlayerManager().getPlayer(request.targetUuid);
        if (target != null) {
            target.sendMessage(MessageUtil.requestCancelledByRequester(requester.getName().getString()));
        }

        return 1;
    }
}
