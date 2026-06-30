package com.itemedit.light.ability.undead;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.light.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ZombieAbilities implements Listener {
    private static final Map<UUID, Long> activeRages = new HashMap<>();
    private static final Map<UUID, Long> infectedEntities = new HashMap<>();
    private static ItemEditLight pluginInstance;

    public static void register(ItemEditLight plugin) {
        pluginInstance = plugin;
        plugin.getAbilityManager().registerAbility(new ZombieSwarm(plugin));
        plugin.getAbilityManager().registerAbility(new UndeadBite(plugin));
        plugin.getAbilityManager().registerAbility(new ZombieInfection(plugin));
        plugin.getAbilityManager().registerAbility(new ZombieRage(plugin));
        plugin.getAbilityManager().registerAbility(new UndeadCall(plugin));
        
        plugin.getServer().getPluginManager().registerEvents(new ZombieAbilities(), plugin);
    }

    public static void registerRage(UUID uuid, long expireTime) {
        activeRages.put(uuid, expireTime);
    }

    public static void registerInfected(UUID uuid, long expireTime) {
        infectedEntities.put(uuid, expireTime);
    }

    @EventHandler
    public void onHelperTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player && event.getEntity().hasMetadata("helper")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHelperDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager().hasMetadata("helper")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRageDamage(EntityDamageByEntityEvent event) {
        // Double damage dealt by player during rage
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Long expire = activeRages.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                event.setDamage(event.getDamage() * 2.0);
                player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
            }
        }
        // Extra damage taken by player during rage
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Long expire = activeRages.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                event.setDamage(event.getDamage() * 1.5);
            }
        }
    }

    @EventHandler
    public void onInfectedDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Long expire = infectedEntities.get(victim.getUniqueId());
        if (expire != null && System.currentTimeMillis() < expire) {
            infectedEntities.remove(victim.getUniqueId());
            Location loc = victim.getLocation();
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 0.3, 0.5, 0.3, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.8f);

            Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
            zombie.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            if (pluginInstance != null) {
                zombie.setMetadata("helper", new FixedMetadataValue(pluginInstance, "true"));
            }
        }
    }
}

class ZombieSwarm extends Ability {
    private final ItemEditLight plugin;

    public ZombieSwarm(ItemEditLight plugin) {
        super("zombie_swarm", "Zombie Swarm", "Summons 2 helper zombies equipped with helmets that decay after 15 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.8f);

        List<Zombie> summoned = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            double angle = i * Math.PI;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            zombie.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            zombie.setMetadata("helper", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
            
            // Find a nearby target for the zombie
            for (Entity entity : zombie.getNearbyEntities(10.0, 5.0, 10.0)) {
                if (entity instanceof LivingEntity && !(entity instanceof Player) && !entity.hasMetadata("helper")) {
                    zombie.setTarget((LivingEntity) entity);
                    break;
                }
            }
            summoned.add(zombie);
        }

        new CompatRunnable() {
            @Override
            public void run() {
                for (Zombie z : summoned) {
                    if (z.isValid()) {
                        z.getWorld().spawnParticle(Particle.SMOKE_NORMAL, z.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.01);
                        z.remove();
                    }
                }
            }
        }.runTaskLater(plugin, player, 300L); // 15 seconds

        return true;
    }
}

class UndeadBite extends Ability {
    private final ItemEditLight plugin;

    public UndeadBite(ItemEditLight plugin) {
        super("undead_bite", "Undead Bite", "Bites the targeted entity, dealing damage, stealing saturation, and healing you.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(5);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in range.");
            return false;
        }

        LivingEntity living = (LivingEntity) target;
        living.damage(4.0, player);
        living.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1));
        
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 4));
        player.setSaturation(Math.min(20.0f, player.getSaturation() + 4.0f));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.02);
        
        return true;
    }
}

class ZombieInfection extends Ability {
    private final ItemEditLight plugin;

    public ZombieInfection(ItemEditLight plugin) {
        super("zombie_infection", "Undead Infection", "Infects target. If they die within 10 seconds, they rise as a helper zombie.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(6);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in range.");
            return false;
        }

        LivingEntity living = (LivingEntity) target;
        living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0));
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.1f);
        ZombieAbilities.registerInfected(living.getUniqueId(), System.currentTimeMillis() + 10000);

        new CompatRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count > 5 || !living.isValid()) {
                    cancel();
                    return;
                }
                living.getWorld().spawnParticle(Particle.SPELL_MOB, living.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0);
                count++;
            }
        }.runTaskTimer(plugin, living, 0L, 20L);

        return true;
    }
}

class ZombieRage extends Ability {
    private final ItemEditLight plugin;

    public ZombieRage(ItemEditLight plugin) {
        super("zombie_rage", "Zombie Rage", "Deal 2x damage but take 1.5x damage for 6 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_HURT, 1.0f, 0.7f);
        ZombieAbilities.registerRage(player.getUniqueId(), System.currentTimeMillis() + 6000);

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 6 || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, player, 0L, 20L);

        return true;
    }
}

class UndeadCall extends Ability {
    private final ItemEditLight plugin;

    public UndeadCall(ItemEditLight plugin) {
        super("undead_call", "Undead Call", "Pulls all nearby vanilla zombies within 20 blocks towards you.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 1.2f, 0.7f);
        
        int count = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(loc, 20.0, 10.0, 20.0)) {
            if (entity instanceof Zombie && !entity.hasMetadata("helper")) {
                Zombie zombie = (Zombie) entity;
                Vector dir = loc.toVector().subtract(zombie.getLocation().toVector()).normalize();
                zombie.setVelocity(dir.multiply(1.5).setY(0.4));
                zombie.getWorld().spawnParticle(Particle.PORTAL, zombie.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                zombie.setTarget(player); // anger vanilla zombies towards caster
                count++;
            }
        }
        player.sendMessage("§aCalled " + count + " nearby zombies!");
        return true;
    }
}
