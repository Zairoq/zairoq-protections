package dev.zairoq.protections;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
public final class ProtectionCommands {
    private ProtectionCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("protection")
            .then(Commands.literal("help").executes(c -> help(c.getSource())))
            .then(Commands.literal("info").executes(c -> info(c.getSource())))
            .then(Commands.literal("list").executes(c -> list(c.getSource())))
            .then(Commands.literal("trust").then(Commands.argument("player", EntityArgument.player()).executes(c -> trust(c.getSource(), EntityArgument.getPlayer(c, "player"), true))))
            .then(Commands.literal("untrust").then(Commands.argument("player", EntityArgument.player()).executes(c -> trust(c.getSource(), EntityArgument.getPlayer(c, "player"), false))))
            .then(Commands.literal("coowner")
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).executes(c -> coowner(c.getSource(), EntityArgument.getPlayer(c, "player"), true))))
                .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).executes(c -> coowner(c.getSource(), EntityArgument.getPlayer(c, "player"), false)))))
            .then(Commands.literal("abandon").executes(c -> abandon(c.getSource())))
            .then(Commands.literal("admin").requires(s -> s.hasPermission(2))
                .then(Commands.literal("remove").then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(c -> adminRemove(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id")))))));
    }
    private static int help(CommandSourceStack s) {
        s.sendSuccess(() -> Component.literal("/protection info | list | trust <player> | untrust <player> | coowner add/remove <player> | abandon"), false); return 1;
    }
    private static int info(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException(); var claim = current(p);
        if (claim == null) return fail(s, "You are not inside a protected claim.");
        s.sendSuccess(() -> Component.literal(describe(claim)), false); return 1;
    }
    private static int list(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        var claims = ProtectionStorage.all().stream().filter(c -> c.owner.equals(p.getUUID().toString())
            || c.coOwners.containsKey(p.getUUID().toString()) || c.trusted.containsKey(p.getUUID().toString())).toList();
        if (claims.isEmpty()) return fail(s, "You do not belong to any claims.");
        for (var claim : claims) s.sendSuccess(() -> Component.literal(describe(claim)), false);
        return claims.size();
    }
    private static int trust(CommandSourceStack s, ServerPlayer target, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException(); var claim = current(p);
        if (claim == null || (!claim.owner.equals(p.getUUID().toString()) && !claim.coOwners.containsKey(p.getUUID().toString())))
            return fail(s, "Only the owner or a co-owner can manage trusted players.");
        String id = target.getUUID().toString();
        if (id.equals(p.getUUID().toString())) return fail(s, "You already own this claim.");
        if (claim.owner.equals(id) || claim.coOwners.containsKey(id)) return fail(s, "That player already has a higher permission level.");
        if (add) claim.trusted.put(id, target.getGameProfile().getName()); else claim.trusted.remove(id);
        ProtectionStorage.save(s.getServer()); s.sendSuccess(() -> Component.literal(add ? "Player trusted." : "Player untrusted."), false); return 1;
    }
    private static int coowner(CommandSourceStack s, ServerPlayer target, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException(); var claim = current(p);
        if (claim == null || !claim.owner.equals(p.getUUID().toString())) return fail(s, "Only the claim owner can manage co-owners.");
        String id = target.getUUID().toString(); if (id.equals(p.getUUID().toString())) return fail(s, "You are already the owner.");
        if (add) { claim.trusted.remove(id); claim.coOwners.put(id, target.getGameProfile().getName()); } else claim.coOwners.remove(id);
        ProtectionStorage.save(s.getServer()); s.sendSuccess(() -> Component.literal(add ? "Co-owner added." : "Co-owner removed."), false); return 1;
    }
    private static int abandon(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException(); var claim = current(p);
        if (claim == null || !claim.owner.equals(p.getUUID().toString())) return fail(s, "Only the claim owner can abandon it.");
        BlockPos core = new BlockPos(claim.coreX, claim.coreY, claim.coreZ);
        if (p.serverLevel().dimension().location().toString().equals(claim.dimension)
            && p.serverLevel().getBlockState(core).is(net.minecraft.world.level.block.Blocks.LODESTONE)) p.serverLevel().removeBlock(core, false);
        ProtectionStorage.remove(s.getServer(), claim); s.sendSuccess(() -> Component.literal("Claim abandoned. Its blocks were left in place."), false); return 1;
    }
    private static int adminRemove(CommandSourceStack s, String id) {
        var claim = ProtectionStorage.findById(id); if (claim == null) return fail(s, "Claim not found.");
        ProtectionStorage.remove(s.getServer(), claim); s.sendSuccess(() -> Component.literal("Claim " + id + " removed from protection data."), true); return 1;
    }
    private static ProtectionStorage.Claim current(ServerPlayer p) {
        return ProtectionStorage.find(p.serverLevel().dimension().location().toString(), BlockPos.containing(p.position()));
    }
    private static String describe(ProtectionStorage.Claim c) {
        return "Claim " + c.id + " | " + c.mineral + " " + c.size + "x" + c.size + " | owner: " + c.ownerName
            + " | dimension: " + c.dimension + " | co-owners: " + c.coOwners.size() + " | trusted: " + c.trusted.size();
    }
    private static int fail(CommandSourceStack s, String message) { s.sendFailure(Component.literal(message)); return 0; }
}
