package dev.zairoq.protections.mixin;

import dev.zairoq.protections.ProtectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.item.BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void zairoq$protectBlockPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) return;
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        var claim = ProtectionStorage.find(level.dimension().identifier().toString(), target);
        if (claim != null && !ProtectionStorage.canBuild(claim, context.getPlayer().getUUID())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void zairoq$registerLodestoneClaim(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;
        if (!context.getItemInHand().is(Items.LODESTONE)) return;

        if (context.getClickedFace() != Direction.UP) {
            player.sendSystemMessage(Component.literal("Invalid protection: place the Lodestone directly on top of a supported mineral block."));
            return;
        }

        BlockPos base = context.getClickedPos();
        BlockPos core = base.relative(Direction.UP);
        BlockState baseState = level.getBlockState(base);
        if (!level.getBlockState(core).is(Blocks.LODESTONE)) {
            player.sendSystemMessage(Component.literal("Invalid protection: the Lodestone was not placed successfully."));
            return;
        }

        int size = ProtectionStorage.claimSizeForBase(baseState);
        if (size == 0) {
            player.sendSystemMessage(Component.literal("Invalid protection: place the Lodestone on a Coal, Copper, Diamond, or Netherite Block."));
            return;
        }

        var claim = ProtectionStorage.create(level.getServer(), player, level, base, core, size,
                ProtectionStorage.mineralName(baseState));
        if (claim != null) {
            player.sendSystemMessage(Component.literal(
                    "Protection created: " + size + "x" + size + " blocks, full world height."));
        } else {
            player.sendSystemMessage(Component.literal(
                    "Invalid protection: this area overlaps another claim or claim storage is unavailable."));
        }
    }
}
