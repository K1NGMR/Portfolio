package com.itemedit.full.ability.end;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.full.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class EndAbilities implements org.bukkit.event.Listener {
    private static ItemEditFull pluginInstance;

    public static void register(ItemEditFull plugin) {
        pluginInstance = plugin;
        plugin.getAbilityManager().registerAbility(new EnderBlink(plugin));
        plugin.getAbilityManager().registerAbility(new PearlStorm(plugin));
        plugin.getAbilityManager().registerAbility(new EnderSwap(plugin));
        plugin.getAbilityManager().registerAbility(new EnderRift(plugin));
        plugin.getAbilityManager().registerAbility(new EnderShriek(plugin));
        plugin.getAbilityManager().registerAbility(new ShulkerLevitate(plugin));
        plugin.getAbilityManager().registerAbility(new DragonBreath(plugin));

        plugin.getServer().getPluginManager().registerEvents(new EndAbilities(), plugin);
    }

    @org.bukkit.event.EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof ShulkerBullet && event.getHitEntity() instanceof LivingEntity) {
            ShulkerBullet bullet = (ShulkerBullet) event.getEntity();
            if (bullet.hasMetadata("levitate_duration")) {
                double dur = bullet.getMetadata("levitate_duration").get(0).asDouble();
                int amp = bullet.hasMetadata("levitate_amplifier") ? bullet.getMetadata("levitate_amplifier").get(0).asInt() : 0;
                LivingEntity hit = (LivingEntity) event.getHitEntity();
                new CompatRunnable() {
                    @Override
                    public void run() {
                        hit.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) (dur * 20), amp), true);
                    }
                }.runTaskLater(pluginInstance, hit, 1L);
            }
        }
    }

    public static Vector rotateY(Vector v, double angleDegrees) {
        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }
}

class EnderBlink extends Ability {
    private final ItemEditFull plugin;

    public EnderBlink(ItemEditFull plugin) {
        super("ender_blink", "Ender Blink", "Instantly teleports you forward safely up to 12 blocks.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double range = getDoubleParam(plugin, item, "range", 12.0);

        Location loc = player.getLocation();
        Vector dir = loc.getDirection().normalize();
        
        Location target = loc.clone();
        for (double d = 1.0; d <= range; d += 0.5) {
            Location check = loc.clone().add(dir.clone().multiply(d));
            Block feet = check.getBlock();
            Block head = check.clone().add(0, 1, 0).getBlock();
            
            if (feet.getType().isSolid() || head.getType().isSolid()) {
                break;
            }
            target = check;
        }

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        player.teleport(target.setDirection(loc.getDirection()));
        
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        return true;
    }
}

class PearlStorm extends Ability {
    private final ItemEditFull plugin;

    public PearlStorm(ItemEditFull plugin) {
        super("pearl_storm", "Pearl Storm", "Fires 3 ender pearls in a horizontal spread.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double velocity = getDoubleParam(plugin, item, "velocity", 1.5);
        double angle = getDoubleParam(plugin, item, "angle", 12.0);

        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector left = EndAbilities.rotateY(dir, -angle);
        Vector right = EndAbilities.rotateY(dir, angle);

        EnderPearl p1 = player.launchProjectile(EnderPearl.class);
        p1.setVelocity(dir.multiply(velocity));
        
        EnderPearl p2 = player.launchProjectile(EnderPearl.class);
        p2.setVelocity(left.multiply(velocity));
        
        EnderPearl p3 = player.launchProjectile(EnderPearl.class);
        p3.setVelocity(right.multiply(velocity));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 0.8f);
        return true;
    }
}

class EnderSwap extends Ability {
    private final ItemEditFull plugin;

    public EnderSwap(ItemEditFull plugin) {
        super("ender_swap", "Ender Swap", "Swaps locations with the targeted entity within 20 blocks.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double range = getDoubleParam(plugin, item, "range", 20.0);

        Entity target = player.getTargetEntity((int) range);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in range.");
            return false;
        }

        LivingEntity living = (LivingEntity) target;
        Location pLoc = player.getLocation();
        Location tLoc = living.getLocation();

        player.getWorld().spawnParticle(Particle.PORTAL, pLoc.add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.05);
        tLoc.getWorld().spawnParticle(Particle.PORTAL, tLoc.add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.05);

        player.teleport(living.getLocation().setDirection(player.getLocation().getDirection()));
        living.teleport(pLoc.setDirection(living.getLocation().getDirection()));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        return true;
    }
}

class EnderRift extends Ability {
    private final ItemEditFull plugin;

