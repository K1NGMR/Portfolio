package com.itemedit.full.ability.general;

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
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.full.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GeneralAbilities implements Listener {
    private static final Map<UUID, Long> activeOverloads = new HashMap<>();
    private static final Map<UUID, Double> overloadDamage = new HashMap<>();
    
    private static final Map<UUID, Long> activeLifeSteals = new HashMap<>();
    private static final Map<UUID, Double> lifeStealMultiplier = new HashMap<>();
    
    private static final Map<UUID, Long> activeThornySkins = new HashMap<>();
    private static final Map<UUID, Double> thornyReflectPercent = new HashMap<>();

    private static final Map<UUID, Long> activePocketShields = new HashMap<>();

    private static ItemEditFull pluginInstance;

    public static void register(ItemEditFull plugin) {
        pluginInstance = plugin;
        
        // 14 Base General
        plugin.getAbilityManager().registerAbility(new IcePath(plugin));
        plugin.getAbilityManager().registerAbility(new Frostbite(plugin));
        plugin.getAbilityManager().registerAbility(new Blizzard(plugin));
        plugin.getAbilityManager().registerAbility(new Earthquake(plugin));
        plugin.getAbilityManager().registerAbility(new Entangle(plugin));
        plugin.getAbilityManager().registerAbility(new LeafGust(plugin));
        plugin.getAbilityManager().registerAbility(new Thunderbolt(plugin));
        plugin.getAbilityManager().registerAbility(new Overload(plugin));
        plugin.getAbilityManager().registerAbility(new ChainLightning(plugin));
        plugin.getAbilityManager().registerAbility(new WindBlade(plugin));
        plugin.getAbilityManager().registerAbility(new TornadoLeap(plugin));
        plugin.getAbilityManager().registerAbility(new HolyBlessing(plugin));
        plugin.getAbilityManager().registerAbility(new SmiteAura(plugin));
        plugin.getAbilityManager().registerAbility(new ShadowStep(plugin));

        // 25 Premium General
        plugin.getAbilityManager().registerAbility(new DeathGrip(plugin));
        plugin.getAbilityManager().registerAbility(new WaterSpout(plugin));
        plugin.getAbilityManager().registerAbility(new Whirlpool(plugin));
        plugin.getAbilityManager().registerAbility(new Magnet(plugin));
        plugin.getAbilityManager().registerAbility(new GravityWell(plugin));
        plugin.getAbilityManager().registerAbility(new Combust(plugin));
        plugin.getAbilityManager().registerAbility(new LifeSteal(plugin));
        plugin.getAbilityManager().registerAbility(new Telekinesis(plugin));
        plugin.getAbilityManager().registerAbility(new HasteBoost(plugin));
        plugin.getAbilityManager().registerAbility(new Flight(plugin));
        plugin.getAbilityManager().registerAbility(new Vanish(plugin));
        plugin.getAbilityManager().registerAbility(new ToxicCloud(plugin));
        plugin.getAbilityManager().registerAbility(new ThornySkin(plugin));
        plugin.getAbilityManager().registerAbility(new SpringJump(plugin));
        plugin.getAbilityManager().registerAbility(new FeatherFall(plugin));
        plugin.getAbilityManager().registerAbility(new PocketShield(plugin));
        plugin.getAbilityManager().registerAbility(new IceBarricade(plugin));
        plugin.getAbilityManager().registerAbility(new LavaFountain(plugin));
        plugin.getAbilityManager().registerAbility(new SunStrike(plugin));
        plugin.getAbilityManager().registerAbility(new DarkVortex(plugin));
        plugin.getAbilityManager().registerAbility(new Tsunami(plugin));
        plugin.getAbilityManager().registerAbility(new Supernova(plugin));
        plugin.getAbilityManager().registerAbility(new TimeFreeze(plugin));
        plugin.getAbilityManager().registerAbility(new Rejuvenate(plugin));
        plugin.getAbilityManager().registerAbility(new Adrenaline(plugin));

        plugin.getServer().getPluginManager().registerEvents(new GeneralAbilities(), plugin);
    }

    public static void registerOverload(UUID uuid, long expire, double bonus) {
        activeOverloads.put(uuid, expire);
        overloadDamage.put(uuid, bonus);
    }

    public static void registerLifeSteal(UUID uuid, long expire, double multiplier) {
        activeLifeSteals.put(uuid, expire);
        lifeStealMultiplier.put(uuid, multiplier);
    }

    public static void registerThornySkin(UUID uuid, long expire, double percent) {
        activeThornySkins.put(uuid, expire);
        thornyReflectPercent.put(uuid, percent);
    }

    public static void registerPocketShield(UUID uuid, long expire) {
        activePocketShields.put(uuid, expire);
    }

    @EventHandler
    public void onOverloadHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Long expire = activeOverloads.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                double bonus = overloadDamage.getOrDefault(player.getUniqueId(), 2.0);
                event.setDamage(event.getDamage() + bonus);
                event.getEntity().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, event.getEntity().getLocation().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.05);
                event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);
            }
        }
    }

    @EventHandler
    public void onFrostbiteHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("frostbite")) {
            Location loc = event.getHitBlock() != null ? event.getHitBlock().getLocation() : event.getEntity().getLocation();
            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.2, 0.2, 0.2, 0.02);
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);

            if (event.getHitEntity() instanceof LivingEntity) {
                LivingEntity hit = (LivingEntity) event.getHitEntity();
                hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 2));
                hit.damage(4.0, (Player) event.getEntity().getShooter());
            }
        }
    }

    @EventHandler
    public void onLifeStealHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Long expire = activeLifeSteals.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                double mult = lifeStealMultiplier.getOrDefault(player.getUniqueId(), 0.3);
                double healAmount = event.getFinalDamage() * mult;
                player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + healAmount));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.01);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }
    }

    @EventHandler
    public void onThornySkinHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
            Player player = (Player) event.getEntity();
            LivingEntity attacker = (LivingEntity) event.getDamager();
            Long expire = activeThornySkins.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                double pct = thornyReflectPercent.getOrDefault(player.getUniqueId(), 0.3);
                double damage = event.getDamage() * pct;
                attacker.damage(damage, player);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.8f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onPocketShieldHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof org.bukkit.entity.Projectile) {
            Player player = (Player) event.getEntity();
            Long expire = activePocketShields.get(player.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                event.setCancelled(true);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }
}

