package dev.zairoq.protections;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
public final class ProtectionStoneItem extends BlockItem {
    private final int claimSize;
    private final String mineral;
    public ProtectionStoneItem(ResourceLocation id, int claimSize, String mineral) {
        super(Blocks.LODESTONE, new Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        this.claimSize = claimSize;
        this.mineral = mineral;
    }
    public int claimSize() { return claimSize; }
    public String mineral() { return mineral; }
}
