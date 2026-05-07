package MinecraftPlugin.minecraftPluginProgrammeringB;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// This is the main class of the plugin.
// Paper calls onEnable when the server starts or when the plugin is loaded.
public final class MinecraftPluginProgrammeringB extends JavaPlugin {
    private CustomEnchantManager enchantManager;

    @Override
    public void onEnable() {
        // The manager is responsible for reading and writing custom enchants on items.
        enchantManager = new CustomEnchantManager(this);

        // The listener reacts to Minecraft events, such as hitting an enemy or breaking a block.
        CustomEnchantListener listener = new CustomEnchantListener(enchantManager);

        // The command class handles /customenchants, /ce, and /cenchants.
        CustomEnchantCommand command = new CustomEnchantCommand(enchantManager);

        // Register the event listener so Paper knows which methods should run during gameplay.
        Bukkit.getPluginManager().registerEvents(listener, this);

        // Register the command executor and tab completer.
        // The command itself is defined in plugin.yml.
        Objects.requireNonNull(getCommand("customenchants")).setExecutor(command);
        Objects.requireNonNull(getCommand("customenchants")).setTabCompleter(command);

        // Some armor enchants are passive effects, like speed or night vision.
        // This repeating task checks all online players every 40 ticks, which is about 2 seconds.
        Bukkit.getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(listener::applyPassiveArmorEffects), 20L, 40L);

        getLogger().info("Custom enchants enabled: " + CustomEnchant.values().length);
    }

    @Override
    public void onDisable() {
        getLogger().info("Custom enchants disabled.");
    }
}