// ==================== 14 BASE GENERAL ABILITIES ====================

class IcePath extends Ability {
    private final ItemEditFull plugin;
    public IcePath(ItemEditFull plugin) { super("ice_path", "Ice Path", "Grants Speed, turning water to ice and lava to obsidian."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 8.0);
        int radius = getIntParam(plugin, item, "radius", 3);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration*20), 1));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration*20) || !player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation().subtract(0, 1, 0);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x*x + z*z <= radius*radius) {
                            Block b = loc.clone().add(x, 0, z).getBlock();
                            if (b.getType() == Material.WATER) {
                                b.setType(Material.PACKED_ICE);
                                final Location bLoc = b.getLocation();
                                new CompatRunnable() {
                                    @Override public void run() { if (bLoc.getBlock().getType() == Material.PACKED_ICE) bLoc.getBlock().setType(Material.WATER); }
                                }.runTaskLater(plugin, bLoc, 80L);
                            } else if (b.getType() == Material.LAVA) {
                                b.setType(Material.OBSIDIAN);
                                final Location bLoc = b.getLocation();
                                new CompatRunnable() {
                                    @Override public void run() { if (bLoc.getBlock().getType() == Material.OBSIDIAN) bLoc.getBlock().setType(Material.LAVA); }
                                }.runTaskLater(plugin, bLoc, 80L);
                            }
                        }
                    }
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, player, 0L, 2L);
        return true;
    }
}

