package dev.zairoq.protections;

import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProtectionEvents {
    private static final Map<UUID, ClaimPresence> PLAYER_CLAIMS = new HashMap<>();
    private ProtectionEvents() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return true;
            ProtectionStorage.Claim claim = ProtectionStorage.find(level.dimension().identifier().toString(), pos);
            if (claim == null) return true;
            if (claim.isCore(pos)) {
                if (!(player instanceof ServerPlayer sp) || !claim.owner.equals(sp.getUUID().toString())) return false;
                boolean craftedProtectionStone = claim.coreX == claim.baseX && claim.coreY == claim.baseY && claim.coreZ == claim.baseZ;
                if (craftedProtectionStone) {
                    ItemStack protectionStack = ProtectionItems.create(claim.mineral);
                    if (protectionStack.isEmpty()) return false;
                    ProtectionStorage.remove(level.getServer(), claim);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, protectionStack));
                    player.sendSystemMessage(Component.literal("Claim removed."));
                    return false;
                }
                if (pos.getX() == claim.baseX && pos.getY() == claim.baseY && pos.getZ() == claim.baseZ) {
                    BlockPos core = new BlockPos(claim.coreX, claim.coreY, claim.coreZ);
                    if (level.getBlockState(core).is(Blocks.LODESTONE)) level.removeBlock(core, false);
                }
                ProtectionStorage.remove(level.getServer(), claim);
                player.sendSystemMessage(Component.literal("Claim removed."));
                return true;
            }
            return ProtectionStorage.canBuild(claim, player.getUUID());
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!(world instanceof ServerLevel level) || player.isSpectator()) return InteractionResult.PASS;
            BlockPos clicked = hit.getBlockPos();
            var claim = ProtectionStorage.find(level.dimension().identifier().toString(), clicked);
            if (claim != null && !ProtectionStorage.canInteract(claim, player.getUUID())) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity.level() instanceof ServerLevel level)) return true;
            var claim = ProtectionStorage.find(level.dimension().identifier().toString(), entity.blockPosition());
            if (claim == null) return true;
            var attacker = source.getEntity();
            return !(attacker instanceof net.minecraft.world.entity.player.Player player) || ProtectionStorage.canInteract(claim, player.getUUID());
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            var claim = ProtectionStorage.find(level.dimension().identifier().toString(), entity.blockPosition());
            return claim != null && !ProtectionStorage.canInteract(claim, player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            var claim = ProtectionStorage.find(level.dimension().identifier().toString(), entity.blockPosition());
            return claim != null && !ProtectionStorage.canInteract(claim, player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        // Evaluate only the player's current position and notify only when the claim changes.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String dimension = player.level().dimension().identifier().toString();
                ProtectionStorage.Claim current = ProtectionStorage.find(dimension, player.blockPosition());
                ClaimPresence previous = PLAYER_CLAIMS.get(player.getUUID());
                String currentId = current == null ? null : dimension + ":" + current.id;
                if (previous != null && previous.key.equals(currentId)) continue;
                if (current != null) {
                    String place = current.owner.equals(player.getUUID().toString())
                            ? "your protection" : current.ownerName + "'s protection";
                    sendActionBar(player, Component.literal("Entered " + place)
                            .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                    PLAYER_CLAIMS.put(player.getUUID(), new ClaimPresence(currentId,
                            current.owner.equals(player.getUUID().toString()) ? "your protection" : current.ownerName + "'s protection"));
                } else {
                    if (previous != null) {
                        sendActionBar(player, Component.literal("Left " + previous.label)
                                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                        PLAYER_CLAIMS.remove(player.getUUID());
                    }
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PLAYER_CLAIMS.remove(handler.player.getUUID()));
    }

    private static void sendActionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private record ClaimPresence(String key, String label) {}
}
