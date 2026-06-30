package com.itemedit.light.ability.general;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
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
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.light.utils.CompatRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GeneralAbilities implements Listener {
    private static final Map<UUID, Long> activeOverloads = new HashMap<>();
    private static final Map<UUID, Double> overloadDamage = new HashMap<>();
    private static ItemEditLight pluginInstance;

    public static void register(ItemEditLight plugin) {
        pluginInstance = plugin;
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

        plugin.getServer().getPluginManager().registerEvents(new GeneralAbilities(), plugin);
    }

    public static void registerOverload(UUID uuid, long expire, double bonus) {
        activeOverloads.put(uuid, expire);
        overloadDamage.put(uuid, bonus);
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
}

class IcePath extends Ability {
    private final ItemEditLight plugin;

    public IcePath(ItemEditLight plugin) {
        super("ice_path", "Ice Path", "Grants Speed, turning water to ice and lava to obsidian under your feet.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = plugin.getConfig().getDouble("abilities.ice_path.duration", 8.0);
        int radius = plugin.getConfig().getInt("abilities.ice_path.radius", 3);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), 1));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 20) || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().subtract(0, 1, 0);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x*x + z*z <= radius*radius) {
                            Block b = loc.clone().add(x, 0, z).getBlock();
                            if (b.getType() == Material.WATER) {
                                b.setType(Material.PACKED_ICE);
                                final Location bLoc = b.getLocation();
                                new CompatRunnable() {
                                    @Override
                                    public void run() {
                                        if (bLoc.getBlock().getType() == Material.PACKED_ICE) {
                                            bLoc.getBlock().setType(Material.WATER);
                                        }
                                    }
                                }.runTaskLater(plugin, bLoc, 80L);
                            } else if (b.getType() == Material.LAVA) {
                                b.setType(Material.OBSIDIAN);
                                final Location bLoc = b.getLocation();
                                new CompatRunnable() {
                                    @Override
                                    public void run() {
                                        if (bLoc.getBlock().getType() == Material.OBSIDIAN) {
                                            bLoc.getBlock().setType(Material.LAVA);
                                        }
                                    }
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
    private final ItemEditLight plugin;

    public Frostbite(ItemEditLight plugin) {
        super("frostbite", "Frostbite", "Fires a frost shard that damages and slows targets.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Snowball ball = player.launchProjectile(Snowball.class);
        ball.setMetadata("frostbite", new FixedMetadataValue(plugin, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);
        return true;
    }
}

class Blizzard extends Ability {
    private final ItemEditLight plugin;

    public Blizzard(ItemEditLight plugin) {
        super("blizzard", "Blizzard", "Spawns a swirling frost cloud, slowing and damaging nearby enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = plugin.getConfig().getDouble("abilities.blizzard.radius", 4.0);
        double duration = plugin.getConfig().getDouble("abilities.blizzard.duration", 6.0);
        double damage = plugin.getConfig().getDouble("abilities.blizzard.damage", 1.5);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.5f);

        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (duration * 20) || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation();
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    Location pLoc = center.clone().add(Math.cos(angle) * r, Math.random() * 2.0, Math.sin(angle) * r);
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
    private final ItemEditLight plugin;

    public Earthquake(ItemEditLight plugin) {
        super("earthquake", "Earthquake", "Slams the ground, dealing damage and knockback to all nearby grounded entities.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double radius = plugin.getConfig().getDouble("abilities.earthquake.radius", 5.0);
        double damage = plugin.getConfig().getDouble("abilities.earthquake.damage", 5.0);

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
    private final ItemEditLight plugin;

    public Entangle(ItemEditLight plugin) {
        super("entangle", "Entangle", "Traps targeted entity in leaves for 3 seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(8);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

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
                if (ticks >= 12 || !living.isValid()) {
                    if (b1.getType() == Material.OAK_LEAVES) b1.setType(orig1);
                    if (b2.getType() == Material.OAK_LEAVES) b2.setType(orig2);
                    cancel();
                    return;
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
    private final ItemEditLight plugin;

    public LeafGust(ItemEditLight plugin) {
        super("leaf_gust", "Leaf Gust", "Pushes nearby enemies back with a cone blast of leaves.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        player.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.2f, 1.2f);

        for (int i = 0; i < 15; i++) {
            Location pLoc = loc.clone().add(dir.clone().multiply(i * 0.4)).add(
                (Math.random() - 0.5) * 1.5,
                (Math.random() - 0.5) * 1.5,
                (Math.random() - 0.5) * 1.5
            );
            pLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, pLoc, 3, 0.1, 0.1, 0.1, 0.02);
        }

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 6.0, 3.0, 6.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                if (toEntity.normalize().dot(dir) > 0.7) {
                    ((LivingEntity) entity).damage(2.0, player);
                    entity.setVelocity(dir.clone().multiply(1.5).setY(0.3));
                }
            }
        }
        return true;
    }
}

class Thunderbolt extends Ability {
    private final ItemEditLight plugin;

    public Thunderbolt(ItemEditLight plugin) {
        super("thunderbolt", "Thunderbolt", "Strikes target block with lightning, damaging nearby enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Block target = player.getTargetBlockExact(30);
        if (target == null) {
            player.sendMessage("§cNo target block in sight.");
            return false;
        }

        Location targetLoc = target.getLocation();
        targetLoc.getWorld().strikeLightningEffect(targetLoc);
        
        for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.0, 3.0, 3.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                ((LivingEntity) entity).damage(5.0, player);
                entity.setFireTicks(40);
            }
        }
        return true;
    }
}

class Overload extends Ability {
    private final ItemEditLight plugin;

    public Overload(ItemEditLight plugin) {
        super("overload", "Overload", "Charges you with electricity, granting Speed III and bonus true damage.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = plugin.getConfig().getDouble("abilities.overload.duration", 10.0);
        double bonus = plugin.getConfig().getDouble("abilities.overload.extra_damage", 2.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration * 20), 2));
        GeneralAbilities.registerOverload(player.getUniqueId(), System.currentTimeMillis() + (long)(duration * 1000), bonus);

        return true;
    }
}

class ChainLightning extends Ability {
    private final ItemEditLight plugin;

    public ChainLightning(ItemEditLight plugin) {
        super("chain_lightning", "Chain Lightning", "Fires lightning that jumps between up to 3 nearby enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity first = player.getTargetEntity(12);
        if (!(first instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

        LivingEntity current = (LivingEntity) first;
        List<LivingEntity> hit = new ArrayList<>();
        hit.add(player); // prevent hitting caster

        new CompatRunnable() {
            int jumps = 0;
            LivingEntity active = current;

            @Override
            public void run() {
                if (jumps >= 3 || active == null) {
                    cancel();
                    return;
                }

                active.damage(4.0, player);
                hit.add(active);
                active.getWorld().playSound(active.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
                active.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, active.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.05);

                LivingEntity next = null;
                for (Entity nearby : active.getNearbyEntities(6.0, 4.0, 6.0)) {
                    if (nearby instanceof LivingEntity && !hit.contains(nearby)) {
                        next = (LivingEntity) nearby;
                        break;
                    }
                }
                active = next;
                jumps++;
            }
        }.runTaskTimer(plugin, player, 0L, 4L);

        return true;
    }
}

class WindBlade extends Ability {
    private final ItemEditLight plugin;

    public WindBlade(ItemEditLight plugin) {
        super("wind_blade", "Wind Blade", "Launches a wind blade that pierces enemies in its path.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        player.getWorld().playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

        new CompatRunnable() {
            int steps = 0;
            Location current = origin.clone();

            @Override
            public void run() {
                if (steps > 30 || !current.getBlock().getType().isAir()) {
                    cancel();
                    return;
                }

                current.add(dir.clone().multiply(0.5));
                com.itemedit.light.utils.SchedulerUtils.runTask(plugin, current, () -> {
                    current.getWorld().spawnParticle(Particle.CLOUD, current, 1, 0, 0, 0, 0);

                    for (Entity entity : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).damage(5.0, player);
                            entity.setVelocity(dir.clone().multiply(0.8).setY(0.2));
                        }
                    }
                });
                steps++;
            }
        }.runTaskTimer(plugin, player, 0L, 1L); // fallback safe handle

        return true;
    }
}

class TornadoLeap extends Ability {
    private final ItemEditLight plugin;

    public TornadoLeap(ItemEditLight plugin) {
        super("tornado_leap", "Tornado Leap", "Launches you into the air and blows back nearby enemies.");
        this.plugin = plugin;
    }

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
    private final ItemEditLight plugin;

    public HolyBlessing(ItemEditLight plugin) {
        super("holy_blessing", "Holy Blessing", "Heals you and all nearby teammates within 4 blocks.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.TOTEM, loc.add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);

        player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 6.0));

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
            if (entity instanceof Player && !entity.equals(player)) {
                Player teammate = (Player) entity;
                teammate.setHealth(Math.min(teammate.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), teammate.getHealth() + 6.0));
                teammate.sendMessage("§aYou were healed by " + player.getName() + "'s Holy Blessing!");
            }
        }
        return true;
    }
}

class SmiteAura extends Ability {
    private final ItemEditLight plugin;

    public SmiteAura(ItemEditLight plugin) {
        super("smite_aura", "Smite Aura", "Strikes down holy light, dealing high damage to Undead enemies in a 5-block radius.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);

        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.random() * 5.0;
            Location pLoc = loc.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
            pLoc.getWorld().spawnParticle(Particle.SPELL_INSTANT, pLoc, 5, 0.1, 0.5, 0.1, 0.05);
        }

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5.0, 3.0, 5.0)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                if (living.getCategory() == org.bukkit.entity.EntityCategory.UNDEAD) {
                    living.damage(8.0, player);
                    living.setFireTicks(60);
                } else {
                    living.damage(3.0, player);
                }
            }
        }
        return true;
    }
}

class ShadowStep extends Ability {
    private final ItemEditLight plugin;

    public ShadowStep(ItemEditLight plugin) {
        super("shadow_step", "Shadow Step", "Teleports you behind targeted entity, granting Invisibility.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Entity target = player.getTargetEntity(15);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage("§cNo target entity in sight.");
            return false;
        }

        LivingEntity living = (LivingEntity) target;
        Location tLoc = living.getLocation();
        Vector dir = tLoc.getDirection().normalize();
        
        // Find position 1 block behind the entity
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
