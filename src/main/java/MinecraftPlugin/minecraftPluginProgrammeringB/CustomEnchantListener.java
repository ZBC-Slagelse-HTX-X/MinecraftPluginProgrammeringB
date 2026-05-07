package MinecraftPlugin.minecraftPluginProgrammeringB;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

// A listener contains methods that run when something happens in Minecraft.
// The @EventHandler methods below are called automatically by Paper.
final class CustomEnchantListener implements Listener {
    // These are the six blocks touching a block: east, west, up, down, south, north.
    // They are used by Veinminer and Lumberjack to search nearby blocks.
    private static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    // This map tells Autosmelt which block should turn into which item.
    // Example: IRON_ORE becomes IRON_INGOT.
    private static final Map<Material, Material> SMELTS = new EnumMap<>(Material.class);

    static {
        SMELTS.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELTS.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELTS.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELTS.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELTS.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELTS.put(Material.SAND, Material.GLASS);
        SMELTS.put(Material.COBBLESTONE, Material.STONE);
        SMELTS.put(Material.STONE, Material.SMOOTH_STONE);
    }

    private final CustomEnchantManager enchants;

    // When this plugin breaks extra blocks for Veinminer or Lumberjack,
    // Paper fires new BlockBreakEvents. This set helps us ignore those extra events.
    private final Set<Location> pluginBrokenBlocks = new HashSet<>();

    CustomEnchantListener(CustomEnchantManager enchants) {
        this.enchants = enchants;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // If a player hits a living entity, check weapon enchants.
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            handleWeaponHit(event, attacker, victim);
        }

        // If a player is hit by a living entity, check armor enchants.
        if (event.getEntity() instanceof Player defender && event.getDamager() instanceof LivingEntity attacker) {
            handleArmorHit(event, defender, attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onFallDamage(EntityDamageEvent event) {
        // Springs only cares about fall damage on players.
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) {
            return;
        }

        int springs = enchants.getLevel(player.getInventory().getBoots(), CustomEnchant.SPRINGS);
        if (springs <= 0) {
            return;
        }

        event.setDamage(event.getDamage() * Math.max(0.0D, 1.0D - (springs * 0.22D)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onBlockBreak(BlockBreakEvent event) {
        // Ignore block break events that were caused by this plugin's chain-breaking code.
        Location location = event.getBlock().getLocation();
        if (pluginBrokenBlocks.remove(location)) {
            return;
        }

        // Tool enchants use the item in the player's main hand.
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            return;
        }

        // Replant cancels the normal block break and manually resets the crop to age 0.
        if (enchants.has(tool, CustomEnchant.REPLANT) && tryReplant(event.getBlock(), tool, player)) {
            event.setCancelled(true);
            return;
        }

        // Autosmelt replaces normal drops with cooked/smelted drops.
        if (enchants.has(tool, CustomEnchant.AUTOSMELT)) {
            autosmelt(event, tool);
        }

        // Veinminer breaks a limited number of connected ore blocks.
        int veinminer = enchants.getLevel(tool, CustomEnchant.VEINMINER);
        if (veinminer > 0 && isOre(event.getBlock().getType())) {
            breakConnectedBlocks(player, tool, event.getBlock(), 8 + (veinminer * 8), true);
        }

        // Lumberjack breaks a limited number of connected log blocks.
        int lumberjack = enchants.getLevel(tool, CustomEnchant.LUMBERJACK);
        if (lumberjack > 0 && Tag.LOGS.isTagged(event.getBlock().getType())) {
            breakConnectedBlocks(player, tool, event.getBlock(), 12 + (lumberjack * 12), false);
        }
    }

    // This method is called every few seconds from the main class.
    // It reapplies short potion effects while the player is wearing enchanted armor.
    void applyPassiveArmorEffects(Player player) {
        PlayerInventory inventory = player.getInventory();

        int swift = enchants.getLevel(inventory.getBoots(), CustomEnchant.SWIFT);
        if (swift > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, Math.min(swift - 1, 2), true, false, true));
        }

        int vitality = enchants.getLevel(inventory.getChestplate(), CustomEnchant.VITALITY);
        if (vitality > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, Math.min(vitality - 1, 1), true, false, true));
        }

