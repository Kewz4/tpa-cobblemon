package com.cobbletp.tpacobblemon.util;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

/**
 * Factory for all player-facing text components used by TPA Cobblemon.
 * Every method returns a {@link Text} instance ready to be sent to a player.
 */
public final class MessageUtil {

    private MessageUtil() {}

    // ─────────────────────────────── Shared helpers ──────────────────────────

    /** Coloured "[TPA] " prefix used at the start of every message. */
    private static MutableText prefix() {
        return Text.literal("[TPA] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
    }

    /** Thin horizontal divider for bordered notification boxes. */
    private static MutableText divider() {
        return Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_PURPLE);
    }

    // ─────────────────────────────── Requester messages ──────────────────────

    /** Shown when the requester has no usable Psychic or Flying Pokémon. */
    public static Text noPokemon() {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("You need a ").formatted(Formatting.GRAY))
                .append(Text.literal("Psychic").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" or ").formatted(Formatting.GRAY))
                .append(Text.literal("Flying").formatted(Formatting.AQUA))
                .append(Text.literal("-type Pokémon in your party to use TPA!").formatted(Formatting.GRAY));
    }

    /** Shown to confirm which Psychic Pokémon is channelling the teleport. */
    public static Text psychicPokemonFound(String pokemonName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Your ").formatted(Formatting.GRAY))
                .append(Text.literal(pokemonName).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC))
                .append(Text.literal(" is channelling psychic energy...").formatted(Formatting.GRAY));
    }

    /** Shown when the teleport is channelled by a Flying-type Pokémon. */
    public static Text flyingPokemonFound(String pokemonName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Your ").formatted(Formatting.GRAY))
                .append(Text.literal(pokemonName).formatted(Formatting.AQUA, Formatting.ITALIC))
                .append(Text.literal(" is carrying you through the skies...").formatted(Formatting.GRAY))
                .append(Text.literal(" (Flying-type costs 50% more XP)").formatted(Formatting.YELLOW));
    }

    /**
     * Shown when the player has both a Psychic and a Flying Pokémon.
     * Psychic is used (cheaper); player is informed and given a tip.
     */
    public static Text usingPsychicOverFlying(String psychicName, String flyingName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Your ").formatted(Formatting.GRAY))
                .append(Text.literal(psychicName).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC))
                .append(Text.literal(" is channelling psychic energy").formatted(Formatting.GRAY))
                .append(Text.literal(" (cheaper than ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(flyingName).formatted(Formatting.AQUA, Formatting.ITALIC))
                .append(Text.literal(").\n").formatted(Formatting.DARK_GRAY))
                .append(prefix())
                .append(Text.literal("TIP: ").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("Having a ").formatted(Formatting.GRAY))
                .append(Text.literal("Psychic").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("-type Pokémon is cheaper than ").formatted(Formatting.GRAY))
                .append(Text.literal("Flying").formatted(Formatting.AQUA))
                .append(Text.literal("!").formatted(Formatting.GRAY));
    }

    /** Shown when the requester is on cooldown. */
    public static Text onCooldown(long secondsRemaining) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("TPA is on cooldown! ").formatted(Formatting.RED))
                .append(Text.literal("(" + secondsRemaining + "s remaining)").formatted(Formatting.GRAY));
    }

    /** Shown when the requester cannot afford the XP cost. */
    public static Text insufficientXp(int cost, int currentXp) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Not enough XP! Cost: ").formatted(Formatting.RED))
                .append(Text.literal(cost + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal(" | You have: ").formatted(Formatting.RED))
                .append(Text.literal(currentXp + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal(".").formatted(Formatting.RED));
    }

    /** Confirmation after a request is successfully dispatched. */
    public static Text requestSent(String targetName, int xpCost, long expiresSeconds) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Request sent to ").formatted(Formatting.GRAY))
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal("  •  Cost: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(xpCost + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal("  •  Expires: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(expiresSeconds + "s").formatted(Formatting.GRAY));
    }

    /** Shown to the requester when the target accepts. */
    public static Text requestAccepted(String targetName, int xpCost) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal(" accepted! Teleporting... ").formatted(Formatting.GREEN))
                .append(Text.literal("(-" + xpCost + " XP)").formatted(Formatting.YELLOW));
    }

    /** Shown to the requester when the target denies. */
    public static Text requestDenied(String targetName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal(" denied your teleport request.").formatted(Formatting.RED));
    }

    /** Shown to the requester when their request expires. */
    public static Text requestExpiredForRequester(String targetName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Your request to ").formatted(Formatting.GRAY))
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal(" has expired.").formatted(Formatting.DARK_GRAY));
    }

    /** Shown to the requester after they cancel via /tpacancel. */
    public static Text requestCancelled(String targetName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Request to ").formatted(Formatting.GRAY))
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal(" cancelled.").formatted(Formatting.YELLOW));
    }

    /** Shown when the requester uses /tpacancel but has no outgoing request. */
    public static Text noOutgoingRequest() {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("You have no active outgoing TPA request.").formatted(Formatting.RED));
    }

    /** Shown to requester when they can't TPA to themselves. */
    public static Text cannotTpaToSelf() {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("You cannot send a TPA request to yourself!").formatted(Formatting.RED));
    }

    /**
     * Shown to the requester when the target accepted but the requester no longer
     * has enough XP (e.g. took damage between request and acceptance).
     */
    public static Text insufficientXpOnAccept(int cost, int currentXp) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Teleport cancelled! You no longer have enough XP. Need ").formatted(Formatting.RED))
                .append(Text.literal(cost + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal(", have ").formatted(Formatting.RED))
                .append(Text.literal(currentXp + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal(".").formatted(Formatting.RED));
    }

    /**
     * Shown to the requester when a new request to the same target replaced the old one.
     */
    public static Text requestReplacedBySomeoneElse(String targetName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Your request to ").formatted(Formatting.GRAY))
                .append(Text.literal(targetName).formatted(Formatting.AQUA))
                .append(Text.literal(" was replaced by a new request from another player.").formatted(Formatting.DARK_GRAY));
    }

    // ─────────────────────────────── Target messages ─────────────────────────

    /**
     * The main notification box shown to the target with clickable Accept / Deny buttons.
     *
     * @param requesterName  display name of the player who sent the request.
     * @param xpCost         XP that will be deducted from the requester on acceptance.
     * @param expiresSeconds seconds until the request auto-expires.
     */
    public static Text incomingRequest(String requesterName, int xpCost, long expiresSeconds) {
        MutableText acceptBtn = Text.literal(" [Accept] ")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Accept the teleport request\n").formatted(Formatting.GREEN)
                                        .append(Text.literal("Costs " + requesterName + " ").formatted(Formatting.GRAY))
                                        .append(Text.literal(xpCost + " XP").formatted(Formatting.YELLOW)))));

        MutableText denyBtn = Text.literal("[Deny]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Deny the teleport request").formatted(Formatting.RED))));

        return Text.empty()
                .append(Text.literal("\n"))
                .append(divider()).append(Text.literal("\n"))
                .append(Text.literal(" ✦ ").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("Teleport Request").formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" ✦\n").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" "))
                .append(Text.literal(requesterName).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" wants to teleport to you!\n").formatted(Formatting.GRAY))
                .append(Text.literal(" XP cost: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(xpCost + " XP").formatted(Formatting.YELLOW))
                .append(Text.literal("  •  Expires in: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(expiresSeconds + "s\n").formatted(Formatting.GRAY))
                .append(acceptBtn)
                .append(denyBtn)
                .append(Text.literal("\n"))
                .append(divider());
    }

    /** Shown to the target when the requester cancels their outgoing request. */
    public static Text requestCancelledByRequester(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal(" cancelled their TPA request.").formatted(Formatting.GRAY));
    }

    /** Shown to the target when a request expires. */
    public static Text requestExpiredForTarget(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("TPA request from ").formatted(Formatting.GRAY))
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal(" has expired.").formatted(Formatting.DARK_GRAY));
    }

    /** Shown when the target uses /tpaaccept or /tpadeny but no request is pending. */
    public static Text noIncomingRequest() {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("You have no pending incoming TPA request.").formatted(Formatting.RED));
    }

    /** Shown to the target after they deny a request. */
    public static Text youDenied(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal("Denied ").formatted(Formatting.GRAY))
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal("'s request.").formatted(Formatting.GRAY));
    }

    /** Shown to the target after the requester successfully teleports to them. */
    public static Text targetReceivedTeleport(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal(" has teleported to you.").formatted(Formatting.GREEN));
    }

    /** Shown to the target when they accept but the requester has since gone offline. */
    public static Text requesterOffline(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal(" is no longer online. Request cancelled.").formatted(Formatting.RED));
    }

    /** Shown to the target when they accept but the requester no longer has enough XP. */
    public static Text requesterInsufficientXp(String requesterName) {
        return Text.empty()
                .append(prefix())
                .append(Text.literal(requesterName).formatted(Formatting.AQUA))
                .append(Text.literal(" no longer has enough XP to teleport. Request cancelled.").formatted(Formatting.RED));
    }
}