class Frostbite extends Ability {
    private final ItemEditFull plugin;
    public Frostbite(ItemEditFull plugin) { super("frostbite", "Frostbite", "Fires a frost shard that damages and slows targets."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        Snowball ball = player.launchProjectile(Snowball.class);
        ball.setMetadata("frostbite", new FixedMetadataValue(plugin, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);
        return true;
    }
}

class Blizzard extends Ability {
    private final ItemEditFull plugin;
    public Blizzard(ItemEditFull plugin) { super("blizzard", "Blizzard", "Spawns a swirling frost cloud, slowing and damaging nearby enemies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 4.0);
        double duration = getDoubleParam(plugin, item, "duration", 6.0);
        double damage = getDoubleParam(plugin, item, "damage", 1.5);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration*20) || !player.isOnline()) { cancel(); return; }
                Location center = player.getLocation();
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    Location pLoc = center.clone().add(Math.cos(angle)*r, Math.random()*2.0, Math.sin(angle)*r);
                    pLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0, 0, 0, 0);
                }
                if (ticks % 20 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 2.0, radius)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            LivingEntity living = (LivingEntity) entity;
                            living.damage(damage, player);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
                        }
                    }
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, player, 0L, 2L);
        return true;
    }
}

class Earthquake extends Ability {
    private final ItemEditFull plugin;
    public Earthquake(ItemEditFull plugin) { super("earthquake", "Earthquake", "Slams ground, dealing damage and knockback."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 5.0);
        double damage = getDoubleParam(plugin, item, "damage", 5.0);
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.8f);
        for (int x = -(int)radius; x <= radius; x++) {
            for (int z = -(int)radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    Location pLoc = loc.clone().add(x, 0, z);
                    pLoc.getWorld().spawnParticle(Particle.CLOUD, pLoc, 1, 0, 0.1, 0, 0.02);
                }
            }
        }
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player) && entity.isOnGround()) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(damage, player);
                living.setVelocity(new Vector(0, 0.6, 0));
            }
        }
        return true;
    }
}

class Entangle extends Ability {
    private final ItemEditFull plugin;
    public Entangle(ItemEditFull plugin) { super("entangle", "Entangle", "Traps targeted entity in leaves."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 3.0);
        Entity target = player.getTargetEntity(8);
        if (!(target instanceof LivingEntity)) { player.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity living = (LivingEntity) target;
        Location tLoc = living.getLocation().getBlock().getLocation();
        living.getWorld().playSound(tLoc, Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.0f, 0.8f);
        Block b1 = tLoc.getBlock();
        Block b2 = tLoc.clone().add(0, 1, 0).getBlock();
        Material orig1 = b1.getType();
        Material orig2 = b2.getType();
        b1.setType(Material.OAK_LEAVES);
        b2.setType(Material.OAK_LEAVES);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration*4) || !living.isValid()) {
                    if (b1.getType() == Material.OAK_LEAVES) b1.setType(orig1);
                    if (b2.getType() == Material.OAK_LEAVES) b2.setType(orig2);
                    cancel(); return;
                }
                living.teleport(tLoc.clone().add(0.5, 0, 0.5));
                living.getWorld().spawnParticle(Particle.CHERRY_LEAVES, tLoc.clone().add(0.5, 1, 0.5), 3, 0.3, 0.5, 0.3, 0.02);
                ticks++;
            }
        }.runTaskTimer(plugin, living, 0L, 5L);
        return true;
    }
}

class LeafGust extends Ability {
    private final ItemEditFull plugin;
    public LeafGust(ItemEditFull plugin) { super("leaf_gust", "Leaf Gust", "Pushes nearby enemies back."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 2.0);
        double range = getDoubleParam(plugin, item, "range", 6.0);
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        player.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.2f, 1.2f);
        for (int i = 0; i < 15; i++) {
            Location pLoc = loc.clone().add(dir.clone().multiply(i * 0.4)).add((Math.random()-0.5)*1.5, (Math.random()-0.5)*1.5, (Math.random()-0.5)*1.5);
            pLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, pLoc, 3, 0.1, 0.1, 0.1, 0.02);
        }
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), range, 3.0, range)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                if (toEntity.normalize().dot(dir) > 0.7) {
                    ((LivingEntity) entity).damage(damage, player);
                    entity.setVelocity(dir.clone().multiply(1.5).setY(0.3));
                }
            }
        }
        return true;
    }
}

class Thunderbolt extends Ability {
    private final ItemEditFull plugin;
    public Thunderbolt(ItemEditFull plugin) { super("thunderbolt", "Thunderbolt", "Strikes target block with lightning."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 5.0);
        Block target = player.getTargetBlockExact(30);
        if (target == null) { player.sendMessage("§cNo target block in sight."); return false; }
        Location targetLoc = target.getLocation();
        targetLoc.getWorld().strikeLightningEffect(targetLoc);
        for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.0, 3.0, 3.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                ((LivingEntity) entity).damage(damage, player);
                entity.setFireTicks(40);
            }
        }
        return true;
    }
}