        int clarity = enchants.getLevel(inventory.getHelmet(), CustomEnchant.CLARITY);
        if (clarity > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, true));
        }
    }

    // Handles weapon enchants when a player hits a mob or another player.
    private void handleWeaponHit(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        int lifesteal = enchants.getLevel(weapon, CustomEnchant.LIFESTEAL);
        if (lifesteal > 0 && attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double newHealth = Math.min(maxHealth, attacker.getHealth() + (lifesteal * 0.75D));
            attacker.setHealth(newHealth);
        }

        int venom = enchants.getLevel(weapon, CustomEnchant.VENOM);
        if (venom > 0) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60 + (venom * 30), venom - 1, true, true, true));
        }

        int thunder = enchants.getLevel(weapon, CustomEnchant.THUNDER);
        if (thunder > 0 && Math.random() < 0.08D * thunder) {
            World world = victim.getWorld();
            world.strikeLightningEffect(victim.getLocation());
            world.spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.55D, 0.35D, 0.02D);
            event.setDamage(event.getDamage() + (1.5D * thunder));
        }

        int launch = enchants.getLevel(weapon, CustomEnchant.LAUNCH);
        if (launch > 0) {
            Vector velocity = victim.getVelocity();
            velocity.setY(Math.min(1.4D, velocity.getY() + 0.35D + (launch * 0.25D)));
            victim.setVelocity(velocity);
        }
    }

    // Handles armor enchants when a player is hit.
    private void handleArmorHit(EntityDamageByEntityEvent event, Player defender, LivingEntity attacker) {
        Iterable<ItemStack> armor = java.util.Arrays.asList(defender.getInventory().getArmorContents());

        int guard = enchants.getArmorLevel(armor, CustomEnchant.GUARD);
        if (guard > 0) {
            event.setDamage(event.getDamage() * Math.max(0.55D, 1.0D - (guard * 0.04D)));
        }

        int inferno = enchants.getArmorLevel(armor, CustomEnchant.INFERNO);
        if (inferno > 0 && Math.random() < 0.12D * inferno) {
            attacker.setFireTicks(Math.max(attacker.getFireTicks(), 50 + (inferno * 30)));
        }
    }

    // Changes block drops into smelted drops.
    private void autosmelt(BlockBreakEvent event, ItemStack tool) {
        Material smelted = SMELTS.get(event.getBlock().getType());
        if (smelted == null) {
            return;
        }

        event.setDropItems(false);
        World world = event.getBlock().getWorld();
        for (ItemStack drop : event.getBlock().getDrops(tool)) {
            int amount = Math.max(1, drop.getAmount());
            world.dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smelted, amount));
        }
    }

    // Harvests a mature crop, removes one seed from the player, and replants the crop.
    private boolean tryReplant(Block block, ItemStack tool, Player player) {
        if (!(block.getBlockData() instanceof Ageable crop) || crop.getAge() < crop.getMaximumAge()) {
            return false;
        }

        Material seed = seedForCrop(block.getType());
        if (seed == null || !player.getInventory().containsAtLeast(new ItemStack(seed), 1)) {
            return false;
        }

        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(cropDrop(block.getType())));
        player.getInventory().removeItem(new ItemStack(seed, 1));
        block.setType(block.getType());
        Ageable replanted = (Ageable) block.getBlockData();
        replanted.setAge(0);
        block.setBlockData(replanted);
        damageTool(player, tool);
        return true;
    }

    // This searches outward from one block and breaks matching nearby blocks.
    // It uses a queue, which means blocks are checked in the order they are discovered.
    private void breakConnectedBlocks(Player player, ItemStack tool, Block origin, int maxBlocks, boolean sameMaterialOnly) {
        Material originType = origin.getType();
        Queue<Block> queue = new ArrayDeque<>();
        Set<Location> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin.getLocation());

        int broken = 0;
        while (!queue.isEmpty() && broken < maxBlocks) {
            Block current = queue.poll();
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Block next = current.getRelative(offset[0], offset[1], offset[2]);
                if (!visited.add(next.getLocation()) || !canChainBreak(next, originType, sameMaterialOnly)) {
                    continue;
                }

                queue.add(next);
                Location nextLocation = next.getLocation();
                pluginBrokenBlocks.add(nextLocation);
                try {
                    if (player.getGameMode() == GameMode.CREATIVE) {
                        next.setType(Material.AIR);
                    } else {
                        next.breakNaturally(tool, true);
                        damageTool(player, tool);
                    }
                } finally {
                    pluginBrokenBlocks.remove(nextLocation);
                }
                broken++;
                if (broken >= maxBlocks) {
                    return;
                }
            }
        }
    }

    // Decides whether a nearby block should be included in Veinminer or Lumberjack.
    private boolean canChainBreak(Block block, Material originType, boolean sameMaterialOnly) {
        if (sameMaterialOnly) {
            return block.getType() == originType;
        }
        return Tag.LOGS.isTagged(block.getType());
    }

    // Damages the tool by 1 durability point, unless the player is in creative mode.
    private void damageTool(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        tool.damage(1, player);
    }

    // Checks whether a material belongs to any of Minecraft's ore tags.
    private boolean isOre(Material material) {
        return Tag.COAL_ORES.isTagged(material)
                || Tag.COPPER_ORES.isTagged(material)
                || Tag.DIAMOND_ORES.isTagged(material)
                || Tag.EMERALD_ORES.isTagged(material)
                || Tag.GOLD_ORES.isTagged(material)
                || Tag.IRON_ORES.isTagged(material)
                || Tag.LAPIS_ORES.isTagged(material)
                || Tag.REDSTONE_ORES.isTagged(material);
    }

    // Returns the seed item needed to replant each crop.
    private Material seedForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> null;
        };
    }

    // Returns the main crop item that should drop when Replant harvests a crop.
    private Material cropDrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            default -> crop;
        };
    }
}
