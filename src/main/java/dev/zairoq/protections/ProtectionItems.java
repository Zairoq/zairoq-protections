package dev.zairoq.protections;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
public final class ProtectionItems {
    public static Item COAL, COPPER, DIAMOND, NETHERITE;
    private ProtectionItems() {}
    public static void register() {
        COAL = register("coal_protection_stone", 10, "coal");
        COPPER = register("copper_protection_stone", 20, "copper");
        DIAMOND = register("diamond_protection_stone", 50, "diamond");
        NETHERITE = register("netherite_protection_stone", 100, "netherite");
    }
    private static Item register(String path, int size, String mineral) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ZairoqProtections.MOD_ID, path);
        return Registry.register(BuiltInRegistries.ITEM, id, new ProtectionStoneItem(id, size, mineral));
    }
    public static Item forMineral(String mineral) {
        return switch (mineral) {
            case "coal" -> COAL;
            case "copper" -> COPPER;
            case "diamond" -> DIAMOND;
            case "netherite" -> NETHERITE;
            default -> null;
        };
    }
}
