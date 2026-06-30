package com.equinox.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.equinox.Equinox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.Material;
import org.bukkit.Chunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiFreecamModule implements Listener {

    private final Equinox plugin;
    private int yThreshold;
    private boolean enabled;
    private PacketAdapter packetListener;
    private PacketAdapter blockChangeListener;
    private PacketAdapter entityPacketListener;
    private PacketAdapter seedPacketListener;

    private boolean packetMaskingEnabled;
    private boolean progressiveUnmaskEnabled;
    private boolean entityHidingEnabled;
    private boolean antiSeedcrackerEnabled;

    // Track the last chunk coordinates of players to detect chunk boundary crossing
    private final Map<UUID, ChunkCoord> playerChunkCoords = new ConcurrentHashMap<>();

    // Progressive unmasking: per‑player set of chunks that have been revealed
    private final Map<UUID, Set<ChunkCoord>> playerUnmaskedChunks = new ConcurrentHashMap<>();

    // Recently-modified blocks per player: block coordinate key -> expiry time (ms)
    // Blocks in this map are excluded from chunk masking to prevent ghost-block desync
    private final Map<UUID, Map<Long, Long>> recentlyModifiedBlocks = new ConcurrentHashMap<>();
    // How long (ms) to keep a broken/placed block excluded from masking (3 seconds = plenty of buffer)
    private static final long MODIFIED_BLOCK_EXPIRY_MS = 3000L;

    // Reflection fields for chunk data manipulation
    private static java.lang.reflect.Field chunkDataField;
    private static java.lang.reflect.Field lightDataField;
    private static java.lang.reflect.Field bufferField;
    private static java.lang.reflect.Field blockEntitiesField;
    private static java.lang.reflect.Field blockEntityYField;

    // Reflection fields for light updates data
    private static java.lang.reflect.Field skyLightMaskField;
    private static java.lang.reflect.Field blockLightMaskField;
    private static java.lang.reflect.Field emptySkyLightMaskField;
    private static java.lang.reflect.Field emptyBlockLightMaskField;
    private static java.lang.reflect.Field skyLightArraysField;
    private static java.lang.reflect.Field blockLightArraysField;

    // Configurable hidden block types (e.g., STONE) for masked areas
    private final Set<Material> customHiddenTargets = new HashSet<>();

    // Module 5 (Core Packet Obfuscation / Anti-Xray)
    private boolean coreObfuscationEnabled;
    private int coreObfuscationMode;
    private final Set<Material> protectedOres = new HashSet<>();
    private final Set<Integer> protectedOreStateIds = new HashSet<>();
    private final List<Integer> overworldFakeOres = new ArrayList<>();
    private final List<Integer> deepslateFakeOres = new ArrayList<>();
    private final List<Integer> netherFakeOres = new ArrayList<>();
    // Shared Random for ore replacement selection (thread-safe via ThreadLocalRandom)
    // Cached set of exposed ore positions updated on main thread for async use
    private final Map<Long, Boolean> exposedOreCache = new ConcurrentHashMap<>();

    private static sun.misc.Unsafe unsafe;
    static {
        try {
            java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        } catch (Exception ignored) {}
    }

    public AntiFreecamModule(Equinox plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("anti-freecam.enabled", true);
        packetMaskingEnabled = plugin.getConfig().getBoolean("anti-freecam.packet-masking", true);
        progressiveUnmaskEnabled = plugin.getConfig().getBoolean("anti-freecam.progressive-unmask", true);
        entityHidingEnabled = plugin.getConfig().getBoolean("anti-freecam.entity-hiding", true);
        antiSeedcrackerEnabled = plugin.getConfig().getBoolean("anti-freecam.anti-seedcracker", true);
        yThreshold = plugin.getConfig().getInt("anti-freecam.y-threshold", 60);

        // Mask block type used when hiding underground data (default: STONE)
        Material maskBlock = Material.STONE;
        String maskBlockName = plugin.getConfig().getString("anti-freecam.mask-block", "STONE");
        try {
            maskBlock = Material.valueOf(maskBlockName.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        customHiddenTargets.clear();
        customHiddenTargets.add(maskBlock);

        // Additional hidden block types defined by the admin
        List<String> list = plugin.getConfig().getStringList("anti-freecam.hidden-targets");
        if (list != null) {
            for (String s : list) {
                try {
                    customHiddenTargets.add(Material.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Reset tracking states when config reloads
        playerUnmaskedChunks.clear();
        playerChunkCoords.clear();

        // Module 5 (Core Packet Obfuscation / Anti-Xray)
        coreObfuscationEnabled = plugin.getConfig().getBoolean("core-packet-obfuscation.enabled", true);
        coreObfuscationMode = plugin.getConfig().getInt("core-packet-obfuscation.engine-mode", 2);
        protectedOres.clear();
        protectedOreStateIds.clear();
        overworldFakeOres.clear();
        deepslateFakeOres.clear();
        netherFakeOres.clear();

        List<String> ores = plugin.getConfig().getStringList("core-packet-obfuscation.protected-ores");
        if (ores != null) {
            for (String oreName : ores) {
                try {
                    Material mat = Material.valueOf(oreName.toUpperCase());
                    protectedOres.add(mat);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Pre-cache all protected ore state IDs and fake ore IDs.
        // State ID resolution is deferred to first actual packet use via getFirstBlockStateId().
        // Here we only clear and populate Material sets; actual ID lookups happen lazily.
        protectedOres.forEach(mat -> protectedOreStateIds.addAll(getBlockStateIds(mat)));

        // Remove invalid (-1) fake ore entries
        for (Material m : new Material[]{Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE}) {
            int id = getFirstBlockStateId(m);
            if (id != -1) overworldFakeOres.add(id);
        }
        for (Material m : new Material[]{Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE}) {
            int id = getFirstBlockStateId(m);
            if (id != -1) deepslateFakeOres.add(id);
        }
        for (Material m : new Material[]{Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE}) {
            int id = getFirstBlockStateId(m);
            if (id != -1) netherFakeOres.add(id);
        }
        exposedOreCache.clear();
    }

    public void start() {
        if (packetListener != null) return;

        // ----- Chunk masking listener (ProtocolLib) -----
        packetListener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, java.util.Arrays.asList(PacketType.Play.Server.MAP_CHUNK), ListenerOptions.ASYNC) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!enabled || AntiFreecamModule.this.plugin.isBypassObfuscation()) return;

                Player player = event.getPlayer();
                if (player == null || !player.isOnline()) return;

                PacketContainer packet = event.getPacket();
                int chunkX = packet.getIntegers().read(0);
                int chunkZ = packet.getIntegers().read(1);

                try {
                    boolean mask = true;
                    if (progressiveUnmaskEnabled) {
                        Set<ChunkCoord> unmasked = playerUnmaskedChunks.getOrDefault(player.getUniqueId(), Collections.emptySet());
                        if (unmasked.contains(new ChunkCoord(chunkX, chunkZ))) {
                            mask = false;
                        }
                    }

                    // Retrieve cached reachable blocks for the player
                    Set<Long> reachableCoords = com.equinox.util.CaveReachabilityManager.getReachableBlocksCached(player.getUniqueId());

                    // Build a set of recently-modified block keys in this chunk to exempt from masking.
                    // This prevents ghost-block desync after a player breaks/places blocks below threshold.
                    Set<Long> exemptKeys = getExemptKeysForChunk(player.getUniqueId(), chunkX, chunkZ);

                    // Shallow clone the NMS packet using Unsafe
                    Object oldPacket = packet.getHandle();
                    initReflection(oldPacket);

                    Object oldChunkData = chunkDataField.get(oldPacket);
                    initChunkDataReflection(oldChunkData);

                    Object newPacket = shallowCloneNmsPacket(oldPacket);
                    Object newChunkData = shallowCloneNmsPacket(oldChunkData);

                    // Clear block entities (chests, spawners, etc.) in masked sections directly in the packet copy
                    filterBlockEntities(newChunkData, player, chunkX, chunkZ, reachableCoords);

                    // Mask block data below threshold in the chunk packet byte buffer
                    if (packetMaskingEnabled && mask) {
                        byte[] originalBuffer = (byte[]) bufferField.get(oldChunkData);
                        if (originalBuffer != null) {
                            int thresholdSections = (yThreshold - player.getWorld().getMinHeight()) / 16;
                            // Pass exempt keys so recently-broken/placed blocks are not re-masked
                            byte[] maskedBuffer = maskChunkBuffer(originalBuffer, player.getWorld(), thresholdSections, chunkX, chunkZ, exemptKeys);
                            setFieldUsingUnsafe(newChunkData, bufferField, maskedBuffer);
                        }
                    }

                    // Link new chunk data to the new packet wrapper
                    setFieldUsingUnsafe(newPacket, chunkDataField, newChunkData);

                    // Mask light data (resets NibbleArrays within masked zones)
                    if (packetMaskingEnabled && mask) {
                        Object oldLightData = lightDataField.get(oldPacket);
                        if (oldLightData != null) {
                            Object newLightData = shallowCloneNmsPacket(oldLightData);
                            int thresholdSections = (yThreshold - player.getWorld().getMinHeight()) / 16;
                            maskLightData(newLightData, thresholdSections);
                            setFieldUsingUnsafe(newPacket, lightDataField, newLightData);
                        }
                    }

                    event.setPacket(PacketContainer.fromPacket(newPacket));
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "[AntiFreecam] Error modifying chunk packet", e);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);

        // ----- BLOCK_CHANGE interceptor: keep masked zone consistent -----
        // When the server sends a BLOCK_CHANGE for a position in the masked zone,
        // the client would see a sudden flip from our fake stone to the real block state.
        // We intercept and: if the position is masked (not reachable by player) AND
        // not in the recently-modified exemption list, replace the block ID with stone.
        // This eliminates the ghost-block flicker caused by server-sent block updates
        // in zones the client currently sees as solid stone.
        blockChangeListener = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                java.util.Arrays.asList(
                        PacketType.Play.Server.BLOCK_CHANGE,
                        PacketType.Play.Server.MULTI_BLOCK_CHANGE),
                ListenerOptions.ASYNC) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!enabled || !packetMaskingEnabled || AntiFreecamModule.this.plugin.isBypassObfuscation()) return;
                Player player = event.getPlayer();
                if (player == null || !player.isOnline()) return;

                PacketType type = event.getPacketType();
                UUID uuid = player.getUniqueId();

                try {
                    if (type == PacketType.Play.Server.BLOCK_CHANGE) {
                        handleSingleBlockChange(event, player, uuid);
                    } else if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                        handleMultiBlockChange(event, player, uuid);
                    }
                } catch (Exception e) {
                    // Don't crash on packet errors; just let the packet through
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(blockChangeListener);

        // ----- Entity ESP blocking listener -----
        entityPacketListener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, java.util.Arrays.asList(
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.ENTITY_METADATA), ListenerOptions.ASYNC) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!enabled || !entityHidingEnabled) return;
                Player viewer = event.getPlayer();
                if (viewer == null) return;
                
                int entityId = event.getPacket().getIntegers().read(0);
                org.bukkit.entity.Entity entity = null;
                for (org.bukkit.entity.Entity ent : viewer.getWorld().getEntities()) {
                    if (ent.getEntityId() == entityId) {
                        entity = ent;
                        break;
                    }
                }
                if (entity == null) return;

                // Hide entities that are underground beyond the Y threshold and not within reachable blocks
                if (entity.getLocation().getY() < yThreshold) {
                    Set<Long> reachable = com.equinox.util.CaveReachabilityManager.getReachableBlocksCached(viewer.getUniqueId());
                    long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ());
                    if (!reachable.contains(key)) {
                        event.setCancelled(true);
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(entityPacketListener);

        // ----- Anti-SeedCracker packet listener -----
        seedPacketListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, java.util.Arrays.asList(
                PacketType.Play.Server.LOGIN,
                PacketType.Play.Server.RESPAWN)) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (enabled && antiSeedcrackerEnabled) {
                    obfuscateSeed(event);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(seedPacketListener);
    }

    public void stop() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
            packetListener = null;
        }
        if (blockChangeListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(blockChangeListener);
            blockChangeListener = null;
        }
        if (entityPacketListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(entityPacketListener);
            entityPacketListener = null;
        }
        if (seedPacketListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(seedPacketListener);
            seedPacketListener = null;
        }

        playerChunkCoords.clear();
        playerUnmaskedChunks.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        updateUnmaskedChunks(player, cx, cz, false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        int fromCx = from.getBlockX() >> 4;
        int fromCz = from.getBlockZ() >> 4;
        int toCx = to.getBlockX() >> 4;
        int toCz = to.getBlockZ() >> 4;

        if (fromCx != toCx || fromCz != toCz) {
            updateUnmaskedChunks(player, toCx, toCz, false);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;
        int cx = to.getBlockX() >> 4;
        int cz = to.getBlockZ() >> 4;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateUnmaskedChunks(player, cx, cz, true);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        Location respawnLoc = event.getRespawnLocation();
        int cx = respawnLoc.getBlockX() >> 4;
        int cz = respawnLoc.getBlockZ() >> 4;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateUnmaskedChunks(player, cx, cz, true);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerChunkCoords.remove(uuid);
        playerUnmaskedChunks.remove(uuid);
        recentlyModifiedBlocks.remove(uuid);
    }

    /**
     * When a player breaks a block below the mask threshold, record it as recently-modified
     * so the block-change interceptor allows the real (AIR) state through to the client.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled || !packetMaskingEnabled) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getY() >= yThreshold) return;

        long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(block.getX(), block.getY(), block.getZ());
        long expiry = System.currentTimeMillis() + MODIFIED_BLOCK_EXPIRY_MS;
        recentlyModifiedBlocks
            .computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
            .put(key, expiry);
    }

    /**
     * When a player places a block below the mask threshold, record it so the chunk masker
     * does not overwrite it with stone on the next chunk packet.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled || !packetMaskingEnabled) return;
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        if (block.getY() >= yThreshold) return;

        long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(block.getX(), block.getY(), block.getZ());
        long expiry = System.currentTimeMillis() + MODIFIED_BLOCK_EXPIRY_MS;
        recentlyModifiedBlocks
            .computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
            .put(key, expiry);
    }

    private void updateUnmaskedChunks(Player player, int cx, int cz, boolean forceResend) {
        UUID uuid = player.getUniqueId();
        Set<ChunkCoord> currentUnmasked = new HashSet<>();

        // Add 3x3 chunks around the player's true position
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                currentUnmasked.add(new ChunkCoord(cx + dx, cz + dz));
            }
        }

        Set<ChunkCoord> previouslyUnmasked = playerUnmaskedChunks.get(uuid);
        if (previouslyUnmasked == null) {
            previouslyUnmasked = ConcurrentHashMap.newKeySet();
            playerUnmaskedChunks.put(uuid, previouslyUnmasked);
        }

        Set<ChunkCoord> toUnmask = new HashSet<>(currentUnmasked);
        toUnmask.removeAll(previouslyUnmasked);

        Set<ChunkCoord> toMask = new HashSet<>(previouslyUnmasked);
        toMask.removeAll(currentUnmasked);

        previouslyUnmasked.clear();
        previouslyUnmasked.addAll(currentUnmasked);

        // Forcefully trigger a localized chunk reload (sendChunk) on unmasking
        for (ChunkCoord coord : toUnmask) {
            if (player.getWorld().isChunkLoaded(coord.x, coord.z)) {
                Chunk chunk = player.getWorld().getChunkAt(coord.x, coord.z);
                resendChunkToPlayer(player, chunk);
            }
        }

        // Forcefully trigger a chunk reload on masking
        for (ChunkCoord coord : toMask) {
            if (player.getWorld().isChunkLoaded(coord.x, coord.z)) {
                Chunk chunk = player.getWorld().getChunkAt(coord.x, coord.z);
                resendChunkToPlayer(player, chunk);
            }
        }

        if (forceResend) {
            for (ChunkCoord coord : currentUnmasked) {
                if (!toUnmask.contains(coord) && !toMask.contains(coord)) {
                    if (player.getWorld().isChunkLoaded(coord.x, coord.z)) {
                        Chunk chunk = player.getWorld().getChunkAt(coord.x, coord.z);
                        resendChunkToPlayer(player, chunk);
                    }
                }
            }
        }
    }

    public void resendChunkToPlayer(Player player, Chunk chunk) {
        try {
            // NMS method to load/send chunk forcefully
            Object craftChunk = chunk.getClass().getMethod("getHandle").invoke(chunk);
            Object worldServer = chunk.getWorld().getClass().getMethod("getHandle").invoke(chunk.getWorld());
            Object lightEngine = worldServer.getClass().getMethod("getLightEngine").invoke(worldServer);
            
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket");
            Class<?> levelChunkClass = Class.forName("net.minecraft.world.level.chunk.LevelChunk");
            Class<?> lightEngineClass = Class.forName("net.minecraft.world.level.lighting.LevelLightEngine");
            
            java.lang.reflect.Constructor<?> constructor = packetClass.getConstructor(levelChunkClass, lightEngineClass, BitSet.class, BitSet.class);
            
            BitSet skyMask = new BitSet();
            BitSet blockMask = new BitSet();
            for (int i = 0; i < 24; i++) {
                skyMask.set(i);
                blockMask.set(i);
            }
            
            Object nmsPacket = constructor.newInstance(craftChunk, lightEngine, skyMask, blockMask);
            PacketContainer container = PacketContainer.fromPacket(nmsPacket);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
        } catch (Exception e) {
            // Standard API Fallback
            player.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
        }
    }

    private void obfuscateSeed(PacketEvent event) {
        try {
            PacketContainer packet = event.getPacket();
            Object handle = packet.getHandle();
            Object newHandle = modifySeedField(handle, new Random().nextLong());
            if (newHandle != handle) {
                event.setPacket(PacketContainer.fromPacket(newHandle));
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "[AntiSeedCracker] Failed to obfuscate seed", e);
        }
    }

    private Object modifySeedField(Object obj, long newSeed) throws Exception {
        if (obj == null || obj instanceof Enum<?>) return obj;
        Class<?> clazz = obj.getClass();
        
        if (clazz.isRecord()) {
            return recreateRecordWithNewSeed(obj, newSeed);
        }
        
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Field f : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                if (f.getType() == long.class) {
                    if (f.getName().equals("seed") || f.getName().contains("seed") || f.getName().equals("h")) {
                        setFieldUsingUnsafe(obj, f, newSeed);
                    }
                } else if (!f.getType().isPrimitive()) {
                    String typeName = f.getType().getName();
                    if (typeName.startsWith("net.minecraft") || typeName.startsWith("com.equinox")) {
                        Object value = f.get(obj);
                        if (value != null) {
                            Object newValue = modifySeedField(value, newSeed);
                            if (newValue != value) {
                                setFieldUsingUnsafe(obj, f, newValue);
                            }
                        }
                    }
                }
            }
            current = current.getSuperclass();
        }
        return obj;
    }

    private Object recreateRecordWithNewSeed(Object recordObj, long newSeed) throws Exception {
        Class<?> clazz = recordObj.getClass();
        java.lang.reflect.RecordComponent[] components = clazz.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        
        for (int i = 0; i < components.length; i++) {
            java.lang.reflect.RecordComponent comp = components[i];
            paramTypes[i] = comp.getType();
            
            java.lang.reflect.Method accessor = comp.getAccessor();
            accessor.setAccessible(true);
            Object val = accessor.invoke(recordObj);
            
            if (comp.getType() == long.class && (comp.getName().equals("seed") || comp.getName().contains("seed") || comp.getName().equals("h"))) {
                args[i] = newSeed;
            } else if (val != null && !comp.getType().isPrimitive()) {
                String typeName = comp.getType().getName();
                if (typeName.startsWith("net.minecraft") || typeName.startsWith("com.equinox")) {
                    args[i] = modifySeedField(val, newSeed);
                } else {
                    args[i] = val;
                }
            } else {
                args[i] = val;
            }
        }
        
        java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor(paramTypes);
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }


    /**
     * Returns true if a block key is exempt (recently broken/placed) for the given player.
     * Safe to call from async threads.
     */
    private boolean isBlockExempt(UUID uuid, long key) {
        Map<Long, Long> map = recentlyModifiedBlocks.get(uuid);
        if (map == null) return false;
        Long expiry = map.get(key);
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) {
            map.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Returns true if the given Y coordinate is in the masked zone for the player's world.
     */
    private boolean isInMaskedZone(Player player, int y) {
        if (!progressiveUnmaskEnabled) return y < yThreshold;
        // Also check if the player's chunk is in the unmasked set — if so, all blocks pass through
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        Set<ChunkCoord> unmasked = playerUnmaskedChunks.getOrDefault(player.getUniqueId(), Collections.emptySet());
        if (unmasked.contains(new ChunkCoord(cx, cz))) return false;
        return y < yThreshold;
    }

    /**
     * Intercept a single BLOCK_CHANGE packet (one block position update).
     * If the block is in the masked zone and NOT recently broken by the player,
     * replace it with stone so the client sees no flicker vs the chunk masking.
     * If it IS recently broken, let the real state through (so they see AIR).
     */
    private void handleSingleBlockChange(PacketEvent event, Player player, UUID uuid) {
        try {
            PacketContainer packet = event.getPacket();
            // The block position is in a BlockPosition wrapper
            com.comphenix.protocol.wrappers.BlockPosition pos = packet.getBlockPositionModifier().read(0);
            if (pos == null) return;

            int y = pos.getY();
            if (!isInMaskedZone(player, y)) return; // Above threshold — not our concern

            long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(pos.getX(), pos.getY(), pos.getZ());
            if (isBlockExempt(uuid, key)) {
                // Player broke/placed this block — let real state through
                return;
            }

            // In masked zone and not exempt: send stone so client stays consistent with chunk mask
            // Use the integer modifier which holds the block state ID
            int stoneId = getStoneStateId();
            // ProtocolLib wraps the block data as a WrappedBlockData or integer depending on version
            // Use the Modifier approach to set the block state
            if (packet.getIntegers().size() != 0) {
                packet.getIntegers().write(0, stoneId);
            } else {
                // Newer versions use WrappedBlockData
                packet.getBlockData().write(0,
                    com.comphenix.protocol.wrappers.WrappedBlockData.createData(Material.STONE));
            }
        } catch (Exception ignored) {
            // If anything fails, let the packet through unchanged
        }
    }

    /**
     * Intercept a MULTI_BLOCK_CHANGE (SECTION_BLOCKS_UPDATE) packet.
     * For each block in the section, apply the same masking logic as BLOCK_CHANGE.
     * In MC 1.20, this packet sends: sectionPos (long) + array of short-encoded block records.
     * Each record encodes: (localY << 8 | localZ << 4 | localX) in the high bits, stateId in the low bits.
     * ProtocolLib 5.x exposes this via getLongs(0) for sectionPos and getShortArrays(0) for records.
     */
    private void handleMultiBlockChange(PacketEvent event, Player player, UUID uuid) {
        try {
            PacketContainer packet = event.getPacket();
            
            // Read the section position packed as a long
            // Format: (chunkX << 42) | (sectionY << 20) | (chunkZ & 0xFFFFF)
            if (packet.getLongs().size() < 1) return;
            long sectionPosLong = packet.getLongs().read(0);
            
            // Decode chunk section coordinates
            int secX = (int) (sectionPosLong >> 42);
            int secY = (int) ((sectionPosLong << 44) >> 44 >> 20); // sectionY
            int sectionMinY = secY * 16;
            
            // If the section is above the threshold, pass through unchanged
            if (!isInMaskedZone(player, sectionMinY)) return;
            if (!isInMaskedZone(player, sectionMinY + 15)) {
                // Partially in masked zone — only some blocks need masking
                // For simplicity, check per-block below
            }
            
            // Get the block state records: encoded as longs (position+state packed together)
            // In ProtocolLib 5 / MC 1.20, the records are exposed as a long array (not short array)
            // Each long: high 12 bits = localPos (x|z|y packed), low 52 bits = blockState ID
            // However the exact API differs; fall back to cancelling if in a fully masked section
            // and no exempt blocks are present for this player.
            Map<Long, Long> modMap = recentlyModifiedBlocks.get(uuid);
            boolean hasExempt = modMap != null && !modMap.isEmpty() &&
                    modMap.values().stream().anyMatch(exp -> exp >= System.currentTimeMillis());
            
            if (!hasExempt) {
                // No recently modified blocks — safe to cancel this packet entirely
                // (client already sees stone from the chunk mask, so this is consistent)
                event.setCancelled(true);
            }
            // If there are exempt blocks we let it through to avoid ghost-block desync
        } catch (Exception ignored) {
            // If anything fails, let the packet through unchanged
        }
    }

    public boolean isEnabled() {
        return enabled;
    }


    /**
     * Returns a set of block keys in the given chunk that have been recently broken/placed
     * by the player and are still within the expiry window.
     * Expired entries are cleaned up lazily.
     */
    private Set<Long> getExemptKeysForChunk(UUID uuid, int chunkX, int chunkZ) {
        Map<Long, Long> modifiedMap = recentlyModifiedBlocks.get(uuid);
        if (modifiedMap == null || modifiedMap.isEmpty()) return Collections.emptySet();

        long now = System.currentTimeMillis();
        Set<Long> exempt = new HashSet<>();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        Iterator<Map.Entry<Long, Long>> iter = modifiedMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Long> entry = iter.next();
            if (entry.getValue() < now) {
                iter.remove(); // Lazy expiry
                continue;
            }
            long key = entry.getKey();
            // Decode x and z from the packed key (matches blockLocToKey encoding)
            int bx = (int) (key & 0x3FFFFFFL);
            if (bx > 0x1FFFFFFL) bx -= 0x4000000; // sign-extend
            int bz = (int) ((key >> 26) & 0x3FFFFFFL);
            if (bz > 0x1FFFFFFL) bz -= 0x4000000;
            if (bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ) {
                exempt.add(key);
            }
        }
        return exempt;
    }

    // --- Chunk Section Data Parsing ---
    private byte[] maskChunkBuffer(byte[] originalBuffer, World world, int thresholdSections,
                                   int chunkX, int chunkZ, Set<Long> exemptKeys) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(originalBuffer));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(originalBuffer.length);
        DataOutputStream out = new DataOutputStream(bos);
        
        int totalSections = (world.getMaxHeight() - world.getMinHeight()) / 16;
        int stoneStateId = getStoneStateId();
        int worldMinY = world.getMinHeight();
        
        for (int sectionIndex = 0; sectionIndex < totalSections; sectionIndex++) {
            if (in.available() <= 0) {
                break;
            }
            
            // MC 26.1.2 ChunkSectionType26_1 writes TWO shorts: nonAirBlocksCount + fluidCount
            short nonAirBlockCount = in.readShort();
            short fluidCount = in.readShort();
            boolean replace = sectionIndex < thresholdSections;

            // If this section contains any recently-modified blocks, do NOT mask it.
            // This prevents ghost-block desync when a player breaks blocks in the masked zone.
            if (replace && !exemptKeys.isEmpty()) {
                int sectionMinY = worldMinY + sectionIndex * 16;
                int sectionMaxY = sectionMinY + 15;
                for (long key : exemptKeys) {
                    int by = (int) ((key >> 52) & 0xFFFL);
                    if (by > 0x7FFL) by -= 0x1000; // sign-extend
                    if (by >= sectionMinY && by <= sectionMaxY) {
                        replace = false; // Exempt this whole section from masking
                        break;
                    }
                }
            }
            
            int sectionMinY = worldMinY + sectionIndex * 16;
            if (replace) {
                out.writeShort((short) 4096); // nonAirBlockCount = all stone
                out.writeShort((short) 0);    // fluidCount = no fluids
                processPalettedContainer(in, out, true, stoneStateId, 8, "blocks", world, chunkX, chunkZ, sectionMinY); // replace blocks with solid stone
                processPalettedContainer(in, out, true, 0, 3, "biomes", world, chunkX, chunkZ, sectionMinY); // replace biomes with default index
            } else {
                out.writeShort(nonAirBlockCount);
                out.writeShort(fluidCount);
                processPalettedContainer(in, out, false, 0, 8, "blocks", world, chunkX, chunkZ, sectionMinY); // copy block states
                processPalettedContainer(in, out, false, 0, 3, "biomes", world, chunkX, chunkZ, sectionMinY); // copy biomes
            }
        }
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        
        return bos.toByteArray();
    }

    private void processPalettedContainer(DataInputStream in, DataOutputStream out, boolean replace, int replaceValue, int maxPaletteBits, String type, World world, int chunkX, int chunkZ, int sectionMinY) throws Exception {
        // MC 26.1.2 PalettedContainer network format (writeFixedSizeLongArray - NO VarInt length prefix):
        //   bpv=0  (SingleValuePalette):  [bpv=0] [VarInt: value]                      → NO data array
        //   bpv≤max (LinearPalette):      [bpv]   [VarInt: count] [VarInt × n entries] → [fixed longs]
        //   bpv>max (GlobalPalette):      [bpv]   NO palette data                      → [fixed longs]
        // Data array length = ceil(containerSize / floor(64/bpv)), NO length VarInt written
        
        int bitsPerEntry = in.readByte() & 0xFF; // read as unsigned byte
        if (replace) {
            // Output a single-value palette (bpv=0) containing the replacement value
            // SingleValuePalette format: [bpv=0] [VarInt: value] → NO data array
            out.writeByte(0);
            writeVarInt(out, replaceValue);
            // NO data array for single-value palette in MC 26.1.2
            
            // Now consume (skip) the original palette data
            if (bitsPerEntry == 0) {
                // Source is also single-value: just consume one VarInt
                readVarInt(in);
                // NO data array to skip
            } else if (bitsPerEntry <= maxPaletteBits) {
                // Source is indirect (Linear): consume palette entries then fixed longs
                int paletteLength = readVarInt(in);
                for (int i = 0; i < paletteLength; i++) {
                    readVarInt(in);
                }
                skipFully(in, (long) computeDataArrayLongs(bitsPerEntry, type) * 8);
            } else {
                // Source is global (Direct): no palette data, just fixed longs
                skipFully(in, (long) computeDataArrayLongs(bitsPerEntry, type) * 8);
            }
        } else if (coreObfuscationEnabled && "blocks".equals(type) && sectionMinY < yThreshold) {
            // Core Packet Obfuscation (Module 5 / Anti-Xray)
            if (bitsPerEntry == 0) {
                // Single-value palette: check if this block is a protected ore
                int value = readVarInt(in);
                if (protectedOreStateIds.contains(value)) {
                    // Entire section is this ore - hide it
                    out.writeByte(0);
                    int replacement = getReplacementStateId(value, world, sectionMinY);
                    writeVarInt(out, replacement);
                } else {
                    out.writeByte(0);
                    writeVarInt(out, value);
                }
            } else if (bitsPerEntry <= maxPaletteBits) {
                int paletteLength = readVarInt(in);
                List<Integer> palette = new ArrayList<>();
                boolean hasTargetOre = false;
                for (int i = 0; i < paletteLength; i++) {
                    int val = readVarInt(in);
                    palette.add(val);
                    if (protectedOreStateIds.contains(val)) {
                        hasTargetOre = true;
                    }
                }

                int numLongs = computeDataArrayLongs(bitsPerEntry, type);
                byte[] dataArray = new byte[numLongs * 8];
                in.readFully(dataArray);

                if (!hasTargetOre) {
                    // No protected ores: copy container verbatim
                    out.writeByte(bitsPerEntry);
                    writeVarInt(out, paletteLength);
                    for (int val : palette) {
                        writeVarInt(out, val);
                    }
                    out.write(dataArray);
                } else {
                    // Has target ores: decode data array and replace unexposed ores
                    long[] packedLongs = new long[numLongs];
                    java.nio.ByteBuffer.wrap(dataArray).asLongBuffer().get(packedLongs);
                    int[] values = unpack(packedLongs, bitsPerEntry, 4096);
                    
                    int startX = chunkX << 4;
                    int startZ = chunkZ << 4;
                    
                    for (int i = 0; i < 4096; i++) {
                        int paletteIdx = values[i];
                        if (paletteIdx >= 0 && paletteIdx < palette.size()) {
                            int stateId = palette.get(paletteIdx);
                            if (protectedOreStateIds.contains(stateId)) {
                                int lx = i & 15;
                                int lz = (i >> 4) & 15;
                                int ly = i >> 8;
                                int absX = startX + lx;
                                int absZ = startZ + lz;
                                int absY = sectionMinY + ly;
                                
                                // isOreExposed uses the exposed cache which is safe to call async
                                if (!isOreExposedCached(absX, absY, absZ)) {
                                    int replacementStateId = getReplacementStateId(stateId, world, absY);
                                    int repIdx = palette.indexOf(replacementStateId);
                                    if (repIdx == -1) {
                                        palette.add(replacementStateId);
                                        repIdx = palette.size() - 1;
                                    }
                                    values[i] = repIdx;
                                }
                            }
                        }
                    }
                    
                    // If palette grew beyond capacity of current bitsPerEntry, upgrade bitsPerEntry
                    int effectiveBpe = bitsPerEntry;
                    while ((1 << effectiveBpe) < palette.size() && effectiveBpe < maxPaletteBits) {
                        effectiveBpe++;
                    }
                    // If still not enough bits, fall through to global palette representation
                    if ((1 << effectiveBpe) < palette.size()) {
                        // Too many entries: convert to global palette (direct state IDs)
                        // Resolve each palette index to a global state ID
                        for (int i = 0; i < 4096; i++) {
                            int idx = values[i];
                            if (idx >= 0 && idx < palette.size()) {
                                values[i] = palette.get(idx);
                            }
                        }
                        // Global palette bitsPerEntry on 1.20.4 blocks is 15
                        int globalBpe = 15;
                        long[] newPackedLongs = pack(values, globalBpe);
                        byte[] newBytes = new byte[computeDataArrayLongs(globalBpe, type) * 8];
                        java.nio.ByteBuffer.wrap(newBytes).asLongBuffer().put(newPackedLongs);
                        out.writeByte(globalBpe);
                        // Global palette has NO palette data written
                        out.write(newBytes);
                    } else {
                        // Re-pack with (possibly upgraded) bitsPerEntry
                        long[] newPackedLongs = pack(values, effectiveBpe);
                        int newNumLongs = computeDataArrayLongs(effectiveBpe, type);
                        byte[] newBytes = new byte[newNumLongs * 8];
                        java.nio.ByteBuffer.wrap(newBytes).asLongBuffer().put(newPackedLongs);
                        out.writeByte(effectiveBpe);
                        writeVarInt(out, palette.size());
                        for (int val : palette) {
                            writeVarInt(out, val);
                        }
                        out.write(newBytes);
                    }
                }
            } else {
                // Global palette (direct state IDs in data array)
                int numLongs = computeDataArrayLongs(bitsPerEntry, type);
                byte[] dataArray = new byte[numLongs * 8];
                in.readFully(dataArray);
                
                long[] packedLongs = new long[numLongs];
                java.nio.ByteBuffer.wrap(dataArray).asLongBuffer().get(packedLongs);
                int[] values = unpack(packedLongs, bitsPerEntry, 4096);
                
                boolean modified = false;
                int startX = chunkX << 4;
                int startZ = chunkZ << 4;
                
                for (int i = 0; i < 4096; i++) {
                    int stateId = values[i];
                    if (protectedOreStateIds.contains(stateId)) {
                        int lx = i & 15;
                        int lz = (i >> 4) & 15;
                        int ly = i >> 8;
                        int absX = startX + lx;
                        int absZ = startZ + lz;
                        int absY = sectionMinY + ly;
                        
                        if (!isOreExposedCached(absX, absY, absZ)) {
                            values[i] = getReplacementStateId(stateId, world, absY);
                            modified = true;
                        }
                    }
                }
                
                if (modified) {
                    long[] newPackedLongs = pack(values, bitsPerEntry);
                    byte[] newBytes = new byte[numLongs * 8];
                    java.nio.ByteBuffer.wrap(newBytes).asLongBuffer().put(newPackedLongs);
                    out.writeByte(bitsPerEntry);
                    out.write(newBytes);
                } else {
                    out.writeByte(bitsPerEntry);
                    out.write(dataArray);
                }
            }
        } else {
            // Pass-through: copy the container verbatim
            out.writeByte(bitsPerEntry);
            if (bitsPerEntry == 0) {
                // Single-value palette: copy one VarInt, NO data array
                writeVarInt(out, readVarInt(in));
            } else if (bitsPerEntry <= maxPaletteBits) {
                // Indirect (Linear) palette: copy palette entries, then copy fixed longs
                int paletteLength = readVarInt(in);
                writeVarInt(out, paletteLength);
                for (int i = 0; i < paletteLength; i++) {
                    writeVarInt(out, readVarInt(in));
                }
                int numLongs = computeDataArrayLongs(bitsPerEntry, type);
                byte[] dataArray = new byte[numLongs * 8];
                in.readFully(dataArray);
                out.write(dataArray);
            } else {
                // Global (Direct) palette: no palette data, just copy fixed longs
                int numLongs = computeDataArrayLongs(bitsPerEntry, type);
                byte[] dataArray = new byte[numLongs * 8];
                in.readFully(dataArray);
                out.write(dataArray);
            }
        }
    }

    // --- Module 5 Dynamic Obfuscation Helpers ---

    private int getReplacementStateId(int originalStateId, World world, int y) {
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        if (coreObfuscationMode == 1) {
            // Level 1: Solid stone/deepslate/netherrack replacement
            if (isNether) return getNetherrackStateId();
            return y < 0 ? getDeepslateStateId() : getStoneStateId();
        } else {
            // Level 2: Fake ores to mislead X-ray users
            if (isNether) {
                if (netherFakeOres.isEmpty()) return getNetherrackStateId();
                return netherFakeOres.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(netherFakeOres.size()));
            } else {
                boolean isDeepslate = y < 0;
                List<Integer> choices = isDeepslate ? deepslateFakeOres : overworldFakeOres;
                if (choices.isEmpty()) return y < 0 ? getDeepslateStateId() : getStoneStateId();
                return choices.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(choices.size()));
            }
        }
    }

    private static int deepslateStateId = -1;
    private static int getDeepslateStateId() {
        if (deepslateStateId != -1) return deepslateStateId;
        deepslateStateId = getFirstBlockStateId(Material.DEEPSLATE);
        if (deepslateStateId == -1) deepslateStateId = getStoneStateId();
        return deepslateStateId;
    }

    private static int netherrackStateId = -1;
    private static int getNetherrackStateId() {
        if (netherrackStateId != -1) return netherrackStateId;
        netherrackStateId = getFirstBlockStateId(Material.NETHERRACK);
        if (netherrackStateId == -1) netherrackStateId = getStoneStateId();
        return netherrackStateId;
    }

    private static int getFirstBlockStateId(Material material) {
        try {
            Class<?> blocksClass = Class.forName("net.minecraft.world.level.block.Blocks");
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            
            java.lang.reflect.Field field = blocksClass.getField(material.name());
            Object block = field.get(null);
            
            java.lang.reflect.Method defaultBlockStateMethod = block.getClass().getMethod("defaultBlockState");
            Object defaultBlockState = defaultBlockStateMethod.invoke(block);
            
            java.lang.reflect.Method getIdMethod = blockClass.getMethod("getId", blockStateClass);
            return (Integer) getIdMethod.invoke(null, defaultBlockState);
        } catch (Exception e) {
            return -1;
        }
    }

    private List<Integer> getBlockStateIds(Material material) {
        List<Integer> ids = new ArrayList<>();
        try {
            Class<?> blocksClass = Class.forName("net.minecraft.world.level.block.Blocks");
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            
            java.lang.reflect.Field field = blocksClass.getField(material.name());
            Object block = field.get(null);
            
            java.lang.reflect.Method getPossibleStatesMethod = block.getClass().getMethod("getStateDefinition");
            Object stateDefinition = getPossibleStatesMethod.invoke(block);
            java.lang.reflect.Method getPossibleStatesListMethod = stateDefinition.getClass().getMethod("getPossibleStates");
            List<?> possibleStates = (List<?>) getPossibleStatesListMethod.invoke(stateDefinition);
            
            java.lang.reflect.Method getIdMethod = blockClass.getMethod("getId", blockStateClass);
            for (Object state : possibleStates) {
                int id = (Integer) getIdMethod.invoke(null, state);
                ids.add(id);
            }
        } catch (Exception e) {
            int defaultId = getFirstBlockStateId(material);
            if (defaultId != -1) {
                ids.add(defaultId);
            }
        }
        return ids;
    }

    private static int[] unpack(long[] longs, int bitsPerEntry, int size) {
        int[] values = new int[size];
        if (bitsPerEntry == 0) return values;
        
        long maxEntryValue = (1L << bitsPerEntry) - 1L;
        int valuesPerLong = 64 / bitsPerEntry;
        
        for (int i = 0; i < size; i++) {
            int longIndex = i / valuesPerLong;
            int bitOffset = (i % valuesPerLong) * bitsPerEntry;
            values[i] = (int) ((longs[longIndex] >>> bitOffset) & maxEntryValue);
        }
        return values;
    }

    private static long[] pack(int[] values, int bitsPerEntry) {
        int size = values.length;
        int valuesPerLong = 64 / bitsPerEntry;
        int numLongs = (size + valuesPerLong - 1) / valuesPerLong;
        long[] longs = new long[numLongs];
        
        long maxEntryValue = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < size; i++) {
            int longIndex = i / valuesPerLong;
            int bitOffset = (i % valuesPerLong) * bitsPerEntry;
            long value = values[i] & maxEntryValue;
            longs[longIndex] |= (value << bitOffset);
        }
        return longs;
    }

    /**
     * Main-thread-safe ore exposure check. Updates the exposed ore cache.
     * Must only be called from the server main thread (in the cave reachability update task).
     */
    public void updateExposedOreCache(World world, int chunkX, int chunkZ) {
        if (!coreObfuscationEnabled) return;
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int minY = world.getMinHeight();
        int maxY = Math.min(yThreshold, world.getMaxHeight());
        int[] ddx = {1, -1, 0, 0, 0, 0};
        int[] ddy = {0, 0, 1, -1, 0, 0};
        int[] ddz = {0, 0, 0, 0, 1, -1};
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = minY; y < maxY; y++) {
                    int bx = startX + lx;
                    int bz = startZ + lz;
                    Block b = world.getBlockAt(bx, y, bz);
                    if (!protectedOres.contains(b.getType())) continue;
                    long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(bx, y, bz);
                    boolean exposed = false;
                    for (int d = 0; d < 6; d++) {
                        int nx = bx + ddx[d];
                        int ny = y + ddy[d];
                        int nz = bz + ddz[d];
                        if (ny < minY || ny >= world.getMaxHeight()) continue;
                        if (!world.isChunkLoaded(nx >> 4, nz >> 4)) continue;
                        Material t = world.getBlockAt(nx, ny, nz).getType();
                        if (t.isAir() || t == Material.WATER || t == Material.LAVA) {
                            exposed = true;
                            break;
                        }
                    }
                    exposedOreCache.put(key, exposed);
                }
            }
        }
    }

    /**
     * Async-safe ore exposure check using the pre-computed cache.
     * Falls back to treating ore as unexposed (hidden) if not in cache.
     */
    private boolean isOreExposedCached(int x, int y, int z) {
        long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(x, y, z);
        Boolean exposed = exposedOreCache.get(key);
        // Default to false (hide the ore) if we don't have data yet
        return exposed != null && exposed;
    }
    
    /**
     * Compute the number of longs in the fixed data array for a PalettedContainer,
     * matching MC 26.1.2's writeFixedSizeLongArray: ceil(containerSize / floor(64/bpv)).
     */
    private static int computeDataArrayLongs(int bitsPerEntry, String type) {
        if (bitsPerEntry == 0) return 0;
        int containerSize = "biomes".equals(type) ? 64 : 4096;
        int valuesPerLong = 64 / bitsPerEntry;
        return (containerSize + valuesPerLong - 1) / valuesPerLong;
    }
    
    private static void skipFully(DataInputStream in, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                throw new java.io.EOFException("Could not skip remaining " + remaining + " bytes");
            }
            remaining -= skipped;
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    private static int stoneStateId = -1;
    private static int getStoneStateId() {
        if (stoneStateId != -1) return stoneStateId;
        try {
            Class<?> blocksClass = Class.forName("net.minecraft.world.level.block.Blocks");
            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            
            java.lang.reflect.Field stoneField = blocksClass.getField("STONE");
            Object stoneBlock = stoneField.get(null);
            
            java.lang.reflect.Method defaultBlockStateMethod = stoneBlock.getClass().getMethod("defaultBlockState");
            Object defaultBlockState = defaultBlockStateMethod.invoke(stoneBlock);
            
            java.lang.reflect.Method getIdMethod = blockClass.getMethod("getId", blockStateClass);
            stoneStateId = (Integer) getIdMethod.invoke(null, defaultBlockState);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AntiFreecam] Could not resolve Stone State ID via NMS: " + e.getMessage() + ". Using fallback 1.");
            stoneStateId = 1;
        }
        return stoneStateId;
    }

    // --- Reflection and Unsafe Helpers ---

    private Object shallowCloneNmsPacket(Object packetHandle) throws Exception {
        Class<?> packetClass = packetHandle.getClass();
        Object newPacket = unsafe.allocateInstance(packetClass);
        Class<?> current = packetClass;
        
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Field f : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                f.set(newPacket, f.get(packetHandle));
            }
            current = current.getSuperclass();
        }
        return newPacket;
    }

    private static void setFieldUsingUnsafe(Object obj, java.lang.reflect.Field f, Object value) throws Exception {
        long offset = unsafe.objectFieldOffset(f);
        if (f.getType() == long.class) {
            unsafe.putLong(obj, offset, (Long) value);
        } else {
            unsafe.putObject(obj, offset, value);
        }
    }

    private static void initReflection(Object packetHandle) throws Exception {
        if (chunkDataField == null) {
            for (java.lang.reflect.Field field : packetHandle.getClass().getDeclaredFields()) {
                if (field.getType().getSimpleName().equals("ClientboundLevelChunkPacketData")) {
                    field.setAccessible(true);
                    chunkDataField = field;
                } else if (field.getType().getSimpleName().equals("ClientboundLightUpdatePacketData")) {
                    field.setAccessible(true);
                    lightDataField = field;
                }
            }
            if (chunkDataField == null) {
                throw new RuntimeException("Could not find chunkData field");
            }
        }
    }

    private static void initChunkDataReflection(Object chunkData) throws Exception {
        if (bufferField != null) return;
        for (java.lang.reflect.Field field : chunkData.getClass().getDeclaredFields()) {
            if (field.getType() == byte[].class) {
                field.setAccessible(true);
                bufferField = field;
            } else if (field.getType() == List.class) {
                field.setAccessible(true);
                blockEntitiesField = field;
            }
        }
        if (bufferField == null) {
            throw new RuntimeException("Could not find buffer field");
        }
    }

    private static void initLightReflection(Object lightData) throws Exception {
        if (skyLightMaskField != null) return;
        
        List<java.lang.reflect.Field> bitSetFields = new ArrayList<>();
        List<java.lang.reflect.Field> listFields = new ArrayList<>();
        
        for (java.lang.reflect.Field f : lightData.getClass().getDeclaredFields()) {
            if (f.getType() == BitSet.class) {
                f.setAccessible(true);
                bitSetFields.add(f);
            } else if (f.getType() == List.class) {
                f.setAccessible(true);
                listFields.add(f);
            }
        }
        
        if (bitSetFields.size() >= 4) {
            skyLightMaskField = bitSetFields.get(0);
            blockLightMaskField = bitSetFields.get(1);
            emptySkyLightMaskField = bitSetFields.get(2);
            emptyBlockLightMaskField = bitSetFields.get(3);
        }
        if (listFields.size() >= 2) {
            skyLightArraysField = listFields.get(0);
            blockLightArraysField = listFields.get(1);
        }
    }

    private void filterBlockEntities(Object chunkData, Player player, int chunkX, int chunkZ, Set<Long> reachableCoords) throws Exception {
        List<?> list = (List<?>) blockEntitiesField.get(chunkData);
        if (list == null || list.isEmpty()) return;

        // Clone list before mutating to avoid aliasing with original packet
        List<Object> newList = new ArrayList<>(list);
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        World world = player.getWorld();

        newList.removeIf(info -> {
            try {
                int xOffset = getBlockEntityXOffset(info);
                int zOffset = getBlockEntityZOffset(info);
                int y = getBlockEntityY(info);

                int absoluteX = startX + xOffset;
                int absoluteZ = startZ + zOffset;

                long key = com.equinox.util.CaveReachabilityManager.blockLocToKey(absoluteX, y, absoluteZ);
                boolean reachable = reachableCoords.contains(key);

                if (!reachable && world.isChunkLoaded(absoluteX >> 4, absoluteZ >> 4)) {
                    org.bukkit.block.Block block = world.getBlockAt(absoluteX, y, absoluteZ);
                    return customHiddenTargets.contains(block.getType());
                }
                return false;
            } catch (Exception e) {
                return false; // Don't remove on error
            }
        });

        blockEntitiesField.set(chunkData, newList);
    }

    private static int getBlockEntityXOffset(Object blockEntityInfo) throws Exception {
        for (java.lang.reflect.Field f : blockEntityInfo.getClass().getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("x") || f.getName().contains("xOffset") || f.getName().contains("packedXZ"))) {
                f.setAccessible(true);
                int val = f.getInt(blockEntityInfo);
                if (f.getName().contains("packedXZ")) {
                    return (val >> 4) & 15;
                }
                return val & 15;
            }
        }
        return 0;
    }

    private static int getBlockEntityZOffset(Object blockEntityInfo) throws Exception {
        for (java.lang.reflect.Field f : blockEntityInfo.getClass().getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("z") || f.getName().contains("zOffset") || f.getName().contains("packedXZ"))) {
                f.setAccessible(true);
                int val = f.getInt(blockEntityInfo);
                if (f.getName().contains("packedXZ")) {
                    return val & 15;
                }
                return val & 15;
            }
        }
        return 0;
    }

    private static int getBlockEntityY(Object blockEntityInfo) throws Exception {
        if (blockEntityYField == null) {
            for (java.lang.reflect.Field f : blockEntityInfo.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    if (f.getName().equals("y")) {
                        blockEntityYField = f;
                        break;
                    }
                }
            }
            if (blockEntityYField == null) {
                int count = 0;
                for (java.lang.reflect.Field f : blockEntityInfo.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        f.setAccessible(true);
                        count++;
                        if (count == 2) {
                            blockEntityYField = f;
                            break;
                        }
                    }
                }
            }
        }
        
        if (blockEntityYField != null) {
            return blockEntityYField.getInt(blockEntityInfo);
        }
        return 0;
    }

    private void maskLightData(Object lightData, int thresholdSections) throws Exception {
        initLightReflection(lightData);
        
        BitSet skyMask = skyLightMaskField != null ? (BitSet) skyLightMaskField.get(lightData) : null;
        BitSet blockMask = blockLightMaskField != null ? (BitSet) blockLightMaskField.get(lightData) : null;
        BitSet emptySky = emptySkyLightMaskField != null ? (BitSet) emptySkyLightMaskField.get(lightData) : null;
        BitSet emptyBlock = emptyBlockLightMaskField != null ? (BitSet) emptyBlockLightMaskField.get(lightData) : null;
        
        List<?> skyList = skyLightArraysField != null ? (List<?>) skyLightArraysField.get(lightData) : null;
        List<?> blockList = blockLightArraysField != null ? (List<?>) blockLightArraysField.get(lightData) : null;
        
        if (skyMask != null && skyList != null) {
            BitSet newSkyMask = (BitSet) skyMask.clone();
            BitSet newEmptySky = emptySky != null ? (BitSet) emptySky.clone() : new BitSet();
            
            int removeCount = 0;
            for (int i = 0; i < thresholdSections; i++) {
                if (newSkyMask.get(i)) {
                    removeCount++;
                }
            }
            
            List<Object> newSkyList = new ArrayList<>(skyList);
            for (int i = 0; i < removeCount && !newSkyList.isEmpty(); i++) {
                newSkyList.remove(0);
            }
            
            for (int i = 0; i < thresholdSections; i++) {
                newSkyMask.clear(i);
                newEmptySky.set(i);
            }
            
            skyLightMaskField.set(lightData, newSkyMask);
            if (emptySkyLightMaskField != null) {
                emptySkyLightMaskField.set(lightData, newEmptySky);
            }
            if (skyLightArraysField != null) {
                skyLightArraysField.set(lightData, newSkyList);
            }
        }
        
        if (blockMask != null && blockList != null) {
            BitSet newBlockMask = (BitSet) blockMask.clone();
            BitSet newEmptyBlock = emptyBlock != null ? (BitSet) emptyBlock.clone() : new BitSet();
            
            int removeCount = 0;
            for (int i = 0; i < thresholdSections; i++) {
                if (newBlockMask.get(i)) {
                    removeCount++;
                }
            }
            
            List<Object> newBlockList = new ArrayList<>(blockList);
            for (int i = 0; i < removeCount && !newBlockList.isEmpty(); i++) {
                newBlockList.remove(0);
            }
            
            for (int i = 0; i < thresholdSections; i++) {
                newBlockMask.clear(i);
                newEmptyBlock.set(i);
            }
            
            blockLightMaskField.set(lightData, newBlockMask);
            if (emptyBlockLightMaskField != null) {
                emptyBlockLightMaskField.set(lightData, newEmptyBlock);
            }
            if (blockLightArraysField != null) {
                blockLightArraysField.set(lightData, newBlockList);
            }
        }
    }

    private static class ChunkCoord {
        public final int x;
        public final int z;

        public ChunkCoord(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkCoord that = (ChunkCoord) o;
            return x == that.x && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}