class Overload extends Ability {
    private final ItemEditFull plugin;
    public Overload(ItemEditFull plugin) { super("overload", "Overload", "Speed III and bonus true damage."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        double bonus = getDoubleParam(plugin, item, "extra_damage", 2.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration*20), 2));
        GeneralAbilities.registerOverload(player.getUniqueId(), System.currentTimeMillis() + (long)(duration*1000), bonus);
        return true;
    }
}

class ChainLightning extends Ability {
    private final ItemEditFull plugin;
    public ChainLightning(ItemEditFull plugin) { super("chain_lightning", "Chain Lightning", "Jumps between up to 3 nearby enemies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 4.0);
        double bounce = getDoubleParam(plugin, item, "bounce_range", 6.0);
        Entity first = player.getTargetEntity(12);
        if (!(first instanceof LivingEntity)) { player.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity current = (LivingEntity) first;
        List<LivingEntity> hit = new ArrayList<>();
        hit.add(player);
        new CompatRunnable() {
            int jumps = 0;
            LivingEntity active = current;
            @Override
            public void run() {
                if (jumps >= 3 || active == null) { cancel(); return; }
                active.damage(damage, player);
                hit.add(active);
                active.getWorld().playSound(active.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
                active.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, active.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.05);
                LivingEntity next = null;
                for (Entity nearby : active.getNearbyEntities(bounce, 4.0, bounce)) {
                    if (nearby instanceof LivingEntity && !hit.contains(nearby)) { next = (LivingEntity) nearby; break; }
                }
                active = next;
                jumps++;
            }
        }.runTaskTimer(plugin, player, 0L, 4L);
        return true;
    }
}

class WindBlade extends Ability {
    private final ItemEditFull plugin;
    public WindBlade(ItemEditFull plugin) { super("wind_blade", "Wind Blade", "Launches wind blade projectile."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 5.0);
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        player.getWorld().playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        new CompatRunnable() {
            int steps = 0;
            Location current = origin.clone();
            @Override
            public void run() {
                if (steps > 30 || !current.getBlock().getType().isAir()) { cancel(); return; }
                current.add(dir.clone().multiply(0.5));
                com.itemedit.full.utils.SchedulerUtils.runTask(plugin, current, () -> {
                    current.getWorld().spawnParticle(Particle.CLOUD, current, 1, 0, 0, 0, 0);
                    for (Entity entity : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).damage(damage, player);
                            entity.setVelocity(dir.clone().multiply(0.8).setY(0.2));
                        }
                    }
                });
                steps++;
            }
        }.runTaskTimer(plugin, player, 0L, 1L);
        return true;
    }
}

class TornadoLeap extends Ability {
    private final ItemEditFull plugin;
    public TornadoLeap(ItemEditFull plugin) { super("tornado_leap", "Tornado Leap", "Leaps high, pushing back enemies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.0f);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 0.5, 0.2, 0.5, 0.1);
        player.setVelocity(new Vector(0, 1.4, 0));
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5.0, 2.0, 5.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                Vector dir = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
                entity.setVelocity(dir.multiply(1.5).setY(0.4));
            }
        }
        return true;
    }
}

class HolyBlessing extends Ability {
    private final ItemEditFull plugin;
    public HolyBlessing(ItemEditFull plugin) { super("holy_blessing", "Holy Blessing", "Heals you and nearby allies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double heal = getDoubleParam(plugin, item, "heal", 6.0);
        double radius = getDoubleParam(plugin, item, "radius", 4.0);
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.TOTEM, loc.add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
        player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + heal));
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
            if (entity instanceof Player && !entity.equals(player)) {
                Player teammate = (Player) entity;
                teammate.setHealth(Math.min(teammate.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), teammate.getHealth() + heal));
                teammate.sendMessage("§aYou were healed by " + player.getName() + "'s Holy Blessing!");
            }
        }
        return true;
    }
}

