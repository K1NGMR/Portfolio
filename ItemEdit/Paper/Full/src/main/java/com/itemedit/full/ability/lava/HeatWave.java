package com.itemedit.full.ability.lava;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.itemedit.full.utils.CompatRunnable;

public class HeatWave extends Ability {
    private final ItemEditFull plugin;

    public HeatWave(ItemEditFull plugin) {
        super("heat_wave", "Heat Wave", "Releases an expanding ring of fire that burns nearby enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double maxRadius = getDoubleParam(plugin, item, "radius", 5.0);
        double damage = getDoubleParam(plugin, item, "damage", 4.0);
        int fireTicks = getIntParam(plugin, item, "fire_ticks", 80);

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

                int particleCount = (int) (currentRadius * 8);
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / particleCount;
                    double x = currentRadius * Math.cos(angle);
                    double z = currentRadius * Math.sin(angle);
                    Location pLoc = origin.clone().add(x, 0.5, z);
                    pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0, 0, 0, 0.02);
                }

                for (Entity entity : origin.getWorld().getNearbyEntities(origin, currentRadius, 2.0, currentRadius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity living = (LivingEntity) entity;
                        double distance = living.getLocation().distance(origin);
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
