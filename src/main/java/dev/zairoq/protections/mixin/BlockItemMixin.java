package dev.zairoq.protections.mixin;

import dev.zairoq.protections.ProtectionItems;
import dev.zairoq.protections.ProtectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.item.BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void zairoq$protectBlockPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) return;

        BlockPos target = context.getClickedPos();
        var claim = ProtectionStorage.find(level.dimension().identifier().toString(), target);
        if (claim != null && !ProtectionStorage.canBuild(claim, context.getPlayer().getUUID())) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        // Capture before vanilla shrinks the held stack (which may become EMPTY).
        ProtectionItems.capturePlacement(context);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void zairoq$createProtectionStoneClaim(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ProtectionItems.PendingPlacement pending = ProtectionItems.takePlacement(context);
        if (pending == null || !cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ProtectionItems.StoneData stone = pending.stone();
        BlockPos core = pending.pos();
        if (!level.getBlockState(core).is(Blocks.LODESTONE)) return;

        var claim = ProtectionStorage.createProtectionStone(level.getServer(), player, level, core, stone.size(), stone.mineral());
        if (claim != null) {
            player.sendSystemMessage(Component.literal("Protection created: " + stone.size() + "x" + stone.size() + " blocks, full world height."));
            return;
        }

        level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
        ItemStack refund = ProtectionItems.create(stone.mineral());
        if (!player.getInventory().add(refund)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), refund));
        }
        player.sendSystemMessage(Component.literal("Invalid protection: this area overlaps another claim or claim storage is unavailable. The item was returned."));
    }
}