class SmiteAura extends Ability {
    private final ItemEditFull plugin;
    public SmiteAura(ItemEditFull plugin) { super("smite_aura", "Smite Aura", "Smite undead enemies in area."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 8.0);
        double radius = getDoubleParam(plugin, item, "radius", 5.0);
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.random() * radius;
            Location pLoc = loc.clone().add(Math.cos(angle)*r, 0.5, Math.sin(angle)*r);
            pLoc.getWorld().spawnParticle(Particle.SPELL_INSTANT, pLoc, 5, 0.1, 0.5, 0.1, 0.05);
        }
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 3.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                if (living.getCategory() == org.bukkit.entity.EntityCategory.UNDEAD) {
                    living.damage(damage, player);
                    living.setFireTicks(60);
                } else {
                    living.damage(damage * 0.4, player);
                }
            }
        }
        return true;
    }
}

class ShadowStep extends Ability {
    private final ItemEditFull plugin;
    public ShadowStep(ItemEditFull plugin) { super("shadow_step", "Shadow Step", "Teleports you behind targeted entity."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double range = getDoubleParam(plugin, item, "range", 15.0);
        Entity target = player.getTargetEntity((int) range);
        if (!(target instanceof LivingEntity)) { player.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity living = (LivingEntity) target;
        Location tLoc = living.getLocation();
        Vector dir = tLoc.getDirection().normalize();
        Location teleportLoc = tLoc.clone().subtract(dir.clone().multiply(1.0));
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.teleport(teleportLoc.setDirection(tLoc.getDirection()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0));
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        return true;
    }
}

// ==================== 25 PREMIUM GENERAL ABILITIES (FULL ONLY) ====================

class DeathGrip extends Ability {
    private final ItemEditFull plugin;
    public DeathGrip(ItemEditFull plugin) { super("death_grip", "Death Grip", "Pulls target entity to you."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double range = getDoubleParam(plugin, item, "range", 15.0);
        Entity target = player.getTargetEntity((int) range);
        if (!(target instanceof LivingEntity)) { player.sendMessage("§cNo target entity."); return false; }
        Vector dir = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        target.setVelocity(dir.multiply(1.8).setY(0.5));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.8f, 1.5f);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 15, 0.2, 0.2, 0.2, 0.05);
        return true;
    }
}

class WaterSpout extends Ability {
    private final ItemEditFull plugin;
    public WaterSpout(ItemEditFull plugin) { super("water_spout", "Water Spout", "Fires a jet of water that damages and pushes enemies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 3.0);
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        player.getWorld().playSound(origin, Sound.ENTITY_BOAT_PADDLE_WATER, 1.0f, 1.2f);
        new CompatRunnable() {
            int steps = 0;
            Location current = origin.clone();
            @Override
            public void run() {
                if (steps > 25 || !current.getBlock().getType().isAir()) { cancel(); return; }
                current.add(dir.clone().multiply(0.5));
                com.itemedit.full.utils.SchedulerUtils.runTask(plugin, current, () -> {
                    current.getWorld().spawnParticle(Particle.WATER_SPLASH, current, 5, 0.05, 0.05, 0.05, 0.01);
                    for (Entity entity : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).damage(damage, player);
                            entity.setFireTicks(0);
                            entity.setVelocity(dir.clone().multiply(1.2).setY(0.2));
                        }
                    }
                });
                steps++;
            }
        }.runTaskTimer(plugin, player, 0L, 1L);
        return true;
    }
}

class Whirlpool extends Ability {
    private final ItemEditFull plugin;
    public Whirlpool(ItemEditFull plugin) { super("whirlpool", "Whirlpool", "Sucks nearby enemies to a center block."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 5.0);
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        Block target = player.getTargetBlockExact(20);
        if (target == null) return false;
        Location center = target.getLocation().add(0.5, 1.0, 0.5);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 20) || !player.isOnline()) { cancel(); return; }
                center.getWorld().spawnParticle(Particle.WATER_SPLASH, center, 10, radius*0.5, 0.1, radius*0.5, 0.05);
                center.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.2f);
                for (Entity vic : center.getWorld().getNearbyEntities(center, radius, 3.0, radius)) {
                    if (vic instanceof LivingEntity && !vic.equals(player)) {
                        Vector pull = center.toVector().subtract(vic.getLocation().toVector());
                        if (pull.length() > 0.5) vic.setVelocity(pull.normalize().multiply(0.35).setY(0.05));
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(plugin, center, 0L, 5L);
        return true;
    }
}

