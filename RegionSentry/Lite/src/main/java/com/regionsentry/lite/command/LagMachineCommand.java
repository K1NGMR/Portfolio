package com.regionsentry.lite.command;

import com.regionsentry.lite.monitor.ChunkKey;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LagMachineCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final JavaPlugin plugin;
    private final java.util.Map<ChunkKey, LagMachine> activeLagMachines = ACTIVE_MACHINES;
    private static final Map<ChunkKey, LagMachine> ACTIVE_MACHINES = new ConcurrentHashMap<>();

    public LagMachineCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void stopLagMachinesForChunks(java.util.Collection<ChunkKey> chunks) {
        for (ChunkKey key : chunks) {
            LagMachine lm = ACTIVE_MACHINES.remove(key);
            if (lm != null) {
                lm.stop();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("regionsentry.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return true;

        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        ChunkKey key = new ChunkKey(world.getName(), cx, cz);

        String type = "all";
        if (args.length > 0) {
            String choice = args[0].toLowerCase();
            if (choice.equals("entity") || choice.equals("cpu") || choice.equals("block") || choice.equals("all")) {
                type = choice;
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /lagmachine [entity|cpu|block|all]");
                return true;
            }
        }

        if (ACTIVE_MACHINES.containsKey(key)) {
            LagMachine lm = ACTIVE_MACHINES.remove(key);
            if (lm != null) {
                lm.stop();
                player.sendMessage(ChatColor.GREEN + "[RegionSentry] Disabled lag machine for region thread owning chunk " + cx + ", " + cz + ".");
            }
        } else {
            LagMachine lm = new LagMachine(plugin, world, loc, cx, cz, type);
            ACTIVE_MACHINES.put(key, lm);
            lm.start();
            player.sendMessage(ChatColor.RED + "[RegionSentry] Enabled '" + type + "' lag machine for region thread owning chunk " + cx + ", " + cz + ".");
        }

        return true;
    }

    private static class LagMachine {
        private final JavaPlugin plugin;
        private final World world;
        private final Location spawnLoc;
        private final int cx;
        private final int cz;
        private final String type;
        private final List<ArmorStand> entities = new ArrayList<>();
        private ScheduledTask loadTask = null;

        public LagMachine(JavaPlugin plugin, World world, Location spawnLoc, int cx, int cz, String type) {
            this.plugin = plugin;
            this.world = world;
            this.spawnLoc = spawnLoc;
            this.cx = cx;
            this.cz = cz;
            this.type = type;
        }

        public void start() {
            int entityCount = plugin.getConfig().getInt("lag-machine.entities-to-spawn", 500);
            long spinNanos = plugin.getConfig().getLong("lag-machine.cpu-spin-duration-nanoseconds", 40000000L);
            int blockRadius = plugin.getConfig().getInt("lag-machine.block-update-radius", 2);

            Bukkit.getRegionScheduler().execute(plugin, world, cx, cz, () -> {
                if (type.equals("entity") || type.equals("all")) {
                    for (int i = 0; i < entityCount; i++) {
                        try {
                            ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, (s) -> {
                                s.setGravity(false);
                                s.setVisible(false);
                                s.setCustomName("RegionSentryTempLagEntity");
                                s.setCustomNameVisible(false);
                                s.setMarker(true);
                            });
                            entities.add(stand);
                        } catch (Exception ignored) {
                        }
                    }
                }

                loadTask = Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, cx, cz, (task) -> {
                    if (type.equals("cpu") || type.equals("all")) {
                        long start = System.nanoTime();
                        while (System.nanoTime() - start < spinNanos) {
                            // spin
                        }
                    }
                    if (type.equals("block") || type.equals("all")) {
                        org.bukkit.block.Block base = spawnLoc.getBlock();
                        for (int x = -blockRadius; x <= blockRadius; x++) {
                            for (int y = -blockRadius; y <= blockRadius; y++) {
                                for (int z = -blockRadius; z <= blockRadius; z++) {
                                    org.bukkit.block.Block rel = base.getRelative(x, y, z);
                                    rel.getState().update(true, true);
                                }
                            }
                        }
                    }
                }, 1, 1);
            });
        }

        public void stop() {
            Bukkit.getRegionScheduler().execute(plugin, world, cx, cz, () -> {
                if (loadTask != null) {
                    loadTask.cancel();
                    loadTask = null;
                }
                for (ArmorStand stand : entities) {
                    if (stand.isValid()) {
                        stand.remove();
                    }
                }
                entities.clear();
            });
        }
    }

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (!sender.hasPermission("regionsentry.admin")) {
            return new ArrayList<>();
        }
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : new String[]{"entity", "cpu", "block", "all"}) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
