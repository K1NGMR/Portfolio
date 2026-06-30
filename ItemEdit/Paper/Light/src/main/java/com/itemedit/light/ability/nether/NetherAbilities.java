package com.itemedit.light.ability.nether;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import com.itemedit.light.ability.end.EndAbilities;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.light.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class NetherAbilities implements Listener {
    private static ItemEditLight pluginInstance;

    public static void register(ItemEditLight plugin) {
        pluginInstance = plugin;
        plugin.getAbilityManager().registerAbility(new WitherDecay(plugin));
        plugin.getAbilityManager().registerAbility(new GhastBlast(plugin));
        plugin.getAbilityManager().registerAbility(new BlazeRodVolley(plugin));
        plugin.getAbilityManager().registerAbility(new MagmaSpit(plugin));
        plugin.getAbilityManager().registerAbility(new WitherBlast(plugin));
        
        plugin.getAbilityManager().registerAbility(new MeteorStrikeGeneric(plugin, "meteor_strike_small", "Small Meteor Strike", 4.0, 3.0, 1));
        plugin.getAbilityManager().registerAbility(new MeteorStrikeGeneric(plugin, "meteor_strike", "Meteor Strike", 8.0, 4.0, 2));
        plugin.getAbilityManager().registerAbility(new MeteorStrikeGeneric(plugin, "meteor_strike_large", "Large Meteor Strike", 12.0, 6.0, 3));
        plugin.getAbilityManager().registerAbility(new MeteorStrikeGeneric(plugin, "meteor_strike_gigantic", "Gigantic Meteor Strike", 20.0, 10.0, 4));

        plugin.getServer().getPluginManager().registerEvents(new NetherAbilities(), plugin);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Fireball || entity instanceof WitherSkull) {
            // Prevent all block damage from custom meteors and wither skulls
            if (entity.hasMetadata("meteor_damage") || entity.hasMetadata("custom_ability")) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();
        if (entity.hasMetadata("meteor_damage")) {
            double damage = entity.getMetadata("meteor_damage").get(0).asDouble();
            double radius = entity.getMetadata("meteor_radius").get(0).asDouble();
            int size = entity.getMetadata("meteor_size").get(0).asInt();
            Player shooter = (Player) event.getEntity().getShooter();

            Location loc = event.getHitBlock() != null ? 
                event.getHitBlock().getLocation().add(0.5, 1.0, 0.5) : entity.getLocation();

            // Trigger manual AoE damage
            for (Entity vic : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (vic instanceof LivingEntity && !vic.equals(shooter)) {
                    ((LivingEntity) vic).damage(damage, shooter);
                    vic.setVelocity(vic.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.4));
                }
            }

            // Custom visuals based on size
            if (size == 1) { // Small
                loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            } else if (size == 2) { // Medium
                loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
            } else if (size == 3) { // Large
                loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 3, 0.3, 0.3, 0.3, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.9f);
            } else if (size == 4) { // Gigantic
                loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 8, 0.6, 0.6, 0.6, 0.1);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.6f);
                loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5f, 0.7f);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
            }
        }
    }
}

class WitherDecay extends Ability {
    private final ItemEditLight plugin;

    public WitherDecay(ItemEditLight plugin) {
        super("wither_decay", "Wither Decay", "Launches a wither skull that explodes and infects targets with Wither.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        WitherSkull skull = player.launchProjectile(WitherSkull.class);
        skull.setMetadata("custom_ability", new FixedMetadataValue(plugin, true));
        skull.setYield(0.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
        return true;
    }
}

class GhastBlast extends Ability {
    private final ItemEditLight plugin;

    public GhastBlast(ItemEditLight plugin) {
        super("ghast_blast", "Ghast Blast", "Fires a large fireball that explodes on impact.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        LargeFireball fireball = player.launchProjectile(LargeFireball.class);
        fireball.setMetadata("custom_ability", new FixedMetadataValue(plugin, true));
        fireball.setYield(0.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        return true;
    }
}

class BlazeRodVolley extends Ability {
    private final ItemEditLight plugin;

    public BlazeRodVolley(ItemEditLight plugin) {
        super("blaze_rod_volley", "Blaze Rod Volley", "Launches 5 burning fireballs in a cone spread.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.0f);

        for (int i = -2; i <= 2; i++) {
            Vector spreadDir = EndAbilities.rotateY(dir.clone(), i * 8);
            SmallFireball sb = player.launchProjectile(SmallFireball.class);
            sb.setVelocity(spreadDir.multiply(1.5));
            sb.setMetadata("custom_ability", new FixedMetadataValue(plugin, true));
        }
        return true;
    }
}

class MagmaSpit extends Ability {
    private final ItemEditLight plugin;

    public MagmaSpit(ItemEditLight plugin) {
        super("magma_spit", "Magma Spit", "Places magma blocks beneath the targeted entity's feet.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(10);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

        Block block = target.getLocation().subtract(0, 1, 0).getBlock();
        if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
            Material original = block.getType();
            Location blockLoc = block.getLocation();
            block.setType(Material.MAGMA_BLOCK);
            
            new CompatRunnable() {
                @Override
                public void run() {
                    if (blockLoc.getBlock().getType() == Material.MAGMA_BLOCK) {
                        blockLoc.getBlock().setType(original);
                    }
                }
            }.runTaskLater(plugin, blockLoc, 100L); // 5 seconds revert
        }
        return true;
    }
}

class WitherBlast extends Ability {
    private final ItemEditLight plugin;

    public WitherBlast(ItemEditLight plugin) {
        super("wither_blast", "Wither Blast", "Releases an explosion around you, pushing back enemies and applying wither.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.2f, 0.8f);

        for (Entity vic : loc.getWorld().getNearbyEntities(loc, 6.0, 3.0, 6.0)) {
            if (vic instanceof LivingEntity && !vic.equals(player)) {
                LivingEntity living = (LivingEntity) vic;
                living.damage(6.0, player);
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                living.setVelocity(living.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.4));
            }
        }
        return true;
    }
}

class MeteorStrikeGeneric extends Ability {
    private final ItemEditLight plugin;
    private final double damage;
    private final double radius;
    private final int size;

    public MeteorStrikeGeneric(ItemEditLight plugin, String id, String name, double damage, double radius, int size) {
        super(id, name, "Summons a falling meteor that strikes the targeted area.");
        this.plugin = plugin;
        this.damage = damage;
        this.radius = radius;
        this.size = size;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Block target = player.getTargetBlockExact(30);
        if (target == null) {
            player.sendMessage("§cNo target location in range.");
            return false;
        }

        Location targetLoc = target.getLocation().add(0.5, 1.0, 0.5);
        Location startLoc = targetLoc.clone().add(2.0, 20.0, 2.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);

        LargeFireball meteor = (LargeFireball) startLoc.getWorld().spawnEntity(startLoc, EntityType.FIREBALL);
        meteor.setShooter(player);
        meteor.setDirection(new Vector(-0.1, -1.0, -0.1).normalize());
        meteor.setYield(0.0f);
        
        meteor.setMetadata("meteor_damage", new FixedMetadataValue(plugin, damage));
        meteor.setMetadata("meteor_radius", new FixedMetadataValue(plugin, radius));
        meteor.setMetadata("meteor_size", new FixedMetadataValue(plugin, size));

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!meteor.isValid() || ticks > 100) {
                    cancel();
                    return;
                }
                meteor.getWorld().spawnParticle(Particle.FLAME, meteor.getLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                ticks++;
            }
        }.runTaskTimer(plugin, meteor, 0L, 2L);

        return true;
    }
}
