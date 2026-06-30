package com.itemedit.full.ability.undead;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.full.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkeletonAbilities implements Listener {
    private static final Map<UUID, Long> activeShields = new HashMap<>();
    private static final Map<UUID, Long> activeWitherStrikes = new HashMap<>();
    private static ItemEditFull pluginInstance;

    public static void register(ItemEditFull plugin) {
        pluginInstance = plugin;
        plugin.getAbilityManager().registerAbility(new BoneShield(plugin));
        plugin.getAbilityManager().registerAbility(new ArrowHail(plugin));
        plugin.getAbilityManager().registerAbility(new SkeletonArchers(plugin));
        plugin.getAbilityManager().registerAbility(new BoneTrap(plugin));
        plugin.getAbilityManager().registerAbility(new WitherSkeletonStrike(plugin));
        
        plugin.getServer().getPluginManager().registerEvents(new SkeletonAbilities(), plugin);
    }

    public static void registerShield(UUID uuid, long expire) {
        activeShields.put(uuid, expire);
    }

    public static void registerWitherStrike(UUID uuid, long expire) {
        activeWitherStrikes.put(uuid, expire);
    }

    @EventHandler
    public void onShieldDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Long expire = activeShields.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                event.setCancelled(true);
                activeShields.remove(player.getUniqueId());
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1.5f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.2f, 1.0f);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 20, 0.2, 0.4, 0.2, 0.1);
                player.sendMessage("§fYour Bone Shield shattered, absorbing the damage!");
            }
        }
    }

    @EventHandler
    public void onWitherStrike(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Long expire = activeWitherStrikes.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                activeWitherStrikes.remove(player.getUniqueId());
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    Ability ab = pluginInstance.getAbilityManager().getAbility("wither_skeleton_strike");
                    double witherDuration = ab != null ? ab.getDoubleParam(hand, "wither_duration", 8.0) : 8.0;
                    int witherAmp = ab != null ? ab.getIntParam(hand, "wither_amplifier", 1) : 1;
                    double extraDmg = ab != null ? ab.getDoubleParam(hand, "extra_damage", 4.0) : 4.0;
                    
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (witherDuration * 20), witherAmp));
                    event.setDamage(event.getDamage() + extraDmg);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SKELETON_HURT, 1.0f, 0.9f);
                    target.getWorld().spawnParticle(Particle.SMOKE_NORMAL, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.05);
                }
            }
        }
    }
}

class BoneShield extends Ability {
    private final ItemEditFull plugin;

    public BoneShield(ItemEditFull plugin) {
        super("bone_shield", "Bone Shield", "Surrounds you with bone shards that negate/absorb your next damage event.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_PLACE, 1.2f, 0.9f);
        SkeletonAbilities.registerShield(player.getUniqueId(), System.currentTimeMillis() + (long) (duration * 1000));

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location pLoc = player.getLocation().add(0, 0.8, 0);
                double angle = (ticks * 0.5) % (2 * Math.PI);
                Location particleLoc1 = pLoc.clone().add(Math.cos(angle) * 0.8, 0, Math.sin(angle) * 0.8);
                Location particleLoc2 = pLoc.clone().add(Math.cos(angle + Math.PI) * 0.8, 0, Math.sin(angle + Math.PI) * 0.8);
                
                pLoc.getWorld().spawnParticle(Particle.CLOUD, particleLoc1, 1, 0, 0, 0, 0);
                pLoc.getWorld().spawnParticle(Particle.CLOUD, particleLoc2, 1, 0, 0, 0, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, player, 0L, 5L);

        return true;
    }
}

class ArrowHail extends Ability {
    private final ItemEditFull plugin;