class Magnet extends Ability {
    private final ItemEditFull plugin;
    public Magnet(ItemEditFull plugin) { super("magnet", "Magnet", "Pulls dropped items to you."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 8.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Item) {
                Item dropped = (Item) entity;
                Vector dir = player.getLocation().toVector().subtract(dropped.getLocation().toVector());
                dropped.setVelocity(dir.normalize().multiply(1.0).setY(0.2));
            }
        }
        return true;
    }
}

class GravityWell extends Ability {
    private final ItemEditFull plugin;
    public GravityWell(ItemEditFull plugin) { super("gravity_well", "Gravity Well", "Slows enemies, pinning them down."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 5.0);
        double duration = getDoubleParam(plugin, item, "duration", 6.0);
        Location center = player.getLocation();
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 20) || !player.isOnline()) { cancel(); return; }
                center.getWorld().spawnParticle(Particle.PORTAL, center, 15, radius*0.6, 0.1, radius*0.6, 0);
                for (Entity vic : center.getWorld().getNearbyEntities(center, radius, 4.0, radius)) {
                    if (vic instanceof LivingEntity && !vic.equals(player)) {
                        LivingEntity living = (LivingEntity) vic;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
                        living.setVelocity(new Vector(0, -0.4, 0));
                    }
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, center, 0L, 10L);
        return true;
    }
}

class Combust extends Ability {
    private final ItemEditFull plugin;
    public Combust(ItemEditFull plugin) { super("combust", "Combust", "Sets self on fire, granting Strength II."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        player.setFireTicks((int) (duration * 20));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int)(duration*20), 1));
        return true;
    }
}

class LifeSteal extends Ability {
    private final ItemEditFull plugin;
    public LifeSteal(ItemEditFull plugin) { super("life_steal", "Life Steal", "Restore health from damage dealt."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 8.0);
        double mult = getDoubleParam(plugin, item, "steal_multiplier", 0.3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.8f);
        GeneralAbilities.registerLifeSteal(player.getUniqueId(), System.currentTimeMillis() + (long)(duration*1000), mult);
        return true;
    }
}

class Telekinesis extends Ability {
    private final ItemEditFull plugin;
    public Telekinesis(ItemEditFull plugin) { super("telekinesis", "Telekinesis", "Breaks block in sight, dropping items directly into inventory."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        Block target = player.getTargetBlockExact(8);
        if (target == null || target.getType() == Material.AIR || target.getType() == Material.BEDROCK) return false;
        Location loc = target.getLocation();
        Collection<ItemStack> drops = target.getDrops(player.getInventory().getItemInMainHand(), player);
        target.setType(Material.AIR);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.2, 0.2, 0.2, 0.05);
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            for (ItemStack left : overflow.values()) {
                loc.getWorld().dropItemNaturally(loc, left);
            }
        }
        return true;
    }
}

class HasteBoost extends Ability {
    private final ItemEditFull plugin;
    public HasteBoost(ItemEditFull plugin) { super("haste_boost", "Haste Boost", "Haste III support."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 15.0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, (int)(duration*20), 2));
        return true;
    }
}

class Flight extends Ability {
    private final ItemEditFull plugin;
    public Flight(ItemEditFull plugin) { super("flight", "Flight", "Allows flying for 10 seconds."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
        player.setAllowFlight(true);
        player.setFlying(true);
        new CompatRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    player.sendMessage("§cFlight has expired!");
                }
            }
        }.runTaskLater(plugin, player, (long)(duration*20));
        return true;
    }
}

class Vanish extends Ability {
    private final ItemEditFull plugin;
    public Vanish(ItemEditFull plugin) { super("vanish", "Vanish", "Invisibility and silent walk."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 15.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int)(duration*20), 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration*20), 0));
        return true;
    }
}

