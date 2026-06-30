package com.itemedit.full.ability.warden;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import com.itemedit.full.utils.CompatRunnable;

public class WardenAbilities {
    public static void register(ItemEditFull plugin) {
        plugin.getAbilityManager().registerAbility(new SonicBoom(plugin));
        plugin.getAbilityManager().registerAbility(new SculkInfestation(plugin));
        plugin.getAbilityManager().registerAbility(new WardenSonicClap(plugin));
        plugin.getAbilityManager().registerAbility(new SculkSensorTrap(plugin));
        plugin.getAbilityManager().registerAbility(new WardenRage(plugin));
    }
}

class SonicBoom extends Ability {
    private final ItemEditFull plugin;

    public SonicBoom(ItemEditFull plugin) {
        super("sonic_boom", "Sonic Boom", "Launches a linear sonic blast that deals magic damage and high knockback.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 6.0);
        double range = getDoubleParam(plugin, item, "range", 15.0);
        double knockback = getDoubleParam(plugin, item, "knockback", 1.5);

        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        player.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);

        for (int i = 0; i < range; i++) {
            Location point = origin.clone().add(dir.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {
                    ((LivingEntity) entity).damage(damage, player);
                    entity.setVelocity(dir.clone().multiply(knockback).setY(0.4));
                }
            }
        }
        return true;
    }
}

class SculkInfestation extends Ability {
    private final ItemEditFull plugin;

    public SculkInfestation(ItemEditFull plugin) {
        super("sculk_infestation", "Sculk Infestation", "Releases sculk energy, causing nearby enemies to wither and go dark.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 4.0);
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        int darknessAmp = getIntParam(plugin, item, "darkness_amplifier", 0);
        int slowAmp = getIntParam(plugin, item, "slow_amplifier", 1);
        int witherAmp = getIntParam(plugin, item, "wither_amplifier", 0);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.2f, 0.8f);

        for (int i = 0; i < 30; i++) {
            double angle = i * 2 * Math.PI / 30;
            Location particleLoc = loc.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
            particleLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, particleLoc, 1, 0, 0.1, 0, 0.05);
        }

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) (duration * 20), darknessAmp));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration * 20), slowAmp));
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration * 20), witherAmp));
            }
        }
        return true;
    }
}

class WardenSonicClap extends Ability {
    private final ItemEditFull plugin;

    public WardenSonicClap(ItemEditFull plugin) {
        super("warden_sonic_clap", "Sonic Clap", "Knocks back and blinds enemies in a short cone in front of you.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 4.0);
        double range = getDoubleParam(plugin, item, "range", 6.0);
        double blindDuration = getDoubleParam(plugin, item, "blindness_duration", 4.0);
        int blindAmp = getIntParam(plugin, item, "blindness_amplifier", 0);
        double knockback = getDoubleParam(plugin, item, "knockback", 1.8);

        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.2f);

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), range, 3.0, range)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                if (toEntity.normalize().dot(dir) > 0.7) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(damage, player);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (blindDuration * 20), blindAmp));
                    living.setVelocity(dir.clone().multiply(knockback).setY(0.5));
                }
            }
        }
        return true;
    }
}

class SculkSensorTrap extends Ability {
    private final ItemEditFull plugin;

    public SculkSensorTrap(ItemEditFull plugin) {
        super("sculk_sensor_trap", "Sculk Sensor Trap", "Places an invisible trap. Explodes with sculk energy when enemies step on it.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 6.0);
        double radius = getDoubleParam(plugin, item, "radius", 4.0);
        double trapDuration = getDoubleParam(plugin, item, "trap_duration", 15.0);
        double triggerRadius = getDoubleParam(plugin, item, "trigger_radius", 1.5);
        double darknessDuration = getDoubleParam(plugin, item, "darkness_duration", 5.0);
        int darknessAmp = getIntParam(plugin, item, "darkness_amplifier", 0);

        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
        Location target = targetBlock != null ? targetBlock.getLocation() : player.getLocation();
        if (target == null) target = player.getLocation();
        else target = target.add(0, 1, 0);

        final Location trapLoc = target.clone();
        trapLoc.getWorld().playSound(trapLoc, Sound.BLOCK_SCULK_SENSOR_PLACE, 1.0f, 1.0f);
        trapLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE, trapLoc, 10, 0.2, 0.2, 0.2, 0.05, 1.0f);

        new CompatRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (ticks > (trapDuration * 20)) {
                    cancel();
                    return;
                }

                trapLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, trapLoc, 1, 0.05, 0.05, 0.05, 0);

                for (Entity entity : trapLoc.getWorld().getNearbyEntities(trapLoc, triggerRadius, triggerRadius, triggerRadius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        trapLoc.getWorld().playSound(trapLoc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.5f, 1.0f);
                        trapLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, trapLoc, 1, 0, 0, 0, 0);
                        
                        for (Entity victim : trapLoc.getWorld().getNearbyEntities(trapLoc, radius, 3.0, radius)) {
                            if (victim instanceof LivingEntity && !victim.equals(player)) {
                                ((LivingEntity) victim).damage(damage, player);
                                ((LivingEntity) victim).addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) (darknessDuration * 20), darknessAmp));
                            }
                        }
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, trapLoc, 0L, 5L);

        return true;
    }
}

class WardenRage extends Ability {
    private final ItemEditFull plugin;

    public WardenRage(ItemEditFull plugin) {
        super("warden_rage", "Warden Rage", "Shrouds you in sculk energy, granting Strength II and Resistance II.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 8.0);
        int strengthAmp = getIntParam(plugin, item, "strength_amplifier", 1);
        int resistanceAmp = getIntParam(plugin, item, "resistance_amplifier", 1);
        double darknessDuration = getDoubleParam(plugin, item, "darkness_duration", 3.0);
        int darknessAmp = getIntParam(plugin, item, "darkness_amplifier", 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.2f, 1.0f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (duration * 20), strengthAmp));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) (duration * 20), resistanceAmp));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) (darknessDuration * 20), darknessAmp));

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
                ticks++;
            }
        }.runTaskTimer(plugin, player, 0L, 20L);

        return true;
    }
}
