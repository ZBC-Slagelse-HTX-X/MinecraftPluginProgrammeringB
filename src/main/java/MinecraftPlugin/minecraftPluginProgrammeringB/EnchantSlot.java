package MinecraftPlugin.minecraftPluginProgrammeringB;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

// This enum describes which kind of item an enchant is allowed on.
// For example, Lifesteal is WEAPON only, while Swift is BOOTS only.
enum EnchantSlot {
    WEAPON,
    TOOL,
    ARMOR,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;

    // Returns true if the given item is allowed for this slot.
    // Bukkit item types are named like DIAMOND_SWORD or NETHERITE_BOOTS,
    // so the code checks the end of the material name.
    boolean accepts(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        Material material = item.getType();
        String name = material.name();

        return switch (this) {
            case WEAPON -> isWeapon(name);
            case TOOL -> isTool(name);
            case ARMOR -> isArmor(name);
            case HELMET -> name.endsWith("_HELMET");
            case CHESTPLATE -> name.endsWith("_CHESTPLATE") || name.equals("ELYTRA");
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
        };
    }

    private static boolean isWeapon(String name) {
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.equals("BOW")
                || name.equals("CROSSBOW")
                || name.equals("TRIDENT")
                || name.equals("MACE");
    }

    private static boolean isTool(String name) {
        return name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.equals("SHEARS");
    }

    private static boolean isArmor(String name) {
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("ELYTRA");
    }
}