class ToxicCloud extends Ability {
    private final ItemEditFull plugin;
    public ToxicCloud(ItemEditFull plugin) { super("toxic_cloud", "Toxic Cloud", "Creates poison cloud."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 3.0);
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        Block target = player.getTargetBlockExact(15);
        Location spawnLoc = (target != null) ? target.getLocation().add(0.5, 1.0, 0.5) : player.getLocation();
        player.getWorld().playSound(spawnLoc, Sound.BLOCK_MUD_PLACE, 1.0f, 0.7f);
        org.bukkit.entity.AreaEffectCloud cloud = (org.bukkit.entity.AreaEffectCloud) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(Particle.VILLAGER_HAPPY);
        cloud.setRadius((float) radius);
        cloud.setDuration((int)(duration*20));
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
        return true;
    }
}

class ThornySkin extends Ability {
    private final ItemEditFull plugin;
    public ThornySkin(ItemEditFull plugin) { super("thorny_skin", "Thorny Skin", "Reflects incoming melee damage."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        double percent = getDoubleParam(plugin, item, "reflect_percent", 0.3);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.0f, 0.8f);
        GeneralAbilities.registerThornySkin(player.getUniqueId(), System.currentTimeMillis() + (long)(duration*1000), percent);
        return true;
    }
}

class SpringJump extends Ability {
    private final ItemEditFull plugin;
    public SpringJump(ItemEditFull plugin) { super("spring_jump", "Spring Jump", "Super jump."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.2f, 1.5f);
        player.setVelocity(new Vector(0, 1.6, 0));
        return true;
    }
}

class FeatherFall extends Ability {
    private final ItemEditFull plugin;
    public FeatherFall(ItemEditFull plugin) { super("feather_fall", "Feather Fall", "Slow falling support."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 20.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.2f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int)(duration*20), 0));
        return true;
    }
}

class PocketShield extends Ability {
    private final ItemEditFull plugin;
    public PocketShield(ItemEditFull plugin) { super("pocket_shield", "Pocket Shield", "Blocks projectiles."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        GeneralAbilities.registerPocketShield(player.getUniqueId(), System.currentTimeMillis() + (long)(duration*1000));
        return true;
    }
}

class IceBarricade extends Ability {
    private final ItemEditFull plugin;
    public IceBarricade(ItemEditFull plugin) { super("ice_barricade", "Ice Barricade", "Ice wall in front of you."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Location wallCenter = loc.clone().add(dir.multiply(2.0));
        List<Block> blocks = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int y = 0; y <= 2; y++) {
                Block b = wallCenter.clone().add(right.clone().multiply(i)).add(0, y, 0).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.ICE);
                    blocks.add(b);
                }
            }
        }
        loc.getWorld().playSound(wallCenter, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.8f);
        new CompatRunnable() {
            @Override
            public void run() {
                for (Block b : blocks) {
                    if (b.getType() == Material.ICE) b.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, wallCenter, 100L); // 5 seconds
        return true;
    }
}

class LavaFountain extends Ability {
    private final ItemEditFull plugin;
    public LavaFountain(ItemEditFull plugin) { super("lava_fountain", "Lava Fountain", "Shoots magma blocks upward."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(3.0));
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 1.5f);
        for (int i = 0; i < 5; i++) {
            new CompatRunnable() {
                @Override
                public void run() {
                    org.bukkit.entity.FallingBlock block = loc.getWorld().spawnFallingBlock(loc, Material.MAGMA_BLOCK.createBlockData());
                    block.setVelocity(new Vector((Math.random()-0.5)*0.4, 1.2, (Math.random()-0.5)*0.4));
                }
            }.runTaskLater(plugin, loc, i * 4L);
        }
        return true;
    }
}

class SunStrike extends Ability {
    private final ItemEditFull plugin;
    public SunStrike(ItemEditFull plugin) { super("sun_strike", "Sun Strike", "Strikes light beam after 1.5s."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 8.0);
        Block target = player.getTargetBlockExact(25);
        if (target == null) return false;
        Location loc = target.getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 10, 0.1, 2.0, 0.1, 0);
        new CompatRunnable() {
            @Override
            public void run() {
                loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.2f);
                loc.getWorld().spawnParticle(Particle.TOTEM, loc, 25, 0.3, 2.0, 0.3, 0.1);
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 3.0, 2.5)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(damage, player);
                        entity.setFireTicks(80);
                    }
                }
            }
        }.runTaskLater(plugin, loc, 30L);
        return true;
    }
}

