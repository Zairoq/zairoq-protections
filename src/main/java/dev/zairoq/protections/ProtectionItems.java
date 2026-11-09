package dev.zairoq.protections;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class ProtectionItems {
    private static final String DATA_KEY = "zairoq_protection";

    private ProtectionItems() {}

    public static StoneData read(ItemStack stack) {
        if (!stack.is(Items.LODESTONE)) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return null;

        CompoundTag marker = customData.copyTag().getCompoundOrEmpty(DATA_KEY);
        String mineral = marker.getStringOr("mineral", "");
        int size = marker.getIntOr("size", 0);
        int expectedSize = sizeFor(mineral);
        if (expectedSize == 0 || size != expectedSize) return null;
        return new StoneData(mineral, size);
    }

    public static ItemStack create(String mineral) {
        int size = sizeFor(mineral);
        if (size == 0) return ItemStack.EMPTY;

        CompoundTag marker = new CompoundTag();
        marker.putString("mineral", mineral);
        marker.putInt("size", size);
        CompoundTag customData = new CompoundTag();
        customData.put(DATA_KEY, marker);

        ItemStack stack = new ItemStack(Items.LODESTONE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName(mineral)));
        return stack;
    }

    private static int sizeFor(String mineral) {
        return switch (mineral) {
            case "coal" -> 10;
            case "copper" -> 20;
            case "diamond" -> 50;
            case "netherite" -> 100;
            default -> 0;
        };
    }

    private static String displayName(String mineral) {
        return switch (mineral) {
            case "coal" -> "Coal Protection Stone";
            case "copper" -> "Copper Protection Stone";
            case "diamond" -> "Diamond Protection Stone";
            case "netherite" -> "Netherite Protection Stone";
            default -> "Protection Stone";
        };
    }

    public record StoneData(String mineral, int size) {}
}
