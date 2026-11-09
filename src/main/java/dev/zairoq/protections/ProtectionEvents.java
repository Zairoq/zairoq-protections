package dev.zairoq.protections;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
public final class ProtectionEvents {
    private ProtectionEvents() {}
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return true;
            ProtectionStorage.Claim claim = ProtectionStorage.find(level.dimension().identifier().toString(), pos);
            if (claim == null) return true;
            if (claim.isCore(pos)) {
                if (!(player instanceof ServerPlayer sp) || !claim.owner.equals(sp.getUUID().toString())) return false;
                if (pos.getX() == claim.baseX && pos.getY() == claim.baseY && pos.getZ() == claim.baseZ) {
                    BlockPos core = new BlockPos(claim.coreX, claim.coreY, claim.coreZ);
                    if (level.getBlockState(core).is(Blocks.LODESTONE)) level.removeBlock(core, false);
                }
                ProtectionStorage.remove(level.getServer(), claim);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Claim removed."));
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
            return !(attacker instanceof net.minecraft.world.entity.player.Player player)
                    || ProtectionStorage.canInteract(claim, player.getUUID());
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
    }
}
