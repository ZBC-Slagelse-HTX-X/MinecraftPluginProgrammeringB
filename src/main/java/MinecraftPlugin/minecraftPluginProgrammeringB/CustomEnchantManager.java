package MinecraftPlugin.minecraftPluginProgrammeringB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

// This class handles the "data" part of custom enchants.
// It saves enchants inside the item using Bukkit's PersistentDataContainer.
// That is more reliable than only checking lore text, because the data stays on the item.
final class CustomEnchantManager {
    // This line is shown in the item's lore before the custom enchant names.
    private static final String LORE_HEADER = ChatColor.DARK_GRAY + "Custom Enchants:";

    // Used only for display, so level 3 becomes III in the item's lore.
    private static final NavigableMap<Integer, String> ROMAN_NUMERALS = new TreeMap<>(Map.of(
            1, "I",
            2, "II",
            3, "III",
            4, "IV",
            5, "V",
            6, "VI",
            7, "VII",
            8, "VIII",
            9, "IX",
            10, "X"
    ));

    private final Plugin plugin;

    CustomEnchantManager(Plugin plugin) {
        this.plugin = plugin;
    }

    // Reads the level of a specific custom enchant from an item.
    // If the item does not have that enchant, this returns 0.
    int getLevel(ItemStack item, CustomEnchant enchant) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        Integer level = item.getItemMeta().getPersistentDataContainer()
                .get(key(enchant), PersistentDataType.INTEGER);
        return level == null ? 0 : Math.max(0, Math.min(level, enchant.maxLevel()));
    }

    // A shortcut for checking whether the item has at least level 1 of an enchant.
    boolean has(ItemStack item, CustomEnchant enchant) {
        return getLevel(item, enchant) > 0;
    }

    // Applies one enchant to an item.
    // It also updates the item's lore so players can see the enchant in game.
    void apply(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("You must hold an item.");
        }
        if (!enchant.accepts(item)) {
            throw new IllegalArgumentException(enchant.displayName() + " can only be applied to " + enchant.slot().name().toLowerCase() + " items.");
        }

        int clampedLevel = Math.max(1, Math.min(level, enchant.maxLevel()));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(enchant), PersistentDataType.INTEGER, clampedLevel);
        item.setItemMeta(meta);
        refreshLore(item);
    }

    // Adds together the levels from multiple armor pieces.
    // This is useful for enchants like Guard, where every armor piece can help.
    int getArmorLevel(Iterable<ItemStack> armor, CustomEnchant enchant) {
        int total = 0;
        for (ItemStack item : armor) {
            total += getLevel(item, enchant);
        }
        return total;
    }

    // Rebuilds the lore lines for custom enchants.
    // Existing normal lore is kept, but old custom enchant lines are replaced.
    void refreshLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? withoutCustomEnchantLore(meta.getLore()) : new ArrayList<>();

        List<String> enchantLines = new ArrayList<>();
        for (CustomEnchant enchant : CustomEnchant.values()) {
            int level = getLevel(item, enchant);
            if (level > 0) {
                enchantLines.add(ChatColor.GRAY + enchant.displayName() + " " + toRoman(level));
            }
        }

        if (!enchantLines.isEmpty()) {
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }
            lore.add(LORE_HEADER);
            lore.addAll(enchantLines);
        }

        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    // Removes the old custom enchant section from lore before adding the new section.
    private List<String> withoutCustomEnchantLore(List<String> lore) {
        List<String> cleaned = new ArrayList<>();
        boolean skippingCustomLines = false;

        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if ("Custom Enchants:".equals(stripped)) {
                skippingCustomLines = true;
                if (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).isEmpty()) {
                    cleaned.remove(cleaned.size() - 1);
                }
                continue;
            }

            if (skippingCustomLines && isCustomEnchantLine(line)) {
                continue;
            }

            skippingCustomLines = false;
            cleaned.add(line);
        }

        return cleaned;
    }

    // Checks if a lore line looks like one of our custom enchant display lines.
    private boolean isCustomEnchantLine(String line) {
        String stripped = ChatColor.stripColor(line);
        if (stripped == null) {
            return false;
        }
        for (CustomEnchant enchant : CustomEnchant.values()) {
            if (stripped.startsWith(enchant.displayName() + " ")) {
                return true;
            }
        }
        return false;
    }

    // Creates a unique storage key for one enchant.
    // NamespacedKey prevents this plugin's data from conflicting with other plugins.
    private NamespacedKey key(CustomEnchant enchant) {
        return new NamespacedKey(plugin, "custom_enchant_" + enchant.key());
    }

    // Converts small numbers into roman numerals for item lore.
    private static String toRoman(int number) {
        Map.Entry<Integer, String> entry = ROMAN_NUMERALS.floorEntry(number);
        if (entry == null) {
            return Integer.toString(number);
        }
        if (entry.getKey() == number) {
            return entry.getValue();
        }
        return entry.getValue() + toRoman(number - entry.getKey());
    }
}
