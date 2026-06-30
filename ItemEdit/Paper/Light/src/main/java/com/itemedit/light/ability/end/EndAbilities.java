package com.itemedit.light.ability.end;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
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
import com.itemedit.light.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class EndAbilities {
    public static void register(ItemEditLight plugin) {
        plugin.getAbilityManager().registerAbility(new EnderBlink(plugin));
        plugin.getAbilityManager().registerAbility(new PearlStorm(plugin));
        plugin.getAbilityManager().registerAbility(new EnderSwap(plugin));
        plugin.getAbilityManager().registerAbility(new EnderRift(plugin));
        plugin.getAbilityManager().registerAbility(new EnderShriek(plugin));
        plugin.getAbilityManager().registerAbility(new ShulkerLevitate(plugin));
        plugin.getAbilityManager().registerAbility(new DragonBreath(plugin));
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
    private final ItemEditLight plugin;

    public EnderBlink(ItemEditLight plugin) {
        super("ender_blink", "Ender Blink", "Instantly teleports you forward safely up to 12 blocks.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double range = plugin.getConfig().getDouble("abilities.ender_blink.range", 12.0);

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
    private final ItemEditLight plugin;

    public PearlStorm(ItemEditLight plugin) {
        super("pearl_storm", "Pearl Storm", "Fires 3 ender pearls in a horizontal spread.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector left = EndAbilities.rotateY(dir, -12);
        Vector right = EndAbilities.rotateY(dir, 12);

        EnderPearl p1 = player.launchProjectile(EnderPearl.class);
        p1.setVelocity(dir.multiply(1.5));
        
        EnderPearl p2 = player.launchProjectile(EnderPearl.class);
        p2.setVelocity(left.multiply(1.5));
        
        EnderPearl p3 = player.launchProjectile(EnderPearl.class);
        p3.setVelocity(right.multiply(1.5));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 0.8f);
        return true;
    }
}

class EnderSwap extends Ability {
    private final ItemEditLight plugin;

    public EnderSwap(ItemEditLight plugin) {
        super("ender_swap", "Ender Swap", "Swaps locations with the targeted entity within 20 blocks.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(20);
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
    private final ItemEditLight plugin;

    public EnderRift(ItemEditLight plugin) {
        super("ender_rift", "Ender Rift", "Creates a vortex at target block that pulls in nearby entities for 3 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
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
                if (ticks > 15 || !player.isOnline()) {
                    cancel();
                    return;
                }

                riftLoc.getWorld().spawnParticle(Particle.PORTAL, riftLoc, 15, 0.5, 0.5, 0.5, 0.1);
                riftLoc.getWorld().playSound(riftLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 1.5f);

                for (Entity entity : riftLoc.getWorld().getNearbyEntities(riftLoc, 6.0, 4.0, 6.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        Vector dir = riftLoc.toVector().subtract(entity.getLocation().toVector());
                        double dist = dir.length();
                        if (dist > 0.5) {
                            entity.setVelocity(dir.normalize().multiply(0.4).setY(0.15));
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
    private final ItemEditLight plugin;

    public EnderShriek(ItemEditLight plugin) {
        super("ender_shriek", "Ender Shriek", "Screams and disorients nearby enemies with high Nausea and Slowness.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 1.2f, 0.7f);

        for (Entity entity : player.getWorld().getNearbyEntities(loc, 8.0, 4.0, 8.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                living.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 1));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                living.damage(2.0, player);
                living.getWorld().spawnParticle(Particle.CHERRY_LEAVES, living.getLocation().add(0, 1.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }
        return true;
    }
}

class ShulkerLevitate extends Ability {
    private final ItemEditLight plugin;

    public ShulkerLevitate(ItemEditLight plugin) {
        super("shulker_levitate", "Shulker Levitate", "Launches a tracking shulker bullet that causes target to levitate.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(25);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        ShulkerBullet bullet = eyeLoc.getWorld().spawn(eyeLoc.add(eyeLoc.getDirection().multiply(1.5)), ShulkerBullet.class);
        bullet.setShooter(player);
        bullet.setTarget(target);
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.0f);
        return true;
    }
}

class DragonBreath extends Ability {
    private final ItemEditLight plugin;

    public DragonBreath(ItemEditLight plugin) {
        super("dragon_breath", "Dragon's Breath", "Spawns a lingering cloud of dragon breath that deals magic damage.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Block target = player.getTargetBlockExact(15);
        Location spawnLoc = (target != null) ? target.getLocation().add(0.5, 1.0, 0.5) : player.getLocation();

        player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.0f);

        AreaEffectCloud cloud = (AreaEffectCloud) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setRadius(3.0f);
        cloud.setDuration(100); // 5 seconds
        cloud.setWaitTime(0);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 0), true);

        return true;
    }
}
