package com.itemedit.full.ability.general;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.itemedit.full.utils.CompatRunnable;
import com.itemedit.full.utils.SchedulerUtils;
import org.bukkit.util.Vector;

import java.util.*;

public class NewExpansionAbilities implements Listener {
    private static ItemEditFull pluginInstance;
    private static final Map<UUID, Long> piglinRageActive = new HashMap<>();
    private static final Map<UUID, Long> activeBreezeDeflects = new HashMap<>();
    private static final List<Location> magmaTrailLocations = new ArrayList<>();

    public static void playSoundSafe(Location loc, String soundName, Sound fallbackSound, float volume, float pitch) {
        try {
            loc.getWorld().playSound(loc, Sound.valueOf(soundName), volume, pitch);
        } catch (Exception e) {
            loc.getWorld().playSound(loc, fallbackSound, volume, pitch);
        }
    }

    public static void spawnDust(Location loc, Color color, float size, int count, double ox, double oy, double oz) {
        try {
            Particle particle;
            try {
                particle = Particle.valueOf("DUST");
            } catch (Exception e) {
                particle = Particle.REDSTONE;
            }
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);
            loc.getWorld().spawnParticle(particle, loc, count, ox, oy, oz, 0.0, dustOptions);
        } catch (Exception e) {
            try {
                loc.getWorld().spawnParticle(Particle.CRIT, loc, count, ox, oy, oz, 0.1);
            } catch (Exception ignored) {}
        }
    }

    public static void drawHelix(Location center, Color color, double radius, double height, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / 15) * i;
            double y = (height / count) * i;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location loc = center.clone().add(x, y, z);
            spawnDust(loc, color, 1.2f, 1, 0, 0, 0);
        }
    }

    public static void drawSphere(Location center, Color color, double radius, int density) {
        for (double u = 0; u <= Math.PI; u += Math.PI / density) {
            for (double v = 0; v < 2 * Math.PI; v += 2 * Math.PI / density) {
                double x = radius * Math.sin(u) * Math.cos(v);
                double y = radius * Math.cos(u);
                double z = radius * Math.sin(u) * Math.sin(v);
                Location loc = center.clone().add(x, y, z);
                spawnDust(loc, color, 1.0f, 1, 0, 0, 0);
            }
        }
    }

    public static void drawRing(Location center, Color color, double radius, int density) {
        for (int i = 0; i < density; i++) {
            double angle = (2 * Math.PI / density) * i;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location loc = center.clone().add(x, 0.1, z);
            spawnDust(loc, color, 1.2f, 1, 0, 0, 0);
        }
    }

    public static void register(ItemEditFull plugin) {
        pluginInstance = plugin;
        
        // Nether Expansion (1-20)
        plugin.getAbilityManager().registerAbility(new MagmaShield(plugin));
        plugin.getAbilityManager().registerAbility(new MagmaJump(plugin));
        plugin.getAbilityManager().registerAbility(new MagmaTrail(plugin));
        plugin.getAbilityManager().registerAbility(new MagmaFist(plugin));
        plugin.getAbilityManager().registerAbility(new BlazeSpeed(plugin));
        plugin.getAbilityManager().registerAbility(new BlazeAura(plugin));
        plugin.getAbilityManager().registerAbility(new BlazeFlight(plugin));
        plugin.getAbilityManager().registerAbility(new BlazeBarrage(plugin));
        plugin.getAbilityManager().registerAbility(new GhastFloat(plugin));
        plugin.getAbilityManager().registerAbility(new GhastFireball(plugin));
        plugin.getAbilityManager().registerAbility(new GhastScream(plugin));
        plugin.getAbilityManager().registerAbility(new GhastTear(plugin));
        plugin.getAbilityManager().registerAbility(new PiglinGreed(plugin));
        plugin.getAbilityManager().registerAbility(new PiglinCrossbow(plugin));
        plugin.getAbilityManager().registerAbility(new PiglinRage(plugin));
        plugin.getAbilityManager().registerAbility(new PiglinBarter(plugin));
        plugin.getAbilityManager().registerAbility(new NetherQuake(plugin));
        plugin.getAbilityManager().registerAbility(new NetherHeat(plugin));
        plugin.getAbilityManager().registerAbility(new NetherPortalRift(plugin));
        plugin.getAbilityManager().registerAbility(new NetherSoulDrain(plugin));

        // Ocean / Aquatic (21-40)
        plugin.getAbilityManager().registerAbility(new GuardianBeam(plugin));
        plugin.getAbilityManager().registerAbility(new GuardianThorns(plugin));
        plugin.getAbilityManager().registerAbility(new ElderGuardianFatigue(plugin));
        plugin.getAbilityManager().registerAbility(new ElderGuardianGhost(plugin));
        plugin.getAbilityManager().registerAbility(new DrownedTridentAbility(plugin));
        plugin.getAbilityManager().registerAbility(new DrownedWaterLeap(plugin));
        plugin.getAbilityManager().registerAbility(new DrownedConduitPower(plugin));
        plugin.getAbilityManager().registerAbility(new DrownedDepthStrider(plugin));
        plugin.getAbilityManager().registerAbility(new DolphinGraceAbility(plugin));
        plugin.getAbilityManager().registerAbility(new DolphinLeap(plugin));
        plugin.getAbilityManager().registerAbility(new DolphinSonic(plugin));
        plugin.getAbilityManager().registerAbility(new TurtleShell(plugin));
        plugin.getAbilityManager().registerAbility(new TurtleFortitude(plugin));
        plugin.getAbilityManager().registerAbility(new SquidInkAbility(plugin));
        plugin.getAbilityManager().registerAbility(new SquidBlindness(plugin));
        plugin.getAbilityManager().registerAbility(new SquidPropulsion(plugin));
        plugin.getAbilityManager().registerAbility(new FishAgility(plugin));
        plugin.getAbilityManager().registerAbility(new FishWaterBreathing(plugin));
        plugin.getAbilityManager().registerAbility(new OceanTempest(plugin));
        plugin.getAbilityManager().registerAbility(new OceanTsunami(plugin));

        // Sky / Aero (41-60)
        plugin.getAbilityManager().registerAbility(new PhantomGlide(plugin));
        plugin.getAbilityManager().registerAbility(new PhantomSwoop(plugin));
        plugin.getAbilityManager().registerAbility(new PhantomInsomnia(plugin));
        plugin.getAbilityManager().registerAbility(new PhantomBite(plugin));
        plugin.getAbilityManager().registerAbility(new BreezeWindCharge(plugin));
        plugin.getAbilityManager().registerAbility(new BreezeDeflect(plugin));
        plugin.getAbilityManager().registerAbility(new BreezeLeap(plugin));
        plugin.getAbilityManager().registerAbility(new BreezeGust(plugin));
        plugin.getAbilityManager().registerAbility(new BatSonar(plugin));
        plugin.getAbilityManager().registerAbility(new BatScreech(plugin));
        plugin.getAbilityManager().registerAbility(new BatFlight(plugin));
        plugin.getAbilityManager().registerAbility(new ChickenEggVolley(plugin));
        plugin.getAbilityManager().registerAbility(new ChickenFeatherShield(plugin));
        plugin.getAbilityManager().registerAbility(new ChickenSlowFall(plugin));
        plugin.getAbilityManager().registerAbility(new BeeSting(plugin));
        plugin.getAbilityManager().registerAbility(new BeeHoneyTrap(plugin));
        plugin.getAbilityManager().registerAbility(new BeeSwarmSummon(plugin));
        plugin.getAbilityManager().registerAbility(new BeePollinate(plugin));
        plugin.getAbilityManager().registerAbility(new SkyLightningStrike(plugin));
        plugin.getAbilityManager().registerAbility(new SkyZephyr(plugin));

        // Ancient / Sculk (61-70)
        plugin.getAbilityManager().registerAbility(new SculkShriekerSound(plugin));
        plugin.getAbilityManager().registerAbility(new SculkSensorPing(plugin));
        plugin.getAbilityManager().registerAbility(new SculkBlindnessAura(plugin));
        plugin.getAbilityManager().registerAbility(new SculkInfectionStrike(plugin));
        plugin.getAbilityManager().registerAbility(new SculkVeinSpread(plugin));
        plugin.getAbilityManager().registerAbility(new SculkCatalystHeal(plugin));
        plugin.getAbilityManager().registerAbility(new WardenSonicStrike(plugin));
        plugin.getAbilityManager().registerAbility(new WardenDarknessBurst(plugin));
        plugin.getAbilityManager().registerAbility(new WardenSculkStep(plugin));
        plugin.getAbilityManager().registerAbility(new WardenVibrationSense(plugin));

        // Ice / Tundra (71-80)
        plugin.getAbilityManager().registerAbility(new StraySlownessArrow(plugin));
        plugin.getAbilityManager().registerAbility(new StrayFreezeTouch(plugin));
        plugin.getAbilityManager().registerAbility(new StraySnowStorm(plugin));
        plugin.getAbilityManager().registerAbility(new SnowGolemSnowball(plugin));
        plugin.getAbilityManager().registerAbility(new SnowGolemTrail(plugin));
        plugin.getAbilityManager().registerAbility(new SnowGolemFreeze(plugin));
        plugin.getAbilityManager().registerAbility(new IceSpikeSummon(plugin));
        plugin.getAbilityManager().registerAbility(new IceShield(plugin));
        plugin.getAbilityManager().registerAbility(new IceSkate(plugin));
        plugin.getAbilityManager().registerAbility(new IceFrostbite(plugin));

        // Desert / Mesa (81-90)
        plugin.getAbilityManager().registerAbility(new HuskHungerStrike(plugin));
        plugin.getAbilityManager().registerAbility(new HuskSandStorm(plugin));
        plugin.getAbilityManager().registerAbility(new HuskDesertHeat(plugin));
        plugin.getAbilityManager().registerAbility(new ShulkerLevitationBulletAbility(plugin));
        plugin.getAbilityManager().registerAbility(new ShulkerShellClose(plugin));
        plugin.getAbilityManager().registerAbility(new ShulkerTeleport(plugin));
        plugin.getAbilityManager().registerAbility(new DesertMirage(plugin));
        plugin.getAbilityManager().registerAbility(new DesertQuicksand(plugin));
        plugin.getAbilityManager().registerAbility(new DesertCactusThorns(plugin));
        plugin.getAbilityManager().registerAbility(new DesertSunstroke(plugin));

        // Magical / Illager (91-100)
        plugin.getAbilityManager().registerAbility(new EvokerFangsAbility(plugin));
        plugin.getAbilityManager().registerAbility(new EvokerVexesAbility(plugin));
        plugin.getAbilityManager().registerAbility(new VindicatorAxeRush(plugin));
        plugin.getAbilityManager().registerAbility(new VindicatorRage(plugin));
        plugin.getAbilityManager().registerAbility(new IllusionerClone(plugin));
        plugin.getAbilityManager().registerAbility(new IllusionerBlindness(plugin));
        plugin.getAbilityManager().registerAbility(new WitchPotionThrowAbility(plugin));
        plugin.getAbilityManager().registerAbility(new WitchPotionDrink(plugin));
        plugin.getAbilityManager().registerAbility(new WitchBrewCauldron(plugin));
        plugin.getAbilityManager().registerAbility(new WitchPoisonCloud(plugin));

        // Custom Legendary Combat Strikes (101-129)
        plugin.getAbilityManager().registerAbility(new FlameStrike(plugin));
        plugin.getAbilityManager().registerAbility(new TsunamiSlash(plugin));
        plugin.getAbilityManager().registerAbility(new WindGustSlash(plugin));
        plugin.getAbilityManager().registerAbility(new VoidSlayer(plugin));
        plugin.getAbilityManager().registerAbility(new EarthSmash(plugin));
        plugin.getAbilityManager().registerAbility(new ThunderClap(plugin));
        plugin.getAbilityManager().registerAbility(new HolyPurify(plugin));
        plugin.getAbilityManager().registerAbility(new ShadowStrike(plugin));
        plugin.getAbilityManager().registerAbility(new MeteorSlam(plugin));
        plugin.getAbilityManager().registerAbility(new LunarCrescent(plugin));
        plugin.getAbilityManager().registerAbility(new SolarFlare(plugin));
        plugin.getAbilityManager().registerAbility(new CosmicRift(plugin));
        plugin.getAbilityManager().registerAbility(new PlagueStrike(plugin));
        plugin.getAbilityManager().registerAbility(new FrostGiantSlam(plugin));
        plugin.getAbilityManager().registerAbility(new VampiricEdge(plugin));
        plugin.getAbilityManager().registerAbility(new ReaperScythe(plugin));
        plugin.getAbilityManager().registerAbility(new ExecutionerChop(plugin));
        plugin.getAbilityManager().registerAbility(new GravityPullStrike(plugin));
        plugin.getAbilityManager().registerAbility(new MagneticDraw(plugin));
        plugin.getAbilityManager().registerAbility(new ToxicSlash(plugin));
        plugin.getAbilityManager().registerAbility(new WitherCleave(plugin));
        plugin.getAbilityManager().registerAbility(new PhoenixStrike(plugin));
        plugin.getAbilityManager().registerAbility(new GlacierCrash(plugin));
        plugin.getAbilityManager().registerAbility(new TempestStrike(plugin));
        plugin.getAbilityManager().registerAbility(new SculkShatter(plugin));
        plugin.getAbilityManager().registerAbility(new VolcanicRupture(plugin));
        plugin.getAbilityManager().registerAbility(new BreezeBurstStrike(plugin));
        plugin.getAbilityManager().registerAbility(new DragonClawSlash(plugin));
        plugin.getAbilityManager().registerAbility(new AbyssalDrownStrike(plugin));

        // JJK Expansion (50)
        plugin.getAbilityManager().registerAbility(new DivergentFist(plugin));
        plugin.getAbilityManager().registerAbility(new BlackFlash(plugin));
        plugin.getAbilityManager().registerAbility(new SukunaCleave(plugin));
        plugin.getAbilityManager().registerAbility(new SukunaDismantle(plugin));
        plugin.getAbilityManager().registerAbility(new SukunaFireArrow(plugin));
        plugin.getAbilityManager().registerAbility(new MalevolentShrine(plugin));
        plugin.getAbilityManager().registerAbility(new GojoInfinity(plugin));
        plugin.getAbilityManager().registerAbility(new LapseBlue(plugin));
        plugin.getAbilityManager().registerAbility(new ReversalRed(plugin));
        plugin.getAbilityManager().registerAbility(new HollowPurple(plugin));
        plugin.getAbilityManager().registerAbility(new UnlimitedVoid(plugin));
        plugin.getAbilityManager().registerAbility(new DivineDog(plugin));
        plugin.getAbilityManager().registerAbility(new NueLightning(plugin));
        plugin.getAbilityManager().registerAbility(new ToadTongue(plugin));
        plugin.getAbilityManager().registerAbility(new MaxElephant(plugin));
        plugin.getAbilityManager().registerAbility(new RabbitEscape(plugin));
        plugin.getAbilityManager().registerAbility(new MahoragaSummon(plugin));
        plugin.getAbilityManager().registerAbility(new ChimeraShadowGarden(plugin));
        plugin.getAbilityManager().registerAbility(new StrawDollResonance(plugin));
        plugin.getAbilityManager().registerAbility(new StrawDollHairpin(plugin));
        plugin.getAbilityManager().registerAbility(new RatioStrike(plugin));
        plugin.getAbilityManager().registerAbility(new CollapseStrike(plugin));
        plugin.getAbilityManager().registerAbility(new BoogieWoogie(plugin));
        plugin.getAbilityManager().registerAbility(new EmberInsect(plugin));
        plugin.getAbilityManager().registerAbility(new MaximumMeteor(plugin));
        plugin.getAbilityManager().registerAbility(new FlowerField(plugin));
        plugin.getAbilityManager().registerAbility(new WoodenRoots(plugin));
        plugin.getAbilityManager().registerAbility(new CursedBud(plugin));
        plugin.getAbilityManager().registerAbility(new WaterTorrent(plugin));
        plugin.getAbilityManager().registerAbility(new BloodPierce(plugin));
        plugin.getAbilityManager().registerAbility(new BloodSupernova(plugin));
        plugin.getAbilityManager().registerAbility(new SlicingExorcism(plugin));
        plugin.getAbilityManager().registerAbility(new IdleTransfiguration(plugin));
        plugin.getAbilityManager().registerAbility(new SoulMultiplicity(plugin));
        plugin.getAbilityManager().registerAbility(new BodyRepel(plugin));
        plugin.getAbilityManager().registerAbility(new SelfEmbodiment(plugin));
        plugin.getAbilityManager().registerAbility(new SpeechStop(plugin));
        plugin.getAbilityManager().registerAbility(new SpeechExplode(plugin));
        plugin.getAbilityManager().registerAbility(new SpeechBlastAway(plugin));
        plugin.getAbilityManager().registerAbility(new PlayfulCloud(plugin));
        plugin.getAbilityManager().registerAbility(new PureLoveBeam(plugin));
        plugin.getAbilityManager().registerAbility(new CopyTechnique(plugin));
        plugin.getAbilityManager().registerAbility(new AuthenticMutualLove(plugin));
        plugin.getAbilityManager().registerAbility(new MaximumUzumaki(plugin));
        plugin.getAbilityManager().registerAbility(new InstantSpiritBody(plugin));
        plugin.getAbilityManager().registerAbility(new Projection24FPS(plugin));
        plugin.getAbilityManager().registerAbility(new ComedianShow(plugin));
        plugin.getAbilityManager().registerAbility(new WindScythe(plugin));
        plugin.getAbilityManager().registerAbility(new BirdStrike(plugin));
        plugin.getAbilityManager().registerAbility(new IdleDeathGamble(plugin));

        plugin.getServer().getPluginManager().registerEvents(new NewExpansionAbilities(), plugin);


        // Magma Trail Damage Loop
        new CompatRunnable() {
            @Override
            public void run() {
                synchronized (magmaTrailLocations) {
                    Ability ab = plugin.getAbilityManager().getAbility("magma_trail");
                    double damage = ab != null ? ab.getDoubleParam(null, "damage", 1.5) : 1.5;
                    int fireTicks = ab != null ? ab.getIntParam(null, "fire_ticks", 40) : 40;
                    double radius = ab != null ? ab.getDoubleParam(null, "radius", 1.2) : 1.2;
                    Iterator<Location> it = magmaTrailLocations.iterator();
                    while (it.hasNext()) {
                        Location loc = it.next();
                        SchedulerUtils.runTask(plugin, loc, () -> {
                            loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.2, 0.1, 0.2, 0.02);
                            for (Entity ent : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                                if (ent instanceof LivingEntity) {
                                    LivingEntity le = (LivingEntity) ent;
                                    le.setFireTicks(fireTicks);
                                    le.damage(damage);
                                }
                            }
                        });
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    public static void addMagmaTrailLocation(Location loc) {
        synchronized (magmaTrailLocations) {
            magmaTrailLocations.add(loc);
            Ability ab = pluginInstance.getAbilityManager().getAbility("magma_trail");
            long durationTicks = ab != null ? ab.getIntParam(null, "trail_duration_ticks", 100) : 100;
            new CompatRunnable() {
                @Override
                public void run() {
                    synchronized (magmaTrailLocations) {
                        magmaTrailLocations.remove(loc);
                    }
                }
            }.runTaskLater(pluginInstance, durationTicks); // default 5 seconds duration
        }
    }

    public static void addBreezeDeflect(UUID uuid, long durationMs) {
        activeBreezeDeflects.put(uuid, System.currentTimeMillis() + durationMs);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        if (entity.hasMetadata("ghast_fireball")) {
            Location loc = event.getHitBlock() != null ? event.getHitBlock().getLocation() : event.getEntity().getLocation();
            float yield = entity.hasMetadata("ghast_fireball_yield") ? (float) entity.getMetadata("ghast_fireball_yield").get(0).asDouble() : 4.0f;
            loc.getWorld().createExplosion(loc, yield, true, true);
        } else if (entity.hasMetadata("shulker_bullet")) {
            if (event.getHitEntity() instanceof LivingEntity) {
                int duration = entity.hasMetadata("shulker_bullet_duration") ? entity.getMetadata("shulker_bullet_duration").get(0).asInt() : 100;
                int amp = entity.hasMetadata("shulker_bullet_amplifier") ? entity.getMetadata("shulker_bullet_amplifier").get(0).asInt() : 1;
                ((LivingEntity) event.getHitEntity()).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amp));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            Long expire = activeBreezeDeflects.get(p.getUniqueId());
            if (expire != null && System.currentTimeMillis() < expire) {
                if (event.getDamager() instanceof Projectile) {
                    event.setCancelled(true);
                    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 15, 0.4, 0.4, 0.4, 0.1);
                    playSoundSafe(p.getLocation(), "ENTITY_WIND_CHARGE_THROW", Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 0.8f, 1.4f);
                }
            }
        }

        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player p = (Player) event.getDamager();
            LivingEntity t = (LivingEntity) event.getEntity();
            ItemStack hand = p.getInventory().getItemInMainHand();
            List<String> abs = pluginInstance.getAbilityManager().getItemAbilities(hand);
            
            if (abs.contains("magma_fist")) {
                Ability ab = pluginInstance.getAbilityManager().getAbility("magma_fist");
                double dmg = ab != null ? ab.getDoubleParam(hand, "damage", 3.0) : 3.0;
                int fireTicks = ab != null ? ab.getIntParam(hand, "fire_ticks", 100) : 100;
                t.setFireTicks(fireTicks);
                t.damage(dmg);
                t.getWorld().spawnParticle(Particle.FLAME, t.getLocation(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            if (abs.contains("husk_hunger_strike")) {
                Ability ab = pluginInstance.getAbilityManager().getAbility("husk_hunger_strike");
                double duration = ab != null ? ab.getDoubleParam(hand, "duration", 6.0) : 6.0;
                int amp = ab != null ? ab.getIntParam(hand, "amplifier", 2) : 2;
                t.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, (int) (duration * 20), amp));
                t.getWorld().playSound(t.getLocation(), Sound.ENTITY_HUSK_AMBIENT, 0.8f, 0.9f);
            }
            if (abs.contains("piglin_rage")) {
                Ability ab = pluginInstance.getAbilityManager().getAbility("piglin_rage");
                double scale = ab != null ? ab.getDoubleParam(hand, "scale", 1.0) : 1.0;
                double mult = 1.0 + (1.0 - (p.getHealth() / p.getMaxHealth())) * scale;
                event.setDamage(event.getDamage() * mult);
            }
            if (abs.contains("vampiric_edge")) {
                Ability ab = pluginInstance.getAbilityManager().getAbility("vampiric_edge");
                double stealPercent = ab != null ? ab.getDoubleParam(hand, "steal_percent", 0.15) : 0.15;
                double maxHeal = ab != null ? ab.getDoubleParam(hand, "max_heal", 2.0) : 2.0;
                double heal = Math.min(maxHeal, event.getFinalDamage() * stealPercent);
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + heal));
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }
}

// Subclasses (1-20 Nether)
class MagmaShield extends Ability {
    public MagmaShield(ItemEditFull pl) { super("magma_shield", "Magma Shield", "Defends against fire and damage."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 15.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) (duration * 20), amp));
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        return true;
    }
}
class MagmaJump extends Ability {
    public MagmaJump(ItemEditFull pl) { super("magma_jump", "Magma Jump", "Leap high using thermal energy."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "velocity", 1.2);
        p.setVelocity(new Vector(0, velocity, 0));
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        return true;
    }
}
class MagmaTrail extends Ability {
    private final ItemEditFull plugin;
    public MagmaTrail(ItemEditFull pl) { super("magma_trail", "Magma Trail", "Leaves a blazing path."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int durationSeconds = getIntParam(i, "duration_seconds", 5);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || ticks++ > (durationSeconds * 2)) { cancel(); return; }
                NewExpansionAbilities.addMagmaTrailLocation(p.getLocation().add(0, 0.1, 0));
            }
        }.runTaskTimer(plugin, p, 0L, 10L);
        return true;
    }
}
class MagmaFist extends Ability {
    public MagmaFist(ItemEditFull pl) { super("magma_fist", "Magma Fist", "Strike with fiery impact."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.1f); return true; }
}
class BlazeSpeed extends Ability {
    public BlazeSpeed(ItemEditFull pl) { super("blaze_speed", "Blaze Speed", "High velocity flame dash."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), amp));
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
        return true;
    }
}
class BlazeAura extends Ability {
    private final ItemEditFull plugin;
    public BlazeAura(ItemEditFull pl) { super("blaze_aura", "Blaze Aura", "Burns nearby targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        double radius = getDoubleParam(i, "radius", 4.0);
        double damage = getDoubleParam(i, "damage", 2.0);
        int fireTicks = getIntParam(i, "fire_ticks", 80);
        new CompatRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || ticks++ > 5) { cancel(); return; }
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 15, radius, 0.5, radius, 0.05);
                for (Entity ent : loc.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.setFireTicks(fireTicks);
                        le.damage(damage, p);
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 20L);
        return true;
    }
}
class BlazeFlight extends Ability {
    public BlazeFlight(ItemEditFull pl) { super("blaze_flight", "Blaze Flight", "Propels player upwards."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double multiplier = getDoubleParam(i, "speed_multiplier", 1.5);
        p.setVelocity(p.getLocation().getDirection().multiply(multiplier));
        return true;
    }
}
class BlazeBarrage extends Ability {
    public BlazeBarrage(ItemEditFull pl) { super("blaze_barrage", "Blaze Barrage", "Fires fireballs."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(SmallFireball.class); return true; }
}
class GhastFloat extends Ability {
    public GhastFloat(ItemEditFull pl) { super("ghast_float", "Ghast Float", "Safe slow falling."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) (duration * 20), 0));
        return true;
    }
}
class GhastFireball extends Ability {
    private final ItemEditFull pl;
    public GhastFireball(ItemEditFull pl) { super("ghast_fireball", "Ghast Fireball", "Spawns giant explosions."); this.pl = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Fireball f = p.launchProjectile(LargeFireball.class);
        f.setMetadata("ghast_fireball", new FixedMetadataValue(pl, true));
        double yield = getDoubleParam(i, "yield", 4.0);
        f.setMetadata("ghast_fireball_yield", new FixedMetadataValue(pl, yield));
        return true;
    }
}
class GhastScream extends Ability {
    public GhastScream(ItemEditFull pl) { super("ghast_scream", "Ghast Scream", "Knocks back targets."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.5f, 1f); return true; }
}
class GhastTear extends Ability {
    public GhastTear(ItemEditFull pl) { super("ghast_tear", "Ghast Tear", "Quick health restore."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 5.0);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (duration * 20), amp));
        return true;
    }
}
class PiglinGreed extends Ability {
    public PiglinGreed(ItemEditFull pl) { super("piglin_greed", "Piglin Greed", "Adrenaline boost."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 8.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (duration * 20), amp));
        return true;
    }
}
class PiglinCrossbow extends Ability {
    public PiglinCrossbow(ItemEditFull pl) { super("piglin_crossbow", "Piglin Crossbow", "Ranged barrage."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(Arrow.class); return true; }
}
class PiglinRage extends Ability {
    public PiglinRage(ItemEditFull pl) { super("piglin_rage", "Piglin Rage", "Damage scales with missing health."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class PiglinBarter extends Ability {
    public PiglinBarter(ItemEditFull pl) { super("piglin_barter", "Piglin Barter", "Boosts luck."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 30.0);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, (int) (duration * 20), amp));
        return true;
    }
}
class NetherQuake extends Ability {
    public NetherQuake(ItemEditFull pl) { super("nether_quake", "Nether Quake", "Slam waves."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class NetherHeat extends Ability {
    public NetherHeat(ItemEditFull pl) { super("nether_heat", "Nether Heat", "Igniation aura."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class NetherPortalRift extends Ability {
    public NetherPortalRift(ItemEditFull pl) { super("nether_portal_rift", "Nether Rift", "Portal dash."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double range = getDoubleParam(i, "range", 6.0);
        p.teleport(p.getLocation().add(p.getLocation().getDirection().multiply(range)));
        return true;
    }
}
class NetherSoulDrain extends Ability {
    public NetherSoulDrain(ItemEditFull pl) { super("nether_soul_drain", "Nether Soul Drain", "Drain life force."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}

// Subclasses (21-40 Ocean)
class GuardianBeam extends Ability {
    public GuardianBeam(ItemEditFull pl) { super("guardian_beam", "Guardian Beam", "Beam blast."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class GuardianThorns extends Ability {
    public GuardianThorns(ItemEditFull pl) { super("guardian_thorns", "Guardian Thorns", "Recoil defense."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class ElderGuardianFatigue extends Ability {
    public ElderGuardianFatigue(ItemEditFull pl) { super("elder_guardian_fatigue", "Elder Fatigue", "Fatigues enemies."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class ElderGuardianGhost extends Ability {
    public ElderGuardianGhost(ItemEditFull pl) { super("elder_guardian_ghost", "Elder Curse", "Curse targets."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class DrownedTridentAbility extends Ability {
    public DrownedTridentAbility(ItemEditFull pl) { super("drowned_trident", "Drowned Trident", "Trident strike."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(Trident.class); return true; }
}
class DrownedWaterLeap extends Ability {
    public DrownedWaterLeap(ItemEditFull pl) { super("drowned_water_leap", "Water Leap", "Quick water vault."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "velocity", 1.1);
        p.setVelocity(new Vector(0, velocity, 0));
        return true;
    }
}
class DrownedConduitPower extends Ability {
    public DrownedConduitPower(ItemEditFull pl) { super("drowned_conduit_power", "Conduit Grace", "Conduit strength."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 15.0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, (int) (duration * 20), 0));
        return true;
    }
}
class DrownedDepthStrider extends Ability {
    public DrownedDepthStrider(ItemEditFull pl) { super("drowned_depth_strider", "Drowned Speed", "Agile swimming."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 15.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, (int) (duration * 20), amp));
        return true;
    }
}
class DolphinGraceAbility extends Ability {
    public DolphinGraceAbility(ItemEditFull pl) { super("dolphin_grace", "Dolphin Grace", "Speed in water."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 20.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, (int) (duration * 20), amp));
        return true;
    }
}
class DolphinLeap extends Ability {
    public DolphinLeap(ItemEditFull pl) { super("dolphin_leap", "Dolphin Leap", "Launch out of water."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double multiplier = getDoubleParam(i, "speed_multiplier", 1.8);
        double leapY = getDoubleParam(i, "leap_y", 0.5);
        p.setVelocity(p.getLocation().getDirection().multiply(multiplier).setY(leapY));
        return true;
    }
}
class DolphinSonic extends Ability {
    public DolphinSonic(ItemEditFull pl) { super("dolphin_sonic", "Dolphin Sonic", "Sonic burst."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class TurtleShell extends Ability {
    public TurtleShell(ItemEditFull pl) { super("turtle_shell", "Turtle Shell", "Crouch defense."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        int amp = getIntParam(i, "amplifier", 2);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) (duration * 20), amp));
        return true;
    }
}
class TurtleFortitude extends Ability {
    public TurtleFortitude(ItemEditFull pl) { super("turtle_fortitude", "Turtle Fortitude", "Steady ground."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SquidInkAbility extends Ability {
    public SquidInkAbility(ItemEditFull pl) { super("squid_ink", "Squid Ink", "Blind targets nearby."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SquidBlindness extends Ability {
    public SquidBlindness(ItemEditFull pl) { super("squid_blindness", "Ink Strike", "Strikes blind."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SquidPropulsion extends Ability {
    public SquidPropulsion(ItemEditFull pl) { super("squid_propulsion", "Ink Jets", "Rapid dash backwards."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double multiplier = getDoubleParam(i, "speed_multiplier", -1.5);
        p.setVelocity(p.getLocation().getDirection().multiply(multiplier));
        return true;
    }
}
class FishAgility extends Ability {
    public FishAgility(ItemEditFull pl) { super("fish_agility", "Fish Agility", "Swift aquatic maneuvers."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class FishWaterBreathing extends Ability {
    public FishWaterBreathing(ItemEditFull pl) { super("fish_water_breathing", "Water Gills", "Breathe underwater."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 60.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration * 20), amp));
        return true;
    }
}
class OceanTempest extends Ability {
    public OceanTempest(ItemEditFull pl) { super("ocean_tempest", "Water Tempest", "Launches entities."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 4.0);
        double damage = getDoubleParam(i, "damage", 4.0);
        double launchVelocity = getDoubleParam(i, "launch_velocity", 0.8);
        p.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.8f);
        p.getWorld().spawnParticle(Particle.WATER_SPLASH, loc, 50, radius, 1.0, radius, 0.1);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, radius, 3.0, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.setVelocity(new Vector(0, launchVelocity, 0));
                le.damage(damage, p);
            }
        }
        return true;
    }
}
class OceanTsunami extends Ability {
    public OceanTsunami(ItemEditFull pl) { super("ocean_tsunami", "Tsunami Wave", "Wave push."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        double damage = getDoubleParam(i, "damage", 3.5);
        double range = getDoubleParam(i, "range", 6.0);
        double pushVelocity = getDoubleParam(i, "push_velocity", 1.4);
        double pushY = getDoubleParam(i, "push_y", 0.25);
        double radius = getDoubleParam(i, "radius", 1.5);
        p.getWorld().playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.5f, 0.7f);
        for (int k = 1; k <= range; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            p.getWorld().spawnParticle(Particle.WATER_SPLASH, step, 10, 0.5, 0.5, 0.5, 0.05);
            for (Entity ent : step.getWorld().getNearbyEntities(step, radius, radius, radius)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.setVelocity(dir.clone().multiply(pushVelocity).setY(pushY));
                    le.damage(damage, p);
                }
            }
        }
        return true;
    }
}

// Subclasses (41-60 Sky)
class PhantomGlide extends Ability {
    public PhantomGlide(ItemEditFull pl) { super("phantom_glide", "Phantom Glide", "Slow glide."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) (duration * 20), amp));
        return true;
    }
}
class PhantomSwoop extends Ability {
    public PhantomSwoop(ItemEditFull pl) { super("phantom_swoop", "Phantom Swoop", "Aerial slam."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double speed = getDoubleParam(i, "speed_multiplier", 1.8);
        double swoopY = getDoubleParam(i, "swoop_y", -0.5);
        p.setVelocity(p.getLocation().getDirection().multiply(speed).setY(swoopY));
        return true;
    }
}
class PhantomInsomnia extends Ability {
    public PhantomInsomnia(ItemEditFull pl) { super("phantom_insomnia", "Insomnia Strike", "Apply darkness."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class PhantomBite extends Ability {
    public PhantomBite(ItemEditFull pl) { super("phantom_bite", "Phantom Bite", "Life leech."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BreezeWindCharge extends Ability {
    public BreezeWindCharge(ItemEditFull pl) { super("breeze_wind_charge", "Wind Charge", "Fires blast."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(WindCharge.class); return true; }
}
class BreezeDeflect extends Ability {
    public BreezeDeflect(ItemEditFull pl) { super("breeze_deflect", "Wind Shield", "Arrows bounce."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        NewExpansionAbilities.addBreezeDeflect(p.getUniqueId(), (long) (duration * 1000));
        NewExpansionAbilities.playSoundSafe(p.getLocation(), "ENTITY_WIND_CHARGE_WIND_BURST", Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1.2f, 1.2f);
        return true;
    }
}
class BreezeLeap extends Ability {
    public BreezeLeap(ItemEditFull pl) { super("breeze_leap", "Breeze Leap", "Vault high."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "velocity", 1.4);
        p.setVelocity(new Vector(0, velocity, 0));
        return true;
    }
}
class BreezeGust extends Ability {
    public BreezeGust(ItemEditFull pl) { super("breeze_gust", "Gust Blast", "Wind push."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 4.0);
        double damage = getDoubleParam(i, "damage", 2.0);
        double pushVelocity = getDoubleParam(i, "push_velocity", 1.5);
        double pushY = getDoubleParam(i, "push_y", 0.3);
        NewExpansionAbilities.playSoundSafe(loc, "ENTITY_WIND_CHARGE_WIND_BURST", Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1.5f, 0.9f);
        p.getWorld().spawnParticle(Particle.CLOUD, loc, 30, radius, 0.5, radius, 0.15);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                Vector push = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(pushVelocity).setY(pushY);
                le.setVelocity(push);
                le.damage(damage, p);
            }
        }
        return true;
    }
}
class BatSonar extends Ability {
    public BatSonar(ItemEditFull pl) { super("bat_sonar", "Bat Sonar", "Highlights targets."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BatScreech extends Ability {
    public BatScreech(ItemEditFull pl) { super("bat_screech", "Bat Screech", "Disorients mobs."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BatFlight extends Ability {
    public BatFlight(ItemEditFull pl) { super("bat_flight", "Bat Flight", "Flaps upwards."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "velocity", 0.7);
        p.setVelocity(new Vector(0, velocity, 0));
        return true;
    }
}
class ChickenEggVolley extends Ability {
    public ChickenEggVolley(ItemEditFull pl) { super("chicken_egg_volley", "Egg Volley", "Explosive egg shower."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(Egg.class); return true; }
}
class ChickenFeatherShield extends Ability {
    public ChickenFeatherShield(ItemEditFull pl) { super("chicken_feather_shield", "Feather Shield", "Fall safety."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 5.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) (duration * 20), amp));
        return true;
    }
}
class ChickenSlowFall extends Ability {
    public ChickenSlowFall(ItemEditFull pl) { super("chicken_slow_fall", "Poultry Glide", "Slow fall glide."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 15.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) (duration * 20), amp));
        return true;
    }
}
class BeeSting extends Ability {
    public BeeSting(ItemEditFull pl) { super("bee_sting", "Bee Sting", "Poison attack."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BeeHoneyTrap extends Ability {
    public BeeHoneyTrap(ItemEditFull pl) { super("bee_honey_trap", "Honey Trap", "Slow pool."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BeeSwarmSummon extends Ability {
    public BeeSwarmSummon(ItemEditFull pl) { super("bee_swarm", "Summon Bee Swarm", "Summon helpers."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BeePollinate extends Ability {
    public BeePollinate(ItemEditFull pl) { super("bee_pollinate", "Bee Pollen", "Heals."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SkyLightningStrike extends Ability {
    public SkyLightningStrike(ItemEditFull pl) { super("sky_lightning_strike", "Lightning Strike", "Lightning strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 30);
        p.getWorld().strikeLightning(p.getTargetBlock(null, range).getLocation());
        return true;
    }
}
class SkyZephyr extends Ability {
    public SkyZephyr(ItemEditFull pl) { super("sky_zephyr", "Sky Zephyr", "Wind lift."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
 
// Subclasses (61-70 Ancient/Sculk)
class SculkShriekerSound extends Ability {
    public SculkShriekerSound(ItemEditFull pl) { super("sculk_shrieker_sound", "Sculk Shriek", "Sound blast."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.5f, 1f); return true; }
}
class SculkSensorPing extends Ability {
    public SculkSensorPing(ItemEditFull pl) { super("sculk_sensor_ping", "Sensor Ping", "Detects motion."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double duration = getDoubleParam(i, "duration", 5.0);
        double radius = getDoubleParam(i, "radius", 10.0);
        int amp = getIntParam(i, "amplifier", 0);
        NewExpansionAbilities.playSoundSafe(loc, "BLOCK_SCULK_SENSOR_CLICK", Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 25, radius, 1.0, radius, 0.05);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, radius, 3.0, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration * 20), amp));
            }
        }
        return true;
    }
}
class SculkBlindnessAura extends Ability {
    public SculkBlindnessAura(ItemEditFull pl) { super("sculk_blindness_aura", "Sculk Darkness", "Apply darkness."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 10.0);
        int amp = getIntParam(i, "amplifier", 0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) (duration * 20), amp));
        return true;
    }
}
class SculkInfectionStrike extends Ability {
    private final ItemEditFull plugin;
    public SculkInfectionStrike(ItemEditFull pl) { super("sculk_infection_strike", "Sculk Infection", "Infects target on strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 5.0);
        int range = getIntParam(i, "range", 6);
        int amp = getIntParam(i, "amplifier", 1);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        LivingEntity living = (LivingEntity) target;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 1.2f);
        living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration * 20), amp));
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 10 || !living.isValid()) { cancel(); return; }
                living.getWorld().spawnParticle(Particle.SCULK_CHARGE, living.getLocation().add(0, 1.0, 0), 5, 0.3, 0.5, 0.3, 0.05);
            }
        }.runTaskTimer(plugin, living, 0L, 10L);
        return true;
    }
}
class SculkVeinSpread extends Ability {
    public SculkVeinSpread(ItemEditFull pl) { super("sculk_vein_spread", "Sculk Spread", "Trail blocks."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SculkCatalystHeal extends Ability {
    public SculkCatalystHeal(ItemEditFull pl) { super("sculk_catalyst_heal", "Sculk Catalyst", "Drains XP."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class WardenSonicStrike extends Ability {
    private final ItemEditFull plugin;
    public WardenSonicStrike(ItemEditFull pl) { super("warden_sonic_strike", "Sonic Strike", "Launches a linear sonic boom strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double damage = getDoubleParam(i, "damage", 6.0);
        double pushVelocity = getDoubleParam(i, "push_velocity", 1.5);
        double pushY = getDoubleParam(i, "push_y", 0.4);
        p.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);
        new CompatRunnable() {
            int step = 0;
            Location current = origin.clone();
            @Override public void run() {
                if (step++ > 20 || !current.getBlock().getType().isAir()) { cancel(); return; }
                current.add(dir.clone().multiply(0.8));
                current.getWorld().spawnParticle(Particle.SONIC_BOOM, current, 1, 0, 0, 0, 0);
                for (Entity ent : current.getWorld().getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        ((LivingEntity) ent).damage(damage, p);
                        ent.setVelocity(dir.clone().multiply(pushVelocity).setY(pushY));
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class WardenDarknessBurst extends Ability {
    private final ItemEditFull plugin;
    public WardenDarknessBurst(ItemEditFull pl) { super("warden_darkness_burst", "Darkness Burst", "Dark wave."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location base = p.getLocation();
        double damage = getDoubleParam(i, "damage", 3.0);
        double duration = getDoubleParam(i, "duration", 6.0);
        int amp = getIntParam(i, "amplifier", 0);
        int stepsLimit = getIntParam(i, "steps", 4);
        double radiusStep = getDoubleParam(i, "radius_step", 1.5);
        p.getWorld().playSound(base, Sound.ENTITY_WARDEN_ROAR, 1.2f, 0.8f);
        new CompatRunnable() {
            int step = 1;
            @Override public void run() {
                if (step > stepsLimit) { cancel(); return; }
                double radius = step * radiusStep;
                for (double angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = base.clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius);
                    particleLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                for (Entity entity : base.getWorld().getNearbyEntities(base, radius, 2.0, radius)) {
                    if (entity instanceof LivingEntity && !entity.equals(p)) {
                        LivingEntity living = (LivingEntity) entity;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) (duration * 20), amp));
                        living.damage(damage, p);
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
    }
}
class WardenSculkStep extends Ability {
    private final ItemEditFull plugin;
    public WardenSculkStep(ItemEditFull pl) { super("warden_sculk_step", "Sculk Step", "Sculk charge speed step."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        double duration = getDoubleParam(i, "duration", 15.0);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), amp));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_CHARGE, 1.0f, 1.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > (duration * 2) || !p.isOnline()) { cancel(); return; }
                p.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, p.getLocation().add(0, 0.1, 0), 2, 0.2, 0.1, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, p, 0L, 10L);
        return true;
    }
}
class WardenVibrationSense extends Ability {
    private final ItemEditFull plugin;
    public WardenVibrationSense(ItemEditFull pl) { super("warden_vibration_sense", "Vibration Sense", "Highlights targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location base = p.getLocation();
        double duration = getDoubleParam(i, "duration", 5.0);
        double maxRadius = getDoubleParam(i, "max_radius", 8.0);
        double radiusStep = getDoubleParam(i, "radius_step", 2.0);
        double horizontalRange = getDoubleParam(i, "horizontal_range", 10.0);
        double verticalRange = getDoubleParam(i, "vertical_range", 4.0);
        int amp = getIntParam(i, "amplifier", 0);
        NewExpansionAbilities.playSoundSafe(base, "BLOCK_SCULK_SENSOR_CLICK", Sound.BLOCK_SCULK_SENSOR_PLACE, 1.2f, 1.2f);
        for (double r = 1.0; r <= maxRadius; r += radiusStep) {
            final double radius = r;
            new CompatRunnable() {
                @Override public void run() {
                    for (double angle = 0; angle < 360; angle += 15) {
                        double rad = Math.toRadians(angle);
                        Location particleLoc = base.clone().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
                        particleLoc.getWorld().spawnParticle(Particle.SPELL_INSTANT, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }.runTaskLater(plugin, base, (long) (r * 2L));
        }
        for (Entity entity : base.getWorld().getNearbyEntities(base, horizontalRange, verticalRange, horizontalRange)) {
            if (entity instanceof LivingEntity && !entity.equals(p)) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration * 20), amp));
            }
        }
        return true;
    }
}
// Subclasses (71-80 Ice)
class StraySlownessArrow extends Ability {
    public StraySlownessArrow(ItemEditFull pl) { super("stray_slowness_arrow", "Slowness Arrow", "Slowness shots."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(Arrow.class); return true; }
}
class StrayFreezeTouch extends Ability {
    private static final Map<UUID, Long> activeFreezeTouches = new HashMap<>();
    private final ItemEditFull plugin;
    public StrayFreezeTouch(ItemEditFull pl) {
        super("stray_freeze_touch", "Freeze Touch", "Freezes targets on next hit.");
        this.plugin = pl;
        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onHit(EntityDamageByEntityEvent event) {
                if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
                    Player p = (Player) event.getDamager();
                    Long expire = activeFreezeTouches.get(p.getUniqueId());
                    if (expire != null && System.currentTimeMillis() < expire) {
                        activeFreezeTouches.remove(p.getUniqueId());
                        LivingEntity living = (LivingEntity) event.getEntity();
                        ItemStack hand = p.getInventory().getItemInMainHand();
                        int slowDuration = getIntParam(hand, "slow_duration", 80);
                        int slowAmp = getIntParam(hand, "slow_amplifier", 4);
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmp));
                        living.getWorld().spawnParticle(Particle.SNOWFLAKE, living.getLocation().add(0, 1.0, 0), 15, 0.2, 0.4, 0.2, 0.02);
                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
                    }
                }
            }
        }, plugin);
    }
    @Override public boolean trigger(Player p, ItemStack i) {
        double activeSec = getDoubleParam(i, "active_duration_seconds", 10.0);
        activeFreezeTouches.put(p.getUniqueId(), System.currentTimeMillis() + (long)(activeSec * 1000));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1.0f, 1.2f);
        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0.02);
        return true;
    }
}
class StraySnowStorm extends Ability {
    private final ItemEditFull plugin;
    public StraySnowStorm(ItemEditFull pl) { super("stray_snow_storm", "Blizzard Storm", "Snow wave."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 15);
        double damage = getDoubleParam(i, "damage", 1.5);
        int slowDuration = getIntParam(i, "slow_duration", 40);
        int slowAmp = getIntParam(i, "slow_amplifier", 2);
        int durationTicks = getIntParam(i, "duration_ticks", 30);
        Location target = p.getTargetBlock(null, range).getLocation();
        p.getWorld().playSound(target, Sound.BLOCK_SNOW_BREAK, 1.2f, 0.8f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > durationTicks) { cancel(); return; }
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.clone().add(0, 4.0, 0), 15, 3.0, 0.2, 3.0, 0.05);
                for (Entity ent : target.getWorld().getNearbyEntities(target, 3.0, 4.0, 3.0)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmp));
                        if (ticks % 5 == 0) le.damage(damage, p);
                    }
                }
            }
        }.runTaskTimer(plugin, target, 0L, 2L);
        return true;
    }
}
class SnowGolemSnowball extends Ability {
    public SnowGolemSnowball(ItemEditFull pl) { super("snow_golem_snowball", "Snowball Barrage", "Shoot snowballs."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(Snowball.class); return true; }
}
class SnowGolemTrail extends Ability {
    private final ItemEditFull plugin;
    public SnowGolemTrail(ItemEditFull pl) { super("snow_golem_trail", "Snow Trail", "Leaves snow trail."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int durationTicks = getIntParam(i, "duration_ticks", 60);
        int trailTicks = getIntParam(i, "trail_duration_ticks", 100);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SNOW_PLACE, 1.0f, 1.0f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > durationTicks || !p.isOnline()) { cancel(); return; }
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.SNOW_SHOVEL, loc.add(0, 0.1, 0), 2, 0.1, 0.05, 0.1, 0.01);
                Block b = loc.getBlock();
                if (b.getType() == Material.AIR && b.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
                    b.setType(Material.SNOW);
                    new CompatRunnable() {
                        @Override public void run() { if (b.getType() == Material.SNOW) b.setType(Material.AIR); }
                    }.runTaskLater(plugin, b.getLocation(), (long) trailTicks);
                }
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
    }
}
class SnowGolemFreeze extends Ability {
    private final ItemEditFull plugin;
    public SnowGolemFreeze(ItemEditFull pl) { super("snow_golem_freeze", "Snow Golem Freeze", "Freeze block."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 8);
        int freezeTicks = getIntParam(i, "freeze_duration_ticks", 80);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity living = (LivingEntity) target;
        Location loc = living.getLocation();
        living.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 0.8f);
        final List<Block> snowBlocks = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = loc.clone().add(x, 0, z).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.SNOW_BLOCK);
                    snowBlocks.add(b);
                }
            }
        }
        new CompatRunnable() {
            @Override public void run() {
                for (Block b : snowBlocks) {
                    if (b.getType() == Material.SNOW_BLOCK) b.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, loc, (long) freezeTicks);
        return true;
    }
}
class IceSpikeSummon extends Ability {
    private final ItemEditFull plugin;
    public IceSpikeSummon(ItemEditFull pl) { super("ice_spike_summon", "Ice Spike", "Summons spikes."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 15);
        int spikeHeight = getIntParam(i, "spike_height", 3);
        double damage = getDoubleParam(i, "damage", 5.0);
        double launchVelocity = getDoubleParam(i, "launch_velocity", 1.0);
        int iceTicks = getIntParam(i, "ice_decay_duration_ticks", 60);
        Block target = p.getTargetBlock(null, range);
        if (target == null || target.getType() == Material.AIR) return false;
        Location spikeLoc = target.getLocation().add(0.5, 1.0, 0.5);
        p.getWorld().playSound(spikeLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
        final List<Block> spikes = new ArrayList<>();
        for (int y = 0; y < spikeHeight; y++) {
            Block b = spikeLoc.clone().add(0, y, 0).getBlock();
            if (b.getType() == Material.AIR) {
                b.setType(Material.PACKED_ICE);
                spikes.add(b);
            }
        }
        for (Entity ent : spikeLoc.getWorld().getNearbyEntities(spikeLoc, 1.5, 3.0, 1.5)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.damage(damage, p);
                le.setVelocity(new Vector(0, launchVelocity, 0));
            }
        }
        new CompatRunnable() {
            @Override public void run() {
                for (Block b : spikes) {
                    if (b.getType() == Material.PACKED_ICE) b.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, spikeLoc, (long) iceTicks);
        return true;
    }
}
class IceShield extends Ability {
    public IceShield(ItemEditFull pl) { super("ice_shield", "Ice Shield", "Defensive shield."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int duration = getIntParam(i, "duration", 200);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, amp));
        return true;
    }
}
class IceSkate extends Ability {
    public IceSkate(ItemEditFull pl) { super("ice_skate", "Ice Skate", "Speed on ice."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int duration = getIntParam(i, "duration", 200);
        int amp = getIntParam(i, "amplifier", 2);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amp));
        return true;
    }
}
class IceFrostbite extends Ability {
    private final ItemEditFull plugin;
    public IceFrostbite(ItemEditFull pl) { super("ice_frostbite", "Ice Frostbite", "Continuous freeze."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 10);
        int ticksCount = getIntParam(i, "ticks_count", 5);
        double damage = getDoubleParam(i, "damage", 2.0);
        int slowDuration = getIntParam(i, "slow_duration", 40);
        int slowAmp = getIntParam(i, "slow_amplifier", 2);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity living = (LivingEntity) target;
        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.8f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > ticksCount || !living.isValid()) { cancel(); return; }
                living.damage(damage, p);
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmp));
                living.getWorld().spawnParticle(Particle.SNOWFLAKE, living.getLocation().add(0, 1.0, 0), 5, 0.2, 0.4, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, living, 0L, 20L);
        return true;
    }
}

// Subclasses (81-90 Desert)
class HuskHungerStrike extends Ability {
    private final ItemEditFull plugin;
    public HuskHungerStrike(ItemEditFull pl) { super("husk_hunger_strike", "Hunger Strike", "Applies hunger."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(5);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in sight."); return false; }
        LivingEntity living = (LivingEntity) target;
        living.damage(4.0, p);
        living.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 2));
        p.setFoodLevel(Math.min(20, p.getFoodLevel() + 4));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1.0, 0), 5, 0.2, 0.4, 0.2, 0.02);
        return true;
    }
}
class HuskSandStorm extends Ability {
    private final ItemEditFull plugin;
    public HuskSandStorm(ItemEditFull pl) { super("husk_sand_storm", "Sand Storm", "Sand wave."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location target = p.getTargetBlock(null, 15).getLocation();
        p.getWorld().playSound(target, Sound.BLOCK_SAND_BREAK, 1.2f, 0.8f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 30) { cancel(); return; }
                target.getWorld().spawnParticle(Particle.FALLING_DUST, target.clone().add(0, 3.0, 0), 10, 2.0, 1.0, 2.0, 0.01, Material.SAND.createBlockData());
                for (Entity ent : target.getWorld().getNearbyEntities(target, 3.0, 3.0, 3.0)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                        if (ticks % 10 == 0) le.damage(1.0, p);
                    }
                }
            }
        }.runTaskTimer(plugin, target, 0L, 2L);
        return true;
    }
}
class HuskDesertHeat extends Ability {
    public HuskDesertHeat(ItemEditFull pl) { super("husk_desert_heat", "Desert Heat", "Heat blast."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.ENTITY_HUSK_AMBIENT, 1.2f, 1.2f);
        p.getWorld().spawnParticle(Particle.FLAME, loc, 40, 4.0, 1.0, 4.0, 0.05);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.setFireTicks(60);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
                le.damage(3.0, p);
            }
        }
        return true;
    }
}
class ShulkerLevitationBulletAbility extends Ability {
    private final ItemEditFull pl;
    public ShulkerLevitationBulletAbility(ItemEditFull pl) { super("shulker_levitation_bullet", "Levitation Bullet", "Levitation bullet."); this.pl = pl; }
    @Override public boolean trigger(Player p, ItemStack i) { ShulkerBullet b = p.launchProjectile(ShulkerBullet.class); b.setMetadata("shulker_bullet", new FixedMetadataValue(pl, true)); return true; }
}
class ShulkerShellClose extends Ability {
    public ShulkerShellClose(ItemEditFull pl) { super("shulker_shell_close", "Shell Close", "High defense."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 160, 3)); return true; }
}
class ShulkerTeleport extends Ability {
    public ShulkerTeleport(ItemEditFull pl) { super("shulker_teleport", "Shulker Teleport", "Rift blink."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.teleport(p.getLocation().add(new Vector((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10))); return true; }
}
class DesertMirage extends Ability {
    private final ItemEditFull plugin;
    public DesertMirage(ItemEditFull pl) { super("desert_mirage", "Desert Mirage", "Clones."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
        List<ArmorStand> stands = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            ArmorStand stand = p.getWorld().spawn(loc.clone().add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3), ArmorStand.class);
            stand.setCustomName("§6Desert Mirage");
            stand.setCustomNameVisible(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
            stands.add(stand);
        }
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 30 || !p.isOnline()) {
                    cancel();
                    for (ArmorStand stand : stands) {
                        stand.getWorld().spawnParticle(Particle.FALLING_DUST, stand.getLocation().add(0, 1.0, 0), 10, 0.2, 0.4, 0.2, 0.01, Material.SAND.createBlockData());
                        stand.remove();
                    }
                    return;
                }
                for (ArmorStand stand : stands) {
                    stand.getWorld().spawnParticle(Particle.FALLING_DUST, stand.getLocation().add(0, 1.0, 0), 1, 0.1, 0.2, 0.1, 0.01, Material.SAND.createBlockData());
                }
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
    }
}
class DesertQuicksand extends Ability {
    private final ItemEditFull plugin;
    public DesertQuicksand(ItemEditFull pl) { super("desert_quicksand", "Quicksand", "Trap block."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Block target = p.getTargetBlock(null, 10);
        if (target == null || target.getType() == Material.AIR) return false;
        Location loc = target.getLocation().add(0.5, 1.0, 0.5);
        p.getWorld().playSound(loc, Sound.BLOCK_SAND_BREAK, 1.0f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 40) { cancel(); return; }
                loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 5, 1.0, 0.2, 1.0, 0.01, Material.SAND.createBlockData());
                for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
                        le.setVelocity(new Vector(0, -0.2, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, loc, 0L, 2L);
        return true;
    }
}
class DesertCactusThorns extends Ability {
    private static final Map<UUID, Long> activeThorns = new HashMap<>();
    private final ItemEditFull plugin;
    public DesertCactusThorns(ItemEditFull pl) {
        super("desert_cactus_thorns", "Cactus Thorns", "Thorns recoil.");
        this.plugin = pl;
        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onHit(EntityDamageByEntityEvent event) {
                if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
                    Player p = (Player) event.getEntity();
                    Long expire = activeThorns.get(p.getUniqueId());
                    if (expire != null && System.currentTimeMillis() < expire) {
                        LivingEntity attacker = (LivingEntity) event.getDamager();
                        attacker.damage(event.getDamage() * 0.3, p);
                        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);
                        attacker.getWorld().spawnParticle(Particle.FALLING_DUST, attacker.getLocation().add(0, 1.0, 0), 10, 0.2, 0.4, 0.2, 0.01, Material.CACTUS.createBlockData());
                    }
                }
            }
        }, plugin);
    }
    @Override public boolean trigger(Player p, ItemStack i) {
        activeThorns.put(p.getUniqueId(), System.currentTimeMillis() + 15000L);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.FALLING_DUST, p.getLocation().add(0, 1.0, 0), 15, 0.3, 0.5, 0.3, 0.01, Material.CACTUS.createBlockData());
        return true;
    }
}
class DesertSunstroke extends Ability {
    private final ItemEditFull plugin;
    public DesertSunstroke(ItemEditFull pl) { super("desert_sunstroke", "Sunstroke", "Blinds targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 12);
        double damage = getDoubleParam(i, "damage", 4.0);
        int fireTicks = getIntParam(i, "fire_ticks", 80);
        int blindDuration = getIntParam(i, "blindness_duration", 100);
        int blindAmp = getIntParam(i, "blindness_amplifier", 0);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        LivingEntity living = (LivingEntity) target;
        living.damage(damage, p);
        living.setFireTicks(fireTicks);
        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDuration, blindAmp));
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.5f);
        living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1.0, 0), 15, 0.2, 0.4, 0.2, 0.05);
        return true;
    }
}

// Subclasses (91-100 Magical)
class EvokerFangsAbility extends Ability {
    public EvokerFangsAbility(ItemEditFull pl) { super("evoker_fangs", "Evoker Fangs", "Fang strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double distance = getDoubleParam(i, "distance", 2.0);
        p.getWorld().spawn(p.getLocation().add(p.getLocation().getDirection().multiply(distance)), org.bukkit.entity.EvokerFangs.class);
        return true;
    }
}
class EvokerVexesAbility extends Ability {
    public EvokerVexesAbility(ItemEditFull pl) { super("evoker_vexes", "Summon Vexes", "Summons assistants."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.getWorld().spawn(p.getLocation(), org.bukkit.entity.Vex.class); return true; }
}
class VindicatorAxeRush extends Ability {
    public VindicatorAxeRush(ItemEditFull pl) { super("vindicator_axe_rush", "Axe Rush", "Axe dash."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "velocity_multiplier", 1.6);
        p.setVelocity(p.getLocation().getDirection().multiply(velocity));
        return true;
    }
}
class VindicatorRage extends Ability {
    public VindicatorRage(ItemEditFull pl) { super("vindicator_rage", "Vindicator Rage", "Attack boost."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int duration = getIntParam(i, "duration", 200);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amp));
        return true;
    }
}
class IllusionerClone extends Ability {
    public IllusionerClone(ItemEditFull pl) { super("illusioner_clone", "Mirror Clone", "Distraction images."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class IllusionerBlindness extends Ability {
    public IllusionerBlindness(ItemEditFull pl) { super("illusioner_blindness", "Blindness Shot", "Blindness arrows."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class WitchPotionThrowAbility extends Ability {
    public WitchPotionThrowAbility(ItemEditFull pl) { super("witch_potion_throw", "Potion Throw", "Throws debuffs."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.launchProjectile(ThrownPotion.class); return true; }
}
class WitchPotionDrink extends Ability {
    public WitchPotionDrink(ItemEditFull pl) { super("witch_potion_drink", "Witch Drink", "Self heal potions."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int duration = getIntParam(i, "duration", 1);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, duration, amp));
        return true;
    }
}
class WitchBrewCauldron extends Ability {
    public WitchBrewCauldron(ItemEditFull pl) { super("witch_brew_cauldron", "Witch Cauldron", "Brew cloud."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class WitchPoisonCloud extends Ability {
    public WitchPoisonCloud(ItemEditFull pl) { super("witch_poison_cloud", "Poison Cloud", "Poison wave."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}

// Subclasses (101-129 Combat Strikes)
class FlameStrike extends Ability {
    private final ItemEditFull plugin;
    public FlameStrike(ItemEditFull pl) { super("flame_strike", "Flame Strike", "Splash fire strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        int steps = getIntParam(i, "steps", 5);
        int fireTicks = getIntParam(i, "fire_ticks", 60);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.2f, 1.0f);
        for (int k = 1; k <= steps; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.FLAME, step, 5, 0.3, 0.3, 0.3, 0.02);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.2, 1.2, 1.2)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.setFireTicks(fireTicks);
                    le.damage(damage, p);
                }
            }
        }
        return true;
    }
}
class TsunamiSlash extends Ability {
    private final ItemEditFull plugin;
    public TsunamiSlash(ItemEditFull pl) { super("tsunami_slash", "Tsunami Slash", "Slash wave."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        int steps = getIntParam(i, "steps", 5);
        double velocity = getDoubleParam(i, "velocity_multiplier", 1.2);
        double pushY = getDoubleParam(i, "push_y", 0.3);
        double damage = getDoubleParam(i, "damage", 3.0);
        p.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.2f, 1.2f);
        for (int k = 1; k <= steps; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.WATER_SPLASH, step, 5, 0.3, 0.3, 0.3, 0.05);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.2, 1.2, 1.2)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.setVelocity(dir.clone().multiply(velocity).setY(pushY));
                    le.damage(damage, p);
                }
            }
        }
        return true;
    }
}
class WindGustSlash extends Ability {
    private final ItemEditFull plugin;
    public WindGustSlash(ItemEditFull pl) { super("wind_gust_slash", "Wind Slash", "Pierces targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        int steps = getIntParam(i, "steps", 6);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        for (int k = 1; k <= steps; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.CLOUD, step, 3, 0.1, 0.1, 0.1, 0.01);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.0, 1.0, 1.0)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(damage, p);
                }
            }
        }
        return true;
    }
}
class VoidSlayer extends Ability {
    private final ItemEditFull plugin;
    public VoidSlayer(ItemEditFull pl) { super("void_slayer", "Void Slayer", "Void damage strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 6);
        double damage = getDoubleParam(i, "damage", 6.0);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        LivingEntity living = (LivingEntity) target;
        living.damage(damage, p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f);
        living.getWorld().spawnParticle(Particle.PORTAL, living.getLocation().add(0, 1.0, 0), 15, 0.2, 0.4, 0.2, 0.05);
        return true;
    }
}
class EarthSmash extends Ability {
    private final ItemEditFull plugin;
    public EarthSmash(ItemEditFull pl) { super("earth_smash", "Earth Smash", "Ground slam."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 4.0);
        double damage = getDoubleParam(i, "damage", 5.0);
        double launchVelocity = getDoubleParam(i, "launch_velocity", 1.0);
        p.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.2f, 0.8f);
        p.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 3.0, 0.2, 3.0, 0.05);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, radius, 2.0, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.damage(damage, p);
                le.setVelocity(new Vector(0, launchVelocity, 0));
            }
        }
        return true;
    }
}
class ThunderClap extends Ability {
    public ThunderClap(ItemEditFull pl) { super("thunder_clap", "Thunder Clap", "Sonic boom."); }
    @Override public boolean trigger(Player p, ItemStack i) { p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1f); return true; }
}
class HolyPurify extends Ability {
    public HolyPurify(ItemEditFull pl) { super("holy_purify", "Holy Purify", "Burns undead."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        int duration = getIntParam(i, "duration", 1);
        int amp = getIntParam(i, "amplifier", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, duration, amp));
        return true;
    }
}
class ShadowStrike extends Ability {
    private final ItemEditFull plugin;
    public ShadowStrike(ItemEditFull pl) { super("shadow_strike", "Shadow Strike", "Teleport behind target."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 12);
        double distance = getDoubleParam(i, "behind_distance", 1.0);
        int duration = getIntParam(i, "duration", 60);
        int amp = getIntParam(i, "amplifier", 1);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        Location tLoc = target.getLocation();
        Vector dir = tLoc.getDirection().normalize();
        Location teleportLoc = tLoc.clone().subtract(dir.clone().multiply(distance));
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 15, 0.2, 0.5, 0.2, 0.02);
        p.teleport(teleportLoc.setDirection(tLoc.getDirection()));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amp));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 15, 0.2, 0.5, 0.2, 0.02);
        return true;
    }
}
class MeteorSlam extends Ability {
    public MeteorSlam(ItemEditFull pl) { super("meteor_slam", "Meteor Slam", "Fire impact slam."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        double velocity = getDoubleParam(i, "downward_velocity", -1.5);
        p.setVelocity(new Vector(0, velocity, 0));
        return true;
    }
}
class LunarCrescent extends Ability {
    private final ItemEditFull plugin;
    public LunarCrescent(ItemEditFull pl) { super("lunar_crescent", "Lunar Crescent", "Crescent wave."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        int steps = getIntParam(i, "steps", 5);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        for (int k = 1; k <= steps; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.SPELL_INSTANT, step, 5, 0.2, 0.2, 0.2, 0.01);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.2, 1.2, 1.2)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(damage, p);
                }
            }
        }
        return true;
    }
}
class SolarFlare extends Ability {
    private final ItemEditFull plugin;
    public SolarFlare(ItemEditFull pl) { super("solar_flare", "Solar Flare", "Ignites targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location base = p.getLocation();
        double radius = getDoubleParam(i, "radius", 4.0);
        int fireTicks = getIntParam(i, "fire_ticks", 80);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(base, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        for (double angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            Location particleLoc = base.clone().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
            particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
        }
        for (Entity ent : p.getWorld().getNearbyEntities(base, radius, 2.0, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.setFireTicks(fireTicks);
                le.damage(damage, p);
            }
        }
        return true;
    }
}
class CosmicRift extends Ability {
    private final ItemEditFull plugin;
    public CosmicRift(ItemEditFull pl) { super("cosmic_rift", "Cosmic Rift", "Pull-in rift."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 15);
        int durationTicks = getIntParam(i, "duration_ticks", 15);
        double pullForce = getDoubleParam(i, "pull_force", 0.5);
        double radius = getDoubleParam(i, "radius", 5.0);
        Block target = p.getTargetBlock(null, range);
        if (target == null || target.getType() == Material.AIR) return false;
        Location center = target.getLocation().add(0.5, 1.0, 0.5);
        p.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.8f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > durationTicks) { cancel(); return; }
                center.getWorld().spawnParticle(Particle.PORTAL, center, 15, 1.0, 0.5, 1.0, 0.05);
                for (Entity ent : center.getWorld().getNearbyEntities(center, radius, 3.0, radius)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        Vector pull = center.toVector().subtract(ent.getLocation().toVector()).normalize().multiply(pullForce);
                        ent.setVelocity(pull);
                    }
                }
            }
        }.runTaskTimer(plugin, center, 0L, 2L);
        return true;
    }
}
class PlagueStrike extends Ability {
    private final ItemEditFull plugin;
    public PlagueStrike(ItemEditFull pl) { super("plague_strike", "Plague Strike", "Poison strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 6);
        double damage = getDoubleParam(i, "damage", 4.0);
        int poisonDuration = getIntParam(i, "poison_duration", 120);
        int poisonAmp = getIntParam(i, "poison_amplifier", 1);
        int witherDuration = getIntParam(i, "wither_duration", 120);
        int witherAmp = getIntParam(i, "wither_amplifier", 0);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        LivingEntity living = (LivingEntity) target;
        living.damage(damage, p);
        living.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmp));
        living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmp));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);
        living.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, living.getLocation().add(0, 1.0, 0), 15, 0.2, 0.4, 0.2, 0.05);
        return true;
    }
}
class FrostGiantSlam extends Ability {
    public FrostGiantSlam(ItemEditFull pl) { super("frost_giant_slam", "Frost Slam", "Slowness slam."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class VampiricEdge extends Ability {
    public VampiricEdge(ItemEditFull pl) { super("vampiric_edge", "Vampiric Edge", "Life leech slash."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class ReaperScythe extends Ability {
    public ReaperScythe(ItemEditFull pl) { super("reaper_scythe", "Reaper Scythe", "Execution strike."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class ExecutionerChop extends Ability {
    public ExecutionerChop(ItemEditFull pl) { super("executioner_chop", "Executioner Chop", "Critical damage."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class GravityPullStrike extends Ability {
    public GravityPullStrike(ItemEditFull pl) { super("gravity_pull_strike", "Gravity Pull", "Pulls targets."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class MagneticDraw extends Ability {
    public MagneticDraw(ItemEditFull pl) { super("magnetic_draw", "Magnetic Draw", "Draws arrows."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class ToxicSlash extends Ability {
    public ToxicSlash(ItemEditFull pl) { super("toxic_slash", "Toxic Slash", "Fatal poison slash."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class WitherCleave extends Ability {
    public WitherCleave(ItemEditFull pl) { super("wither_cleave", "Wither Cleave", "Decay slash."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class PhoenixStrike extends Ability {
    public PhoenixStrike(ItemEditFull pl) { super("phoenix_strike", "Phoenix Strike", "Fire healing."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class GlacierCrash extends Ability {
    private final ItemEditFull plugin;
    public GlacierCrash(ItemEditFull pl) { super("glacier_crash", "Glacier Crash", "Ice crash."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 15);
        double startHeight = getDoubleParam(i, "start_height", 10.0);
        int durationTicks = getIntParam(i, "duration_ticks", 20);
        double damage = getDoubleParam(i, "damage", 6.0);
        int slowDuration = getIntParam(i, "slow_duration", 80);
        int slowAmp = getIntParam(i, "slow_amplifier", 3);
        double radius = getDoubleParam(i, "radius", 3.0);
        Block target = p.getTargetBlock(null, range);
        if (target == null || target.getType() == Material.AIR) return false;
        Location targetLoc = target.getLocation().add(0.5, 1.0, 0.5);
        Location startLoc = targetLoc.clone().add(0, startHeight, 0);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            Location current = startLoc.clone();
            @Override public void run() {
                if (ticks++ > durationTicks || current.distance(targetLoc) < 1.0) {
                    cancel();
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                    targetLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc, 25, 1.5, 0.5, 1.5, 0.05);
                    for (Entity ent : targetLoc.getWorld().getNearbyEntities(targetLoc, radius, 2.0, radius)) {
                        if (ent instanceof LivingEntity && !ent.equals(p)) {
                            LivingEntity le = (LivingEntity) ent;
                            le.damage(damage, p);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmp));
                        }
                    }
                    return;
                }
                current.subtract(0, 0.5, 0);
                current.getWorld().spawnParticle(Particle.SNOWBALL, current, 3, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, targetLoc, 0L, 1L);
        return true;
    }
}
class TempestStrike extends Ability {
    public TempestStrike(ItemEditFull pl) { super("tempest_strike", "Tempest Strike", "Thunder cloud."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class SculkShatter extends Ability {
    private final ItemEditFull plugin;
    public SculkShatter(ItemEditFull pl) { super("sculk_shatter", "Sculk Shatter", "Shatters armor."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        int range = getIntParam(i, "range", 6);
        double baseDamage = getDoubleParam(i, "base_damage", 4.0);
        double heavyDamage = getDoubleParam(i, "heavy_damage", 8.0);
        Entity target = p.getTargetEntity(range);
        if (!(target instanceof LivingEntity)) { p.sendMessage("§cNo target entity in range."); return false; }
        LivingEntity living = (LivingEntity) target;
        double dmg = baseDamage;
        ItemStack helmet = living.getEquipment().getHelmet();
        ItemStack chest = living.getEquipment().getChestplate();
        if ((helmet != null && helmet.getType().name().contains("DIAMOND")) || (chest != null && chest.getType().name().contains("DIAMOND")) ||
            (helmet != null && helmet.getType().name().contains("NETHERITE")) || (chest != null && chest.getType().name().contains("NETHERITE"))) {
            dmg = heavyDamage;
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.5f);
        }
        living.damage(dmg, p);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_BREAK, 1.2f, 0.8f);
        living.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, living.getLocation().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0.05);
        return true;
    }
}
class VolcanicRupture extends Ability {
    public VolcanicRupture(ItemEditFull pl) { super("volcanic_rupture", "Volcanic Rupture", "Magma spewer."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class BreezeBurstStrike extends Ability {
    public BreezeBurstStrike(ItemEditFull pl) { super("breeze_burst_strike", "Breeze Strike", "Wind sweep."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class DragonClawSlash extends Ability {
    public DragonClawSlash(ItemEditFull pl) { super("dragon_claw_slash", "Dragon Claw", "Sweep claw slash."); }
    @Override public boolean trigger(Player p, ItemStack i) { return true; }
}
class AbyssalDrownStrike extends Ability {
    private final ItemEditFull plugin;
    public AbyssalDrownStrike(ItemEditFull pl) { super("abyssal_drown_strike", "Abyssal Drown", "Fatigues targets."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 5.0);
        double height = getDoubleParam(i, "height", 2.5);
        int air = getIntParam(i, "remaining_air", 0);
        int slowDuration = getIntParam(i, "slow_duration", 120);
        int slowAmp = getIntParam(i, "slow_amplifier", 2);
        int fatigueDuration = getIntParam(i, "fatigue_duration", 120);
        int fatigueAmp = getIntParam(i, "fatigue_amplifier", 2);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 1.2f, 0.8f);
        p.getWorld().spawnParticle(Particle.WATER_BUBBLE, loc, 50, radius, 1.5, radius, 0.1);
        for (Entity ent : p.getWorld().getNearbyEntities(loc, radius, height, radius)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.setRemainingAir(air);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmp));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, fatigueDuration, fatigueAmp));
                le.damage(damage, p);
                NewExpansionAbilities.playSoundSafe(le.getLocation(), "ENTITY_GENERIC_DROWN", Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            }
        }
        return true;
    }
}

// JJK Custom Ability Classes (50)
class DivergentFist extends Ability {
    private final ItemEditFull plugin;
    public DivergentFist(ItemEditFull pl) { super("divergent_fist", "Divergent Fist", "Delayed impact strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(5);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location targetLoc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        double dmg1 = getDoubleParam(i, "damage_initial", 4.0);
        double dmg2 = getDoubleParam(i, "damage_delayed", 8.0);
        if (le != null) {
            le.damage(dmg1, p);
        }
        NewExpansionAbilities.spawnDust(targetLoc, Color.fromRGB(0, 191, 255), 1.5f, 25, 0.3, 0.3, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        new CompatRunnable() {
            @Override public void run() {
                if (le != null) {
                    if (le.isValid() && !le.isDead()) {
                        le.damage(dmg2, p);
                        Location currentLoc = le.getLocation().add(0, 1, 0);
                        NewExpansionAbilities.spawnDust(currentLoc, Color.fromRGB(255, 69, 0), 2.2f, 35, 0.4, 0.4, 0.4);
                        le.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, currentLoc, 3, 0.1, 0.1, 0.1, 0.1);
                        p.getWorld().playSound(currentLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 1.2f);
                    }
                } else {
                    NewExpansionAbilities.spawnDust(targetLoc, Color.fromRGB(255, 69, 0), 2.2f, 35, 0.4, 0.4, 0.4);
                    targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetLoc, 3, 0.1, 0.1, 0.1, 0.1);
                    p.getWorld().playSound(targetLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 1.2f);
                }
            }
        }.runTaskLater(plugin, p, 8L);
        return true;
    }
}
class BlackFlash extends Ability {
    public BlackFlash(ItemEditFull pl) { super("black_flash", "Black Flash", "Critical spark strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(5);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        double dmg = getDoubleParam(i, "damage", 16.0);
        if (le != null) {
            le.damage(dmg, p);
        }
        for (int k = 0; k < 15; k++) {
            NewExpansionAbilities.spawnDust(loc, Color.fromRGB(0, 0, 0), 2.0f, 2, 0.3, 0.3, 0.3);
            NewExpansionAbilities.spawnDust(loc, Color.fromRGB(255, 0, 0), 1.8f, 2, 0.3, 0.3, 0.3);
        }
        loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc, 25, 0.5, 0.5, 0.5, 0.15);
        try {
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0.0, 0.0, 0.0, 0.0, Color.WHITE);
        } catch (Exception e) {
            try {
                loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0.0, 0.0, 0.0, 0.0);
            } catch (Exception ex) {
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 1, 0.0, 0.0, 0.0, 0.1);
            }
        }
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.4f);
        if (le != null) {
            le.setVelocity(p.getLocation().getDirection().normalize().multiply(1.8).setY(0.4));
        }
        return true;
    }
}
class SukunaCleave extends Ability {
    public SukunaCleave(ItemEditFull pl) { super("cleave", "Cleave", "Adaptive slash strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(6);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        if (le != null) {
            double percent = getDoubleParam(i, "max_health_percent", 0.20);
            double damage = le.getMaxHealth() * percent + 4.0;
            le.damage(damage, p);
        }
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 8, 0.3, 0.3, 0.3, 0.1);
        NewExpansionAbilities.spawnDust(loc, Color.fromRGB(139, 0, 0), 1.5f, 20, 0.4, 0.4, 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
        return true;
    }
}
class SukunaDismantle extends Ability {
    private final ItemEditFull plugin;
    public SukunaDismantle(ItemEditFull pl) { super("dismantle", "Dismantle", "Flying slash projectile."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double damage = getDoubleParam(i, "damage", 6.0);
        double range = getDoubleParam(i, "range", 15.0);
        p.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        new CompatRunnable() {
            int step = 0;
            Location current = origin.clone();
            @Override public void run() {
                if (step++ > range || !current.getBlock().getType().isAir()) { cancel(); return; }
                current.add(dir.clone().multiply(1.0));
                current.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 1, 0, 0, 0, 0);
                NewExpansionAbilities.spawnDust(current, Color.fromRGB(245, 245, 245), 1.2f, 6, 0.1, 0.1, 0.1);
                for (Entity ent : current.getWorld().getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        ((LivingEntity) ent).damage(damage, p);
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class SukunaFireArrow extends Ability {
    private final ItemEditFull plugin;
    public SukunaFireArrow(ItemEditFull pl) { super("fuga_fire_arrow", "Fuga Fire Arrow", "Blazing explosive arrow."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double speed = getDoubleParam(i, "speed", 1.5);
        double yield = getDoubleParam(i, "yield", 4.0);
        p.getWorld().playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        NewExpansionAbilities.drawHelix(p.getLocation(), Color.fromRGB(255, 69, 0), 1.5, 2.0, 15);
        new CompatRunnable() {
            int step = 0;
            Location current = origin.clone();
            @Override public void run() {
                if (step++ > 25 || !current.getBlock().getType().isAir()) {
                    current.getWorld().createExplosion(current, (float) yield, true, true);
                    cancel();
                    return;
                }
                current.add(dir.clone().multiply(speed));
                current.getWorld().spawnParticle(Particle.FLAME, current, 8, 0.1, 0.1, 0.1, 0.02);
                current.getWorld().spawnParticle(Particle.LAVA, current, 3, 0.1, 0.1, 0.1, 0.02);
                NewExpansionAbilities.spawnDust(current, Color.fromRGB(255, 140, 0), 1.5f, 5, 0.1, 0.1, 0.1);
                for (Entity ent : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        current.getWorld().createExplosion(current, (float) yield, true, true);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class MalevolentShrine extends Ability {
    private final ItemEditFull plugin;
    public MalevolentShrine(ItemEditFull pl) { super("malevolent_shrine", "Malevolent Shrine", "Rapid slash storm."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 12.0);
        double damage = getDoubleParam(i, "damage", 2.0);
        int durationTicks = getIntParam(i, "duration_ticks", 120);
        p.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > durationTicks || !p.isOnline()) { cancel(); return; }
                Location center = p.getLocation();
                NewExpansionAbilities.drawRing(center, Color.fromRGB(139, 0, 0), radius, 30);
                NewExpansionAbilities.drawRing(center, Color.fromRGB(0, 0, 0), radius - 0.5, 30);
                if (ticks % 5 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                    for (Entity ent : center.getWorld().getNearbyEntities(center, radius, 4.0, radius)) {
                        if (ent instanceof LivingEntity && !ent.equals(p)) {
                            LivingEntity le = (LivingEntity) ent;
                            le.damage(damage, p);
                            le.getWorld().spawnParticle(Particle.SWEEP_ATTACK, le.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.05);
                            NewExpansionAbilities.spawnDust(le.getLocation().add(0, 1, 0), Color.fromRGB(255, 0, 0), 1.5f, 12, 0.3, 0.3, 0.3);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class GojoInfinity extends Ability {
    private final ItemEditFull plugin;
    public GojoInfinity(ItemEditFull pl) { super("infinity", "Infinity Barrier", "Repels attacks."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        double durationSec = getDoubleParam(i, "duration", 8.0);
        double radius = getDoubleParam(i, "radius", 3.0);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > (durationSec * 20) || !p.isOnline()) { cancel(); return; }
                Location loc = p.getLocation();
                if (ticks % 5 == 0) {
                    NewExpansionAbilities.drawSphere(loc.add(0, 1, 0), Color.fromRGB(224, 255, 255), radius, 6);
                }
                for (Entity ent : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (ent instanceof Projectile) {
                        Vector push = ent.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2);
                        ent.setVelocity(push);
                        loc.getWorld().spawnParticle(Particle.PORTAL, ent.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                    } else if (ent instanceof LivingEntity && !ent.equals(p)) {
                        Vector push = ent.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5).setY(0.1);
                        ent.setVelocity(push);
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class LapseBlue extends Ability {
    private final ItemEditFull plugin;
    public LapseBlue(ItemEditFull pl) { super("lapse_blue", "Lapse: Blue", "Vortex pull."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Block target = p.getTargetBlock(null, 15);
        Location center;
        if (target != null && target.getType() != Material.AIR) {
            center = target.getLocation().add(0.5, 1.0, 0.5);
        } else {
            center = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(8));
        }
        double radius = getDoubleParam(i, "radius", 6.0);
        double pullForce = getDoubleParam(i, "pull_force", 0.6);
        double damage = getDoubleParam(i, "damage", 4.0);
        p.getWorld().playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 1.2f, 1.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 30) { cancel(); return; }
                NewExpansionAbilities.drawRing(center, Color.fromRGB(0, 191, 255), radius, 20);
                center.getWorld().spawnParticle(Particle.PORTAL, center, 15, 1.0, 1.0, 1.0, 0.1);
                for (Entity ent : center.getWorld().getNearbyEntities(center, radius, 3.0, radius)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        Vector pull = center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(pullForce);
                        le.setVelocity(pull);
                        if (ticks % 10 == 0) le.damage(damage, p);
                    }
                }
            }
        }.runTaskTimer(plugin, center, 0L, 2L);
        return true;
    }
}
class ReversalRed extends Ability {
    public ReversalRed(ItemEditFull pl) { super("reversal_red", "Reversal: Red", "Repulsion blast."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double range = getDoubleParam(i, "range", 8.0);
        double damage = getDoubleParam(i, "damage", 8.0);
        double push = getDoubleParam(i, "push_multiplier", 1.8);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
        for (int k = 1; k <= range; k++) {
            Location step = origin.clone().add(dir.clone().multiply(k));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(255, 0, 50), 1.8f, 15, 0.3, 0.3, 0.3);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.8, 1.8, 1.8)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.damage(damage, p);
                    le.setVelocity(dir.clone().multiply(push).setY(0.4));
                }
            }
        }
        return true;
    }
}
class HollowPurple extends Ability {
    private final ItemEditFull plugin;
    public HollowPurple(ItemEditFull pl) { super("hollow_purple", "Hollow: Purple", "Destructive orb."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double damage = getDoubleParam(i, "damage", 25.0);
        int maxSteps = getIntParam(i, "max_steps", 25);
        p.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.0f);
        new CompatRunnable() {
            int step = 0;
            Location current = origin.clone();
            @Override public void run() {
                if (step++ > maxSteps) { cancel(); return; }
                current.add(dir.clone().multiply(0.8));
                NewExpansionAbilities.drawSphere(current, Color.fromRGB(186, 85, 211), 1.5, 6);
                current.getWorld().spawnParticle(Particle.SPELL_WITCH, current, 15, 0.5, 0.5, 0.5, 0.05);
                current.getWorld().spawnParticle(Particle.PORTAL, current, 10, 0.4, 0.4, 0.4, 0.05);
                current.getWorld().playSound(current, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.5f);
                for (Entity ent : current.getWorld().getNearbyEntities(current, 2.5, 2.5, 2.5)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        ((LivingEntity) ent).damage(damage, p);
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
      }
}
class UnlimitedVoid extends Ability {
    private final ItemEditFull plugin;
    public UnlimitedVoid(ItemEditFull pl) { super("unlimited_void", "Unlimited Void", "Sensory overload."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 10.0);
        double durationSec = getDoubleParam(i, "duration", 6.0);
        p.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > (durationSec * 20) || !p.isOnline()) { cancel(); return; }
                Location center = p.getLocation();
                NewExpansionAbilities.drawRing(center, Color.fromRGB(15, 10, 45), radius, 40);
                NewExpansionAbilities.drawRing(center, Color.fromRGB(75, 0, 130), radius - 0.5, 30);
                center.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, center, 15, radius, 2.0, radius, 0.01);
                for (Entity ent : center.getWorld().getNearbyEntities(center, radius, 4.0, radius)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 9));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class DivineDog extends Ability {
    public DivineDog(ItemEditFull pl) { super("divine_dog", "Divine Dog", "Summons shikigami wolf."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Wolf wolf = p.getWorld().spawn(p.getLocation(), Wolf.class);
        wolf.setOwner(p);
        wolf.setCustomName("§7Divine Dog");
        wolf.setCustomNameVisible(true);
        NewExpansionAbilities.drawRing(wolf.getLocation(), Color.fromRGB(20, 20, 20), 1.2, 15);
        wolf.getWorld().spawnParticle(Particle.TOWN_AURA, wolf.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
        return true;
    }
}
class NueLightning extends Ability {
    public NueLightning(ItemEditFull pl) { super("nue_lightning", "Nue Lightning", "Summons thunder bird."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Block b = p.getTargetBlock(null, 15);
        Location loc;
        if (b != null && b.getType() != Material.AIR) {
            loc = b.getLocation().add(0.5, 1.0, 0.5);
        } else {
            loc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(8));
        }
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(255, 215, 0), 2.5, 20);
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 3, 0), 20, 1.0, 0.5, 1.0, 0.05);
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                ((LivingEntity) ent).damage(6.0, p);
            }
        }
        return true;
    }
}
class ToadTongue extends Ability {
    public ToadTongue(ItemEditFull pl) { super("toad_tongue", "Toad Tongue", "Pulls target with tongue."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(12);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location pLoc = p.getLocation();
        Location tLoc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(8));
        double distance = pLoc.distance(tLoc);
        Vector dir = pLoc.toVector().subtract(tLoc.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.5) {
            Location point = tLoc.clone().add(dir.clone().multiply(d));
            NewExpansionAbilities.spawnDust(point, Color.fromRGB(255, 182, 193), 1.5f, 3, 0.05, 0.05, 0.05);
        }
        if (le != null) {
            le.setVelocity(dir.multiply(1.5).setY(0.3));
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SLIME_ATTACK, 1.0f, 1.2f);
        return true;
    }
}
class MaxElephant extends Ability {
    public MaxElephant(ItemEditFull pl) { super("max_elephant", "Max Elephant", "Water torrent blast."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        double damage = getDoubleParam(i, "damage", 5.0);
        p.getWorld().playSound(loc, Sound.ENTITY_DONKEY_ANGRY, 1.2f, 0.8f);
        for (int k = 1; k <= 6; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.WATER_SPLASH, step, 15, 0.8, 0.5, 0.8, 0.05);
            step.getWorld().spawnParticle(Particle.WATER_DROP, step, 10, 0.8, 0.5, 0.8, 0.05);
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(0, 191, 255), 1.2f, 8, 0.4, 0.4, 0.4);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.8, 1.8, 1.8)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.damage(damage, p);
                    le.setVelocity(dir.clone().multiply(1.2).setY(0.2));
                }
            }
        }
        return true;
    }
}
class RabbitEscape extends Ability {
    public RabbitEscape(ItemEditFull pl) { super("rabbit_escape", "Rabbit Escape", "Summons distraction swarm."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        for (int k = 0; k < 8; k++) {
            Location offsetLoc = loc.clone().add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
            Rabbit r = p.getWorld().spawn(offsetLoc, Rabbit.class);
            r.setRabbitType(Rabbit.Type.WHITE);
            NewExpansionAbilities.spawnDust(offsetLoc, Color.fromRGB(240, 240, 240), 1.0f, 5, 0.1, 0.1, 0.1);
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
        p.getWorld().playSound(loc, Sound.ENTITY_RABBIT_AMBIENT, 1.0f, 1.2f);
        return true;
    }
}
class MahoragaSummon extends Ability {
    private final ItemEditFull plugin;
    public MahoragaSummon(ItemEditFull pl) { super("mahoraga_summon", "Mahoraga", "Summons general shikigami."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        IronGolem golem = p.getWorld().spawn(p.getLocation(), IronGolem.class);
        golem.setCustomName("§cMahoraga");
        golem.setCustomNameVisible(true);
        NewExpansionAbilities.drawRing(golem.getLocation(), Color.fromRGB(220, 20, 60), 2.0, 25);
        golem.getWorld().spawnParticle(Particle.PORTAL, golem.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
        golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.7f);
        new CompatRunnable() {
            @Override public void run() {
                if (golem.isValid()) {
                    golem.getWorld().spawnParticle(Particle.CLOUD, golem.getLocation(), 15, 0.5, 1.0, 0.5, 0.05);
                    golem.remove();
                }
            }
        }.runTaskLater(plugin, p, 300L); // 15 seconds
        return true;
    }
}
class ChimeraShadowGarden extends Ability {
    private final ItemEditFull plugin;
    public ChimeraShadowGarden(ItemEditFull pl) { super("chimera_shadow_garden", "Chimera Shadow Garden", "Traps targets in shadow."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double radius = getDoubleParam(i, "radius", 8.0);
        int durationTicks = getIntParam(i, "duration_ticks", 120);
        p.getWorld().playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.2f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > durationTicks || !p.isOnline()) { cancel(); return; }
                Location center = p.getLocation();
                NewExpansionAbilities.drawRing(center, Color.fromRGB(10, 10, 15), radius, 30);
                NewExpansionAbilities.drawRing(center, Color.fromRGB(25, 25, 30), radius - 2.0, 20);
                center.getWorld().spawnParticle(Particle.SQUID_INK, center, 15, radius, 0.2, radius, 0.02);
                for (Entity ent : center.getWorld().getNearbyEntities(center, radius, 2.0, radius)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        LivingEntity le = (LivingEntity) ent;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 3));
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
    }
}
class StrawDollResonance extends Ability {
    public StrawDollResonance(ItemEditFull pl) { super("resonance", "Resonance", "Soul-link strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(10);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        double damage = getDoubleParam(i, "damage", 8.0);
        if (le != null) {
            le.damage(damage, p);
        }
        Location pLoc = p.getEyeLocation();
        Location tLoc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(6));
        double dist = pLoc.distance(tLoc);
        Vector dir = tLoc.toVector().subtract(pLoc.toVector()).normalize();
        for (double d = 0; d < dist; d += 0.4) {
            NewExpansionAbilities.spawnDust(pLoc.clone().add(dir.clone().multiply(d)), Color.fromRGB(220, 20, 60), 1.2f, 2, 0.05, 0.05, 0.05);
        }
        Location loc = (le != null) ? le.getLocation().add(0, 1, 0) : tLoc;
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.2, 0.4, 0.2, 0.05);
        loc.getWorld().spawnParticle(Particle.HEART, loc, 5, 0.2, 0.4, 0.2, 0.01);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.5f);
        return true;
    }
}
class StrawDollHairpin extends Ability {
    private final ItemEditFull plugin;
    public StrawDollHairpin(ItemEditFull pl) { super("hairpin", "Hairpin", "Shrapnel nail explosion."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(10);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(6));
        double damage = getDoubleParam(i, "damage", 6.0);
        NewExpansionAbilities.drawHelix(loc, Color.fromRGB(255, 69, 0), 1.0, 2.0, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);
        new CompatRunnable() {
            @Override public void run() {
                if (le != null) {
                    if (le.isValid()) {
                        le.damage(damage, p);
                        Location currentLoc = le.getLocation().add(0, 1, 0);
                        currentLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, currentLoc, 15, 0.3, 0.3, 0.3, 0.05);
                        NewExpansionAbilities.spawnDust(currentLoc, Color.fromRGB(255, 140, 0), 1.8f, 20, 0.5, 0.5, 0.5);
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                    }
                } else {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0.3, 0.3, 0.3, 0.05);
                    NewExpansionAbilities.spawnDust(loc, Color.fromRGB(255, 140, 0), 1.8f, 20, 0.5, 0.5, 0.5);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                }
            }
        }.runTaskLater(plugin, p, 15L);
        return true;
    }
}
class RatioStrike extends Ability {
    public RatioStrike(ItemEditFull pl) { super("ratio_strike", "Ratio Strike", "Precise critical strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(5);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        double damage = getDoubleParam(i, "damage", 6.0);
        Location loc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        if (Math.random() < 0.3) {
            damage *= 2.5;
            p.sendMessage("§e[7:3] Precise hit! Critical Damage!");
            loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc, 25, 0.2, 0.3, 0.2, 0.1);
            for (double offset = -1.0; offset <= 1.0; offset += 0.1) {
                Location lineLoc = loc.clone().add(offset, offset * 0.7, 0);
                NewExpansionAbilities.spawnDust(lineLoc, Color.fromRGB(255, 215, 0), 1.8f, 2, 0, 0, 0);
            }
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.4f);
        } else {
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.05);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
        }
        if (le != null) {
            le.damage(damage, p);
        }
        return true;
    }
}
class CollapseStrike extends Ability {
    public CollapseStrike(ItemEditFull pl) { super("collapse_strike", "Collapse", "Ground crushing strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        double damage = getDoubleParam(i, "damage", 5.0);
        p.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.2f, 0.5f);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 15, 2.0, 0.2, 2.0, 0.05);
        NewExpansionAbilities.spawnDust(loc, Color.fromRGB(139, 69, 19), 1.5f, 25, 1.5, 0.2, 1.5);
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 3.0, 1.5, 3.0)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.damage(damage, p);
                le.setVelocity(le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.0).setY(0.35));
            }
        }
        return true;
    }
}
class BoogieWoogie extends Ability {
    public BoogieWoogie(ItemEditFull pl) { super("boogie_woogie", "Boogie Woogie", "Location swap clap."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(15);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location pLoc = p.getLocation();
        if (le != null) {
            Location tLoc = le.getLocation();
            p.getWorld().playSound(pLoc, Sound.ENTITY_CHICKEN_EGG, 1.5f, 1.5f);
            NewExpansionAbilities.drawRing(pLoc, Color.fromRGB(255, 215, 0), 1.2, 12);
            NewExpansionAbilities.drawRing(tLoc, Color.fromRGB(255, 215, 0), 1.2, 12);
            p.getWorld().spawnParticle(Particle.CLOUD, pLoc, 10, 0.3, 0.5, 0.3, 0.05);
            p.getWorld().spawnParticle(Particle.CLOUD, tLoc, 10, 0.3, 0.5, 0.3, 0.05);
            p.teleport(tLoc);
            le.teleport(pLoc);
        } else {
            // Blink forward
            Location targetLoc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(8));
            Location groundLoc = targetLoc;
            for (int d = 0; d < 8; d++) {
                if (targetLoc.clone().add(0, -d, 0).getBlock().getType().isSolid()) {
                    groundLoc = targetLoc.clone().add(0, -d + 1, 0);
                    break;
                }
            }
            p.getWorld().playSound(pLoc, Sound.ENTITY_CHICKEN_EGG, 1.5f, 1.5f);
            NewExpansionAbilities.drawRing(pLoc, Color.fromRGB(255, 215, 0), 1.2, 12);
            NewExpansionAbilities.drawRing(groundLoc, Color.fromRGB(255, 215, 0), 1.2, 12);
            p.getWorld().spawnParticle(Particle.CLOUD, pLoc, 10, 0.3, 0.5, 0.3, 0.05);
            p.getWorld().spawnParticle(Particle.CLOUD, groundLoc, 10, 0.3, 0.5, 0.3, 0.05);
            p.teleport(groundLoc);
        }
        return true;
    }
}
class EmberInsect extends Ability {
    private final ItemEditFull plugin;
    public EmberInsect(ItemEditFull pl) { super("ember_insect", "Ember Insect", "Explosive flame bugs."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        p.getWorld().playSound(loc, Sound.ENTITY_BEE_LOOP, 1.0f, 1.4f);
        for (int k = 0; k < 3; k++) {
            Bat bat = p.getWorld().spawn(loc.clone().add(dir.clone().multiply(1.0)), Bat.class);
            bat.setCustomName("§cEmber Insect");
            new CompatRunnable() {
                int ticks = 0;
                @Override public void run() {
                    if (ticks++ > 60 || !bat.isValid()) {
                        if (bat.isValid()) {
                            bat.getWorld().createExplosion(bat.getLocation(), 2.0f, false, false);
                            bat.remove();
                        }
                        cancel();
                        return;
                    }
                    NewExpansionAbilities.spawnDust(bat.getLocation(), Color.fromRGB(255, 165, 0), 1.2f, 3, 0.1, 0.1, 0.1);
                    bat.getWorld().spawnParticle(Particle.FLAME, bat.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                    for (Entity ent : bat.getWorld().getNearbyEntities(bat.getLocation(), 1.2, 1.2, 1.2)) {
                        if (ent instanceof LivingEntity && !ent.equals(p) && !(ent instanceof Bat)) {
                            bat.getWorld().createExplosion(bat.getLocation(), 2.0f, false, false);
                            bat.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, p, 0L, 2L);
        }
        return true;
    }
}
class MaximumMeteor extends Ability {
    private final ItemEditFull plugin;
    public MaximumMeteor(ItemEditFull pl) { super("maximum_meteor", "Maximum Meteor", "Summons giant meteor."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Block b = p.getTargetBlock(null, 15);
        if (b == null || b.getType() == Material.AIR) return false;
        Location targetLoc = b.getLocation().add(0.5, 1.0, 0.5);
        Location startLoc = targetLoc.clone().add(0, 10, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
        new CompatRunnable() {
            int step = 0;
            Location current = startLoc.clone();
            @Override public void run() {
                if (step++ > 20 || current.distance(targetLoc) < 1.0) {
                    cancel();
                    targetLoc.getWorld().createExplosion(targetLoc, 5.0f, true, true);
                    return;
                }
                current.subtract(0, 0.5, 0);
                NewExpansionAbilities.drawSphere(current, Color.fromRGB(255, 69, 0), 2.0, 6);
                current.getWorld().spawnParticle(Particle.FLAME, current, 15, 0.5, 0.5, 0.5, 0.05);
                current.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.3, 0.3, 0.3, 0.05);
            }
        }.runTaskTimer(plugin, targetLoc, 0L, 1L);
        return true;
    }
}
class FlowerField extends Ability {
    public FlowerField(ItemEditFull pl) { super("flower_field", "Flower Field", "Pacifying flower field."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 25, 4.0, 1.0, 4.0, 0.05);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(50, 205, 50), 4.0, 20);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(255, 105, 180), 3.0, 15);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1));
        p.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.0f, 1.2f);
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
            if (ent instanceof LivingEntity && !ent.equals(p)) {
                LivingEntity le = (LivingEntity) ent;
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
            }
        }
        return true;
    }
}
class WoodenRoots extends Ability {
    private final ItemEditFull plugin;
    public WoodenRoots(ItemEditFull pl) { super("wooden_roots", "Wooden Roots", "Root entrapment."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(8);
        LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location center;
        if (le != null) {
            center = le.getLocation();
        } else {
            Block b = p.getTargetBlockExact(8);
            if (b != null) {
                center = b.getLocation().add(0.5, 1, 0.5);
            } else {
                center = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(5));
            }
        }
        p.getWorld().playSound(center, Sound.BLOCK_WOOD_BREAK, 1.2f, 0.8f);
        NewExpansionAbilities.drawRing(center, Color.fromRGB(139, 69, 19), 1.2, 12);
        final List<Block> rootBlocks = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = center.clone().add(x, 0, z).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.OAK_LEAVES);
                    rootBlocks.add(b);
                }
            }
        }
        new CompatRunnable() {
            @Override public void run() {
                for (Block b : rootBlocks) {
                    if (b.getType() == Material.OAK_LEAVES) b.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, center, 80L); // 4 seconds
        return true;
    }
}
class CursedBud extends Ability {
    private final ItemEditFull plugin;
    public CursedBud(ItemEditFull pl) { super("cursed_bud", "Cursed Bud", "Energy-draining bud."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(8);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(5));
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 15, 0.3, 0.5, 0.3, 0.05);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(0, 100, 80), 1.0, 15);
        loc.getWorld().playSound(loc, Sound.BLOCK_CHERRY_SAPLING_PLACE, 1.2f, 0.8f);
        if (le != null) {
            new CompatRunnable() {
                int ticks = 0;
                Location lastLoc = le.getLocation();
                @Override public void run() {
                    if (ticks++ > 80 || !le.isValid()) { cancel(); return; }
                    if (le.getLocation().distanceSquared(lastLoc) > 0.05) {
                        le.damage(2.0, p);
                        NewExpansionAbilities.spawnDust(le.getLocation().add(0, 1, 0), Color.fromRGB(34, 139, 34), 1.2f, 6, 0.1, 0.1, 0.1);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.02);
                    }
                    lastLoc = le.getLocation();
                }
            }.runTaskTimer(plugin, le, 0L, 2L);
        }
        return true;
    }
}
class WaterTorrent extends Ability {
    public WaterTorrent(ItemEditFull pl) { super("water_torrent", "Water Torrent", "Drowning wave push."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        p.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.6f);
        for (int k = 1; k <= 7; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            step.getWorld().spawnParticle(Particle.WATER_SPLASH, step, 12, 0.6, 0.5, 0.6, 0.05);
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(30, 144, 255), 1.4f, 10, 0.4, 0.4, 0.4);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.5, 1.5, 1.5)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.damage(4.0, p);
                    le.setVelocity(dir.clone().multiply(1.5).setY(0.2));
                }
            }
        }
        return true;
    }
}

class BloodPierce extends Ability {
    public BloodPierce(ItemEditFull pl) { super("blood_pierce", "Blood Pierce", "Piercing laser stream."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        double damage = getDoubleParam(i, "damage", 8.0);
        p.getWorld().playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
        for (double d = 1.0; d <= 15.0; d += 0.5) {
            Location step = origin.clone().add(dir.clone().multiply(d));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(200, 0, 0), 1.2f, 6, 0.05, 0.05, 0.05);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 0.8, 0.8, 0.8)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(damage, p);
                }
            }
        }
        return true;
    }
}
class BloodSupernova extends Ability {
    private final ItemEditFull plugin;
    public BloodSupernova(ItemEditFull pl) { super("supernova", "Supernova", "Blood burst bomb."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location targetLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(4));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 0.5f);
        NewExpansionAbilities.drawSphere(targetLoc, Color.fromRGB(150, 0, 0), 1.2, 6);
        new CompatRunnable() {
            @Override public void run() {
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);
                NewExpansionAbilities.spawnDust(targetLoc, Color.fromRGB(200, 10, 10), 1.8f, 30, 1.5, 1.5, 1.5);
                for (Entity ent : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.5, 3.5, 3.5)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        ((LivingEntity) ent).damage(7.0, p);
                    }
                }
            }
        }.runTaskLater(plugin, targetLoc, 15L);
        return true;
    }
}
class SlicingExorcism extends Ability {
    private final ItemEditFull plugin;
    public SlicingExorcism(ItemEditFull pl) { super("slicing_exorcism", "Slicing Exorcism", "Blood blades strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 0.5f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 80 || !p.isOnline()) { cancel(); return; }
                Location loc = p.getLocation().add(0, 0.8, 0);
                double angle = ticks * 0.4;
                Location pLoc1 = loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                Location pLoc2 = loc.clone().add(Math.cos(angle + Math.PI) * 1.5, 0, Math.sin(angle + Math.PI) * 1.5);
                NewExpansionAbilities.spawnDust(pLoc1, Color.fromRGB(200, 10, 10), 1.2f, 3, 0.05, 0.05, 0.05);
                NewExpansionAbilities.spawnDust(pLoc2, Color.fromRGB(200, 10, 10), 1.2f, 3, 0.05, 0.05, 0.05);
                for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.8, 1.2, 1.8)) {
                    if (ent instanceof LivingEntity && !ent.equals(p)) {
                        ((LivingEntity) ent).damage(1.5, p);
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class IdleTransfiguration extends Ability {
    public IdleTransfiguration(ItemEditFull pl) { super("idle_transfiguration", "Idle Transfiguration", "Soul alteration."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(6);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        NewExpansionAbilities.drawHelix(loc, Color.fromRGB(148, 0, 211), 1.2, 2.0, 12);
        loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.4, 0.8, 0.4, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
        if (le != null) {
            le.damage(10.0, p);
        }
        return true;
    }
}
class SoulMultiplicity extends Ability {
    private final ItemEditFull plugin;
    public SoulMultiplicity(ItemEditFull pl) { super("soul_multiplicity", "Soul Multiplicity", "Fleshy summon strike."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 1.2f, 0.5f);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(128, 0, 128), 2.0, 15);
        for (int k = 0; k < 2; k++) {
            Zombie z = p.getWorld().spawn(loc.clone().add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3), Zombie.class);
            z.setCustomName("§7Transfigured Soul");
            z.setCustomNameVisible(true);
            z.setBaby(true);
            new CompatRunnable() {
                @Override public void run() {
                    if (z.isValid()) z.remove();
                }
            }.runTaskLater(plugin, p, 160L);
        }
        return true;
    }
}
class BodyRepel extends Ability {
    public BodyRepel(ItemEditFull pl) { super("body_repel", "Body Repel", "Flesh spike stream."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        p.getWorld().playSound(loc, Sound.ENTITY_LLAMA_SPIT, 1.2f, 0.5f);
        for (double d = 1.0; d <= 8.0; d += 0.5) {
            Location step = loc.clone().add(dir.clone().multiply(d));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(128, 128, 128), 1.2f, 6, 0.1, 0.1, 0.1);
            step.getWorld().spawnParticle(Particle.CRIT, step, 8, 0.2, 0.2, 0.2, 0.05);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.0, 1.0, 1.0)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(5.0, p);
                }
            }
        }
        return true;
    }
}
class SelfEmbodiment extends Ability {
    private final ItemEditFull plugin;
    public SelfEmbodiment(ItemEditFull pl) { super("self_embodiment", "Self-Embodiment of Perfection", "Soul transfiguration trap."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(8);
        LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc;
        if (le != null) {
            loc = le.getLocation();
        } else {
            Block b = p.getTargetBlockExact(8);
            if (b != null) {
                loc = b.getLocation().add(0.5, 1, 0.5);
            } else {
                loc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(5));
            }
        }
        p.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(30, 30, 30), 2.5, 20);
        final List<Block> blocks = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 2; y++) {
                    if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                        Block b = loc.clone().add(x, y, z).getBlock();
                        if (b.getType() == Material.AIR) {
                            b.setType(Material.NETHER_BRICKS);
                            blocks.add(b);
                        }
                    }
                }
            }
        }
        new CompatRunnable() {
            @Override public void run() {
                for (Block b : blocks) {
                    if (b.getType() == Material.NETHER_BRICKS) b.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, loc, 100L); // 5 seconds
        return true;
    }
}
class SpeechStop extends Ability {
    public SpeechStop(ItemEditFull pl) { super("speech_stop", "Cursed Speech: Stop", "Freezes targets."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(12);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        p.damage(2.0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
        Location loc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(6));
        if (le != null) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 9));
        }
        NewExpansionAbilities.drawHelix(loc, Color.fromRGB(240, 255, 255), 1.2, 2.0, 10);
        loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc.add(0, 1, 0), 15, 0.2, 0.4, 0.2, 0.02);
        p.sendMessage("§bCursed Speech: \"Don't Move!\"");
        return true;
    }
}
class SpeechExplode extends Ability {
    public SpeechExplode(ItemEditFull pl) { super("speech_explode", "Cursed Speech: Explode", "Target detonation."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(12);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        p.damage(4.0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
        Location loc = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(6));
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(255, 0, 0), 1.5, 15);
        loc.getWorld().createExplosion(loc, 3.0f, false, false);
        p.sendMessage("§bCursed Speech: \"Explode!\"");
        return true;
    }
}
class SpeechBlastAway extends Ability {
    public SpeechBlastAway(ItemEditFull pl) { super("speech_blast_away", "Cursed Speech: Blast Away", "Extreme push."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(12);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        p.damage(3.0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        Vector push = p.getLocation().getDirection().normalize().multiply(2.2).setY(0.4);
        Location center = (le != null) ? le.getLocation() : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(6));
        if (le != null) {
            le.setVelocity(push);
        }
        for (int k = 0; k < 12; k++) {
            NewExpansionAbilities.spawnDust(center, Color.fromRGB(220, 220, 220), 1.5f, 2, 0.5, 0.5, 0.5);
        }
        center.getWorld().spawnParticle(Particle.CLOUD, center, 15, 0.4, 0.4, 0.4, 0.1);
        p.sendMessage("§bCursed Speech: \"Blast Away!\"");
        return true;
    }
}
class PlayfulCloud extends Ability {
    public PlayfulCloud(ItemEditFull pl) { super("playful_cloud", "Playful Cloud", "Physical scaling strike."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(5);
        final LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        double dmg = 5.0 + (p.getMaxHealth() - p.getHealth()) * 0.5;
        Location loc = (le != null) ? le.getLocation().add(0, 1, 0) : p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(4));
        if (le != null) {
            le.damage(dmg, p);
        }
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.2, 0.2, 0.2, 0.1);
        NewExpansionAbilities.spawnDust(loc, Color.fromRGB(139, 0, 0), 1.5f, 15, 0.3, 0.3, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.2f);
        return true;
    }
}
class PureLoveBeam extends Ability {
    public PureLoveBeam(ItemEditFull pl) { super("pure_love_beam", "Pure Love Beam", "Gigantic laser blast."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        p.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);
        for (double d = 1.0; d <= 20.0; d += 0.5) {
            Location step = origin.clone().add(dir.clone().multiply(d));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(255, 20, 147), 1.6f, 10, 0.2, 0.2, 0.2);
            step.getWorld().spawnParticle(Particle.HEART, step, 5, 0.2, 0.2, 0.2, 0.05);
            step.getWorld().spawnParticle(Particle.SPELL_INSTANT, step, 8, 0.2, 0.2, 0.2, 0.05);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.5, 1.5, 1.5)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(14.0, p);
                }
            }
        }
        return true;
    }
}
class CopyTechnique extends Ability {
    public CopyTechnique(ItemEditFull pl) { super("copy_technique", "Copy Technique", "Duplicates enemy ability."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 20, 0.5, 1.0, 0.5, 0.05);
        NewExpansionAbilities.drawHelix(p.getLocation(), Color.fromRGB(0, 191, 255), 1.2, 2.0, 15);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1));
        p.sendMessage("§dCopied combat prowess! (Strength II granted)");
        return true;
    }
}
class AuthenticMutualLove extends Ability {
    private final ItemEditFull plugin;
    public AuthenticMutualLove(ItemEditFull pl) { super("authentic_mutual_love", "Authentic Mutual Love", "Domain weapon field."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.0f);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 100 || !p.isOnline()) { cancel(); return; }
                Location center = p.getLocation();
                NewExpansionAbilities.drawRing(center, Color.fromRGB(255, 105, 180), 6.0, 30);
                NewExpansionAbilities.drawRing(center, Color.fromRGB(255, 192, 203), 4.0, 20);
                center.getWorld().spawnParticle(Particle.SPELL_INSTANT, center, 10, 6.0, 1.0, 6.0, 0.01);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 1));
            }
        }.runTaskTimer(plugin, p, 0L, 2L);
        return true;
    }
}
class MaximumUzumaki extends Ability {
    public MaximumUzumaki(ItemEditFull pl) { super("maximum_uzumaki", "Maximum Uzumaki", "Cursed energy beam."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location origin = p.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        p.getWorld().playSound(origin, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.6f);
        for (double d = 1.0; d <= 18.0; d += 0.5) {
            Location step = origin.clone().add(dir.clone().multiply(d));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(75, 0, 130), 1.5f, 6, 0.2, 0.2, 0.2);
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(0, 0, 0), 1.5f, 6, 0.2, 0.2, 0.2);
            step.getWorld().spawnParticle(Particle.PORTAL, step, 8, 0.3, 0.3, 0.3, 0.05);
            step.getWorld().spawnParticle(Particle.SMOKE_LARGE, step, 3, 0.3, 0.3, 0.3, 0.02);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.4, 1.4, 1.4)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    ((LivingEntity) ent).damage(12.0, p);
                }
            }
        }
        return true;
    }
}
class InstantSpiritBody extends Ability {
    public InstantSpiritBody(ItemEditFull pl) { super("instant_spirit_body", "Instant Spirit Body", "Distorted killing form."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 1.2f);
        NewExpansionAbilities.drawHelix(p.getLocation(), Color.fromRGB(75, 75, 75), 1.5, 2.5, 20);
        p.getWorld().spawnParticle(Particle.LAVA, p.getLocation(), 20, 0.5, 1.0, 0.5, 0.1);
        return true;
    }
}
class Projection24FPS extends Ability {
    private final ItemEditFull plugin;
    public Projection24FPS(ItemEditFull pl) { super("projection_24fps", "Projection Technique", "Frame rate freeze."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Entity target = p.getTargetEntity(8);
        LivingEntity le = (target instanceof LivingEntity) ? (LivingEntity) target : null;
        Location loc;
        if (le != null) {
            loc = le.getLocation();
        } else {
            Block b = p.getTargetBlockExact(8);
            if (b != null) {
                loc = b.getLocation().add(0.5, 1, 0.5);
            } else {
                loc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(5));
            }
        }
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_PLACE, 1.2f, 1.5f);
        NewExpansionAbilities.drawSphere(loc.clone().add(0, 1, 0), Color.fromRGB(173, 216, 230), 1.5, 6);
        final List<Block> glassBlocks = new ArrayList<>();
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR) {
            b.setType(Material.GLASS);
            glassBlocks.add(b);
        }
        new CompatRunnable() {
            @Override public void run() {
                for (Block g : glassBlocks) {
                    if (g.getType() == Material.GLASS) g.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, loc, 48L); // ~2.4 seconds
        return true;
    }
}
class ComedianShow extends Ability {
    public ComedianShow(ItemEditFull pl) { super("comedian_show", "Comedian Show", "Bypassing comedy gag."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Block b = p.getTargetBlock(null, 15);
        Location baseLoc;
        if (b != null && b.getType() != Material.AIR) {
            baseLoc = b.getLocation().add(0.5, 0.0, 0.5);
        } else {
            baseLoc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(8));
        }
        Location loc = baseLoc.clone().add(0, 5.0, 0);
        p.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_CELEBRATE, 1.2f, 1.2f);
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 15, 0.5, 0.5, 0.5, 0.05);
        NewExpansionAbilities.spawnDust(loc, Color.fromRGB(255, 20, 147), 1.5f, 10, 0.5, 0.5, 0.5);
        FallingBlock fb = p.getWorld().spawnFallingBlock(loc, Material.ANVIL.createBlockData());
        fb.setDropItem(false);
        return true;
    }
}
class WindScythe extends Ability {
    public WindScythe(ItemEditFull pl) { super("wind_scythe", "Wind Scythe", "Weapon sweep wind blade."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
        for (int k = 1; k <= 6; k++) {
            Location step = loc.clone().add(dir.clone().multiply(k));
            NewExpansionAbilities.spawnDust(step, Color.fromRGB(240, 248, 255), 1.2f, 8, 0.3, 0.3, 0.3);
            step.getWorld().spawnParticle(Particle.CLOUD, step, 10, 0.4, 0.4, 0.4, 0.1);
            for (Entity ent : step.getWorld().getNearbyEntities(step, 1.4, 1.4, 1.4)) {
                if (ent instanceof LivingEntity && !ent.equals(p)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.damage(4.5, p);
                    le.setVelocity(dir.clone().multiply(1.4).setY(0.2));
                }
            }
        }
        return true;
    }
}
class BirdStrike extends Ability {
    private final ItemEditFull plugin;
    public BirdStrike(ItemEditFull pl) { super("bird_strike", "Bird Strike", "Sacrificial crow bomb."); this.plugin = pl; }
    @Override public boolean trigger(Player p, ItemStack i) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        p.getWorld().playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.0f, 0.8f);
        Parrot crow = p.getWorld().spawn(loc.clone().add(dir.clone().multiply(1.0)), Parrot.class);
        crow.setVariant(Parrot.Variant.CYAN);
        new CompatRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ > 40 || !crow.isValid()) {
                    if (crow.isValid()) {
                        crow.getWorld().createExplosion(crow.getLocation(), 3.5f, false, false);
                        crow.remove();
                    }
                    cancel();
                    return;
                    }
                crow.setVelocity(dir.clone().multiply(0.8));
                NewExpansionAbilities.spawnDust(crow.getLocation(), Color.fromRGB(20, 20, 20), 1.2f, 3, 0.1, 0.1, 0.1);
                crow.getWorld().spawnParticle(Particle.SMOKE_NORMAL, crow.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                for (Entity ent : crow.getWorld().getNearbyEntities(crow.getLocation(), 1.2, 1.2, 1.2)) {
                    if (ent instanceof LivingEntity && !ent.equals(p) && !(ent instanceof Parrot)) {
                        crow.getWorld().createExplosion(crow.getLocation(), 3.5f, false, false);
                        crow.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, p, 0L, 1L);
        return true;
    }
}
class IdleDeathGamble extends Ability {
    public IdleDeathGamble(ItemEditFull pl) { super("idle_death_gamble", "Idle Death Gamble", "Jackpot luck surge."); }
    @Override public boolean trigger(Player p, ItemStack i) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 1.5f);
        Location loc = p.getLocation();
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(0, 255, 0), 1.5, 15);
        NewExpansionAbilities.drawRing(loc, Color.fromRGB(255, 215, 0), 2.0, 20);
        p.getWorld().spawnParticle(Particle.SPELL_INSTANT, p.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
        if (Math.random() < 0.35) {
            p.sendMessage("§a§lJACKPOT! §eInfinite Cursed Energy (Regeneration & Speed IV granted!)");
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 240, 3));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 240, 3));
            p.getWorld().playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
        } else {
            p.sendMessage("§c§lMISS! §7Better luck next time.");
        }
        return true;
    }
}
