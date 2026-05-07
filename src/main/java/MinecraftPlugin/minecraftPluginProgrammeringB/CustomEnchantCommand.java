package MinecraftPlugin.minecraftPluginProgrammeringB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

// This class controls what happens when someone uses /customenchants.
// It also provides tab completion suggestions while typing the command.
final class CustomEnchantCommand implements CommandExecutor, TabCompleter {
    private final CustomEnchantManager enchants;

    CustomEnchantCommand(CustomEnchantManager enchants) {
        this.enchants = enchants;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If the player only types /ce, show the enchant list.
        // args contains the words after the command name.
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendEnchantList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("apply")) {
            return applyEnchant(sender, args);
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " list");
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " give <enchant> [level]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // This method returns possible words for Minecraft to show in the tab-complete menu.
        if (args.length == 1) {
            return matching(args[0], Arrays.asList("list", "give", "apply"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("apply"))) {
            return matching(args[1], Arrays.stream(CustomEnchant.values()).map(CustomEnchant::key).toList());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("apply"))) {
            return matching(args[2], Arrays.asList("1", "2", "3", "4"));
        }
        return Collections.emptyList();
    }

    // Sends one line for every custom enchant, including its max level and description.
    private void sendEnchantList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Custom enchants:");
        for (CustomEnchant enchant : CustomEnchant.values()) {
            sender.sendMessage(ChatColor.YELLOW + enchant.key()
                    + ChatColor.GRAY + " [" + enchant.slot().name().toLowerCase() + ", max " + enchant.maxLevel() + "] "
                    + ChatColor.WHITE + enchant.description());
        }
    }

    // Applies an enchant to the item in the player's main hand.
    private boolean applyEnchant(CommandSender sender, String[] args) {
        // The console can run commands too, but it does not have a hand or an item.
        // Therefore only players can use the give/apply command.
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can apply enchants to held items.");
            return true;
        }
        if (!player.hasPermission("customenchants.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to apply custom enchants.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /customenchants give <enchant> [level]");
            return true;
        }

        // Look up the enchant name typed by the player.
        CustomEnchant enchant = CustomEnchant.byKey(args[1]).orElse(null);
        if (enchant == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchant: " + args[1]);
            return true;
        }

        // If no level is typed, use the enchant's maximum level.
        int level = enchant.maxLevel();
        if (args.length >= 3) {
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.RED + "Level must be a number.");
                return true;
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        try {
            // Clamp means "keep the number inside a safe range".
            // This prevents levels below 1 or above the enchant's max level.
            int appliedLevel = Math.max(1, Math.min(level, enchant.maxLevel()));
            enchants.apply(item, enchant, appliedLevel);
            player.sendMessage(ChatColor.GREEN + "Applied " + enchant.displayName() + " " + appliedLevel + " to your held item.");
        } catch (IllegalArgumentException exception) {
            player.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    // Returns only the options that start with what the player has typed so far.
    private List<String> matching(String input, List<String> options) {
        String lowerInput = input.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerInput)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
