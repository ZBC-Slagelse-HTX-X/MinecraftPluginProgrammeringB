package MinecraftPlugin.minecraftPluginProgrammeringB;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

// An enum is a fixed list of possible values.
// Here, each value is one custom enchant that the plugin supports.
enum CustomEnchant {
    // Format:
    // INTERNAL_NAME(command name, display name, max level, allowed item type, description)
    LIFESTEAL("lifesteal", "Lifesteal", 3, EnchantSlot.WEAPON, "Heals you when you hit an enemy."),
    THUNDER("thunder", "Thunder", 3, EnchantSlot.WEAPON, "Chance to strike your target with lightning damage."),
    VENOM("venom", "Venom", 2, EnchantSlot.WEAPON, "Poisons enemies you hit."),
    LAUNCH("launch", "Launch", 2, EnchantSlot.WEAPON, "Knocks enemies upward."),
    GUARD("guard", "Guard", 4, EnchantSlot.ARMOR, "Reduces incoming damage."),
    INFERNO("inferno", "Inferno", 3, EnchantSlot.ARMOR, "Burns enemies that hit you."),
    VITALITY("vitality", "Vitality", 2, EnchantSlot.CHESTPLATE, "Grants regeneration while worn."),
    SWIFT("swift", "Swift", 3, EnchantSlot.BOOTS, "Grants speed while worn."),
    CLARITY("clarity", "Clarity", 1, EnchantSlot.HELMET, "Grants night vision while worn."),
    SPRINGS("springs", "Springs", 4, EnchantSlot.BOOTS, "Reduces fall damage."),
    AUTOSMELT("autosmelt", "Autosmelt", 1, EnchantSlot.TOOL, "Smelts mined blocks automatically."),
    VEINMINER("veinminer", "Veinminer", 3, EnchantSlot.TOOL, "Mines nearby matching ores in one break."),
    REPLANT("replant", "Replant", 1, EnchantSlot.TOOL, "Automatically replants mature crops."),
    LUMBERJACK("lumberjack", "Lumberjack", 3, EnchantSlot.TOOL, "Breaks connected logs faster.");

    private final String key;
    private final String displayName;
    private final int maxLevel;
    private final EnchantSlot slot;
    private final String description;

    // This constructor runs once for every enchant listed above.
    // It stores the settings for that enchant in the fields below.
    CustomEnchant(String key, String displayName, int maxLevel, EnchantSlot slot, String description) {
        this.key = key;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.slot = slot;
        this.description = description;
    }

    // These small methods are called "getters".
    // They let other classes read the private fields without changing them.
    String key() {
        return key;
    }

    String displayName() {
        return displayName;
    }

    int maxLevel() {
        return maxLevel;
    }

    EnchantSlot slot() {
        return slot;
    }

    String description() {
        return description;
    }

    // Checks whether this enchant can be placed on the item the player is holding.
    boolean accepts(ItemStack item) {
        return slot.accepts(item);
    }

    // Finds an enchant from text typed in a command.
    // For example, "lifesteal", "LifeSteal", and "life-steal" are treated similarly.
    static Optional<CustomEnchant> byKey(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return Arrays.stream(values())
                .filter(enchant -> enchant.key.replace("_", "").equals(normalized)
                        || enchant.displayName.toLowerCase(Locale.ROOT).replace(" ", "").equals(normalized))
                .findFirst();
    }
}