    public EnderRift(ItemEditFull plugin) {
        super("ender_rift", "Ender Rift", "Creates a vortex at target block that pulls in nearby entities for 3 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 6.0);
        double duration = getDoubleParam(plugin, item, "duration", 3.0);
        double pullStrength = getDoubleParam(plugin, item, "pull_strength", 0.4);
        double yStrength = getDoubleParam(plugin, item, "y_strength", 0.15);

        Block target = player.getTargetBlockExact(25);
        if (target == null) {
            player.sendMessage("§cNo block in sight.");
            return false;
        }

        Location riftLoc = target.getLocation().add(0.5, 1.5, 0.5);
        riftLoc.getWorld().playSound(riftLoc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > (duration * 5) || !player.isOnline()) {
                    cancel();
                    return;
                }

                riftLoc.getWorld().spawnParticle(Particle.PORTAL, riftLoc, 15, 0.5, 0.5, 0.5, 0.1);
                riftLoc.getWorld().playSound(riftLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 1.5f);

                for (Entity entity : riftLoc.getWorld().getNearbyEntities(riftLoc, radius, 4.0, radius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        Vector dir = riftLoc.toVector().subtract(entity.getLocation().toVector());
                        double dist = dir.length();
                        if (dist > 0.5) {
                            entity.setVelocity(dir.normalize().multiply(pullStrength).setY(yStrength));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, riftLoc, 0L, 4L);

        return true;
    }
}

class EnderShriek extends Ability {
    private final ItemEditFull plugin;

    public EnderShriek(ItemEditFull plugin) {
        super("ender_shriek", "Ender Shriek", "Screams and disorients nearby enemies with high Nausea and Slowness.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 8.0);
        double damage = getDoubleParam(plugin, item, "damage", 2.0);
        double nauseaDuration = getDoubleParam(plugin, item, "nausea_duration", 8.0);
        int nauseaAmp = getIntParam(plugin, item, "nausea_amplifier", 1);
        double slowDuration = getDoubleParam(plugin, item, "slow_duration", 5.0);
        int slowAmp = getIntParam(plugin, item, "slow_amplifier", 2);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 1.2f, 0.7f);

        for (Entity entity : player.getWorld().getNearbyEntities(loc, radius, 4.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                living.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (nauseaDuration * 20), nauseaAmp));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (slowDuration * 20), slowAmp));
                living.damage(damage, player);
                living.getWorld().spawnParticle(Particle.CHERRY_LEAVES, living.getLocation().add(0, 1.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }
        return true;
    }
}

class ShulkerLevitate extends Ability {
    private final ItemEditFull plugin;

    public ShulkerLevitate(ItemEditFull plugin) {
        super("shulker_levitate", "Shulker Levitate", "Launches a tracking shulker bullet that causes target to levitate.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 4.0);
        int levitateAmp = getIntParam(plugin, item, "levitation_amplifier", 0);
        double range = getDoubleParam(plugin, item, "range", 25.0);

        Entity target = player.getTargetEntity((int) range);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        ShulkerBullet bullet = eyeLoc.getWorld().spawn(eyeLoc.add(eyeLoc.getDirection().multiply(1.5)), ShulkerBullet.class);
        bullet.setShooter(player);
        bullet.setTarget(target);
        
        bullet.setMetadata("levitate_duration", new org.bukkit.metadata.FixedMetadataValue(plugin, duration));
        bullet.setMetadata("levitate_amplifier", new org.bukkit.metadata.FixedMetadataValue(plugin, levitateAmp));
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.0f);
        return true;
    }
}

class DragonBreath extends Ability {
    private final ItemEditFull plugin;

    public DragonBreath(ItemEditFull plugin) {
        super("dragon_breath", "Dragon's Breath", "Spawns a lingering cloud of dragon breath that deals magic damage.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 3.0);
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        int harmAmp = getIntParam(plugin, item, "harm_amplifier", 0);
        double range = getDoubleParam(plugin, item, "range", 15.0);

        Block target = player.getTargetBlockExact((int) range);
        Location spawnLoc = (target != null) ? target.getLocation().add(0.5, 1.0, 0.5) : player.getLocation();

        player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.0f);

        AreaEffectCloud cloud = (AreaEffectCloud) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setRadius((float) radius);
        cloud.setDuration((int) (duration * 20));
        cloud.setWaitTime(0);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, harmAmp), true);

        return true;
    }
}
