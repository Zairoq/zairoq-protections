package dev.zairoq.protections.mixin;
import dev.zairoq.protections.ProtectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
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
        BlockPos target = context.getPlacementPos();
        var claim = ProtectionStorage.find(level.dimension().location().toString(), target);
        if (claim != null && !ProtectionStorage.canBuild(claim, context.getPlayer().getUUID())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void zairoq$registerLodestoneClaim(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;
        if (!context.getItemInHand().is(net.minecraft.world.item.Items.LODESTONE)) return;
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) return;
        BlockPos base = context.getClickedPos(), core = base.relative(context.getClickedFace());
        BlockState baseState = level.getBlockState(base);
        int size = ProtectionStorage.claimSizeForBase(baseState); if (size == 0) return;
        var claim = ProtectionStorage.create(level.getServer(), player, level, base, core, size, ProtectionStorage.mineralName(baseState));
        if (claim != null) player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "Claim " + claim.id + " created: " + size + "x" + size + " blocks, full world height."));
        else {
            level.removeBlock(core, false);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This area overlaps another claim. The protection core was not registered."));
        }
    }
}
