package com.itemedit.light.ability.lava;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.light.utils.CompatRunnable;

import java.util.HashSet;
import java.util.Set;

public class LavaWalker extends Ability {
    private final ItemEditLight plugin;
    private final Set<Location> modifiedBlocks = new HashSet<>();

    public LavaWalker(ItemEditLight plugin) {
        super("lava_walker", "Lava Walker", "Allows you to walk on lava by turning it to obsidian/magma.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = plugin.getConfig().getDouble("abilities.lava_walker.duration", 10.0);
        int radius = plugin.getConfig().getInt("abilities.lava_walker.radius", 3);

        // Apply Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) (duration * 20), 0));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

        // Periodically turn lava to magma/obsidian under feet
        new CompatRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticksElapsed >= (duration * 20)) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().subtract(0, 1, 0);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + z * z <= radius * radius) {
                            Block block = loc.clone().add(x, 0, z).getBlock();
                            if (block.getType() == Material.LAVA) {
                                Material originalType = block.getType();
                                Location blockLoc = block.getLocation();

                                if (!modifiedBlocks.contains(blockLoc)) {
                                    modifiedBlocks.add(blockLoc);
                                    // Turn to magma or obsidian
                                    block.setType(Material.MAGMA_BLOCK);

                                    // Schedule reversion
                                    new CompatRunnable() {
                                        @Override
                                        public void run() {
                                            if (blockLoc.getBlock().getType() == Material.MAGMA_BLOCK) {
                                                blockLoc.getBlock().setType(originalType);
                                            }
                                            modifiedBlocks.remove(blockLoc);
                                        }
                                    }.runTaskLater(plugin, blockLoc, 80L); // 4 seconds
                                }
                            }
                        }
                    }
                }
                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, player, 0L, 2L);

        return true;
    }
}
