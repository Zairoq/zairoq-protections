package dev.zairoq.protections.mixin;
import dev.zairoq.protections.ProtectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private List<BlockPos> toBlow;
    @Shadow @Final private Level level;
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void zairoq$removeClaimedBlocksFromExplosion(boolean spawnParticles, CallbackInfo ci) {
        toBlow.removeIf(pos -> ProtectionStorage.isProtected(level, pos));
    }
}