class DarkVortex extends Ability {
    private final ItemEditFull plugin;
    public DarkVortex(ItemEditFull plugin) { super("dark_vortex", "Dark Vortex", "Blinds and draws in enemies."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 5.0);
        double duration = getDoubleParam(plugin, item, "duration", 5.0);
        Block target = player.getTargetBlockExact(20);
        if (target == null) return false;
        Location center = target.getLocation().add(0.5, 1.0, 0.5);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 20) || !player.isOnline()) { cancel(); return; }
                center.getWorld().spawnParticle(Particle.PORTAL, center, 20, radius*0.5, 0.5, radius*0.5, 0);
                center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.5f);
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3.0, radius)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity living = (LivingEntity) entity;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                        Vector pull = center.toVector().subtract(living.getLocation().toVector());
                        if (pull.length() > 0.5) living.setVelocity(pull.normalize().multiply(0.3).setY(0.05));
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(plugin, center, 0L, 5L);
        return true;
    }
}

class Tsunami extends Ability {
    private final ItemEditFull plugin;
    public Tsunami(ItemEditFull plugin) { super("tsunami", "Tsunami", "Launches water wave."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double damage = getDoubleParam(plugin, item, "damage", 4.0);
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        player.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.5f, 0.8f);
        new CompatRunnable() {
            int step = 0;
            Location current = loc.clone();
            @Override
            public void run() {
                if (step > 15 || !current.getBlock().getType().isAir()) { cancel(); return; }
                current.add(dir);
                com.itemedit.full.utils.SchedulerUtils.runTask(plugin, current, () -> {
                    current.getWorld().spawnParticle(Particle.WATER_SPLASH, current, 15, 1.0, 0.5, 1.0, 0.05);
                    for (Entity entity : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).damage(damage, player);
                            entity.setVelocity(dir.clone().multiply(1.2).setY(0.3));
                        }
                    }
                });
                step++;
            }
        }.runTaskTimer(plugin, player, 0L, 2L);
        return true;
    }
}

class Supernova extends Ability {
    private final ItemEditFull plugin;
    public Supernova(ItemEditFull plugin) { super("supernova", "Supernova", "Explosion centered on you (zero self damage)."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 6.0);
        double damage = getDoubleParam(plugin, item, "damage", 10.0);
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 12, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 3.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                ((LivingEntity) entity).damage(damage, player);
                entity.setVelocity(entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.8).setY(0.5));
            }
        }
        return true;
    }
}

class TimeFreeze extends Ability {
    private final ItemEditFull plugin;
    public TimeFreeze(ItemEditFull plugin) { super("time_freeze", "Time Freeze", "Freezes nearby entities."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = getDoubleParam(plugin, item, "radius", 6.0);
        double duration = getDoubleParam(plugin, item, "duration", 3.0);
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 30, radius*0.5, 1.0, radius*0.5, 0.05);
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 3.0, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int)(duration*20), 10));
                living.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int)(duration*20), 0));
                living.setVelocity(new Vector(0, 0, 0));
            }
        }
        return true;
    }
}

class Rejuvenate extends Ability {
    private final ItemEditFull plugin;
    public Rejuvenate(ItemEditFull plugin) { super("rejuvenate", "Rejuvenate", "Heals 1 heart every second."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 10.0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.8f);
        new CompatRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= duration || !player.isOnline()) { cancel(); return; }
                player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 2.0));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2, 0.01);
                count++;
            }
        }.runTaskTimer(plugin, player, 0L, 20L);
        return true;
    }
}

class Adrenaline extends Ability {
    private final ItemEditFull plugin;
    public Adrenaline(ItemEditFull plugin) { super("adrenaline", "Adrenaline", "Speed III and Strength I if below 4 hearts."); this.plugin = plugin; }
    @Override
    public boolean trigger(Player player, ItemStack item) {
        if (player.getHealth() > 8.0) { player.sendMessage("§cYou must be below 4 hearts to use this!"); return false; }
        double duration = getDoubleParam(plugin, item, "duration", 8.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration*20), 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int)(duration*20), 0));
        return true;
    }
}
