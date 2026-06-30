package com.itemedit.light.ability.lava;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.itemedit.light.utils.CompatRunnable;

public class HeatWave extends Ability {
    private final ItemEditLight plugin;

    public HeatWave(ItemEditLight plugin) {
        super("heat_wave", "Heat Wave", "Releases an expanding ring of fire that burns nearby enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double maxRadius = plugin.getConfig().getDouble("abilities.heat_wave.radius", 5.0);
        double damage = plugin.getConfig().getDouble("abilities.heat_wave.damage", 4.0);
        int fireTicks = plugin.getConfig().getInt("abilities.heat_wave.fire_ticks", 80);

        Location origin = player.getLocation();
        player.getWorld().playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);

        new CompatRunnable() {
            double currentRadius = 1.0;

            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    cancel();
                    return;
                }

                // Spawn particle circle
                int particleCount = (int) (currentRadius * 8);
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / particleCount;
                    double x = currentRadius * Math.cos(angle);
                    double z = currentRadius * Math.sin(angle);
                    Location pLoc = origin.clone().add(x, 0.5, z);
                    pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0, 0, 0, 0.02);
                }

                // Damage entities near the current radius
                for (Entity entity : origin.getWorld().getNearbyEntities(origin, currentRadius, 2.0, currentRadius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity living = (LivingEntity) entity;
                        double distance = living.getLocation().distance(origin);
                        // Check if entity is on the ring (with tolerance of 1 block)
                        if (Math.abs(distance - currentRadius) <= 1.0) {
                            living.damage(damage, player);
                            living.setFireTicks(fireTicks);
                        }
                    }
                }

                currentRadius += 1.0;
            }
        }.runTaskTimer(plugin, player, 0L, 2L);

        return true;
    }
}