    public ArrowHail(ItemEditFull plugin) {
        super("arrow_hail", "Arrow Hail", "Rains a volley of arrows targeting the location you look at.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        int count = getIntParam(plugin, item, "arrows", 10);
        double range = getDoubleParam(plugin, item, "range", 30.0);

        Block target = player.getTargetBlockExact((int) range);
        if (target == null) {
            player.sendMessage("§cNo target location in range.");
            return false;
        }

        Location targetLoc = target.getLocation().add(0.5, 0.5, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 0.8f);

        for (int i = 0; i < count; i++) {
            new CompatRunnable() {
                @Override
                public void run() {
                    Location spawnLoc = targetLoc.clone().add(
                        (Math.random() - 0.5) * 4.0,
                        10.0,
                        (Math.random() - 0.5) * 4.0
                    );
                    Arrow arrow = spawnLoc.getWorld().spawn(spawnLoc, Arrow.class);
                    arrow.setShooter(player);
                    arrow.setVelocity(new Vector(0, -1.5, 0));
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ARROW_SHOOT, 0.5f, 1.2f);
                }
            }.runTaskLater(plugin, targetLoc, i * 2L);
        }
        return true;
    }
}

class SkeletonArchers extends Ability {
    private final ItemEditFull plugin;

    public SkeletonArchers(ItemEditFull plugin) {
        super("skeleton_archers", "Skeleton Archers", "Summons 2 helper skeleton archers with helmets for 15 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 15.0);
        int skeletonsCount = getIntParam(plugin, item, "skeletons", 2);
        String helmetMatStr = getStringParam(plugin, item, "helmet_material", "LEATHER_HELMET");
        Material helmetMat = Material.matchMaterial(helmetMatStr);
        if (helmetMat == null) helmetMat = Material.LEATHER_HELMET;

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.8f);

        List<Skeleton> summoned = new ArrayList<>();
        for (int i = 0; i < skeletonsCount; i++) {
            double angle = i * 2 * Math.PI / skeletonsCount;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
            skeleton.getEquipment().setHelmet(new ItemStack(helmetMat));
            skeleton.setMetadata("helper", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            
            for (Entity entity : skeleton.getNearbyEntities(10.0, 5.0, 10.0)) {
                if (entity instanceof LivingEntity && !(entity instanceof Player) && !entity.hasMetadata("helper")) {
                    skeleton.setTarget((LivingEntity) entity);
                    break;
                }
            }
            summoned.add(skeleton);
        }

        new CompatRunnable() {
            @Override
            public void run() {
                for (Skeleton s : summoned) {
                    if (s.isValid()) {
                        s.getWorld().spawnParticle(Particle.SMOKE_NORMAL, s.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.01);
                        s.remove();
                    }
                }
            }
        }.runTaskLater(plugin, player, (long) (duration * 20));

        return true;
    }
}

class BoneTrap extends Ability {
    private final ItemEditFull plugin;

    public BoneTrap(ItemEditFull plugin) {
        super("bone_trap", "Bone Trap", "Locks the targeted entity in a cage of bones for 3 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 3.0);
        double range = getDoubleParam(plugin, item, "range", 8.0);

        Entity target = player.getTargetEntity((int) range);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in range.");
            return false;
        }

        LivingEntity living = (LivingEntity) target;
        Location loc = living.getLocation();
        living.getWorld().playSound(loc, Sound.BLOCK_BONE_BLOCK_PLACE, 1.0f, 0.7f);

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 4) || !living.isValid()) {
                    cancel();
                    return;
                }
                living.teleport(loc);
                living.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.5, 0), 6, 0.3, 0.5, 0.3, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, living, 0L, 5L);

        return true;
    }
}

class WitherSkeletonStrike extends Ability {
    private final ItemEditFull plugin;

    public WitherSkeletonStrike(ItemEditFull plugin) {
        super("wither_skeleton_strike", "Wither Strike", "Your next melee hit deals bonus damage and inflicts Wither for 8 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 0.9f);
        SkeletonAbilities.registerWitherStrike(player.getUniqueId(), System.currentTimeMillis() + (long) (duration * 1000));
        player.sendMessage("§8Wither Strike charged!");
        return true;
    }
}
