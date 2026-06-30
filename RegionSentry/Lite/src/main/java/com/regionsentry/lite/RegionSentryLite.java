package com.regionsentry.lite;

import com.regionsentry.lite.command.LagMachineCommand;
import com.regionsentry.lite.gui.RegionGridGUI;
import com.regionsentry.lite.monitor.PerformanceMonitor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionSentryLite extends JavaPlugin implements CommandExecutor, org.bukkit.command.TabCompleter {
    protected PerformanceMonitor monitor;
    protected RegionGridGUI gui;
    protected com.regionsentry.lite.hud.HUDManager hudManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing RegionSentryLite Core...");
        
        monitor = new PerformanceMonitor(this);
        monitor.start();
        
        gui = new RegionGridGUI(this, monitor);
        
        hudManager = new com.regionsentry.lite.hud.HUDManager(this, monitor);
        hudManager.start();
        
        // Register chunk listener
        getServer().getPluginManager().registerEvents(new com.regionsentry.lite.monitor.ChunkListener(monitor), this);
        getServer().getPluginManager().registerEvents(new com.regionsentry.lite.monitor.BoundaryListener(monitor), this);
        
        // Register commands
        if (getCommand("lagmachine") != null) {
            LagMachineCommand lmc = new LagMachineCommand(this);
            getCommand("lagmachine").setExecutor(lmc);
            getCommand("lagmachine").setTabCompleter(lmc);
        }
        if (getCommand("regionsentry") != null) {
            getCommand("regionsentry").setExecutor(this);
            getCommand("regionsentry").setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        if (hudManager != null) {
            hudManager.cleanup();
        }
        if (monitor != null) {
            monitor.stop();
        }
        getLogger().info("RegionSentryLite Core disabled.");
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return monitor;
    }

    public RegionGridGUI getGui() {
        return gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the region map GUI.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("regionsentry.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to open the region map GUI.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("hud")) {
            hudManager.toggleHUD(player);
            return true;
        }

        gui.openGUI(player, 1);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("regionsentry.admin")) {
            return new java.util.ArrayList<>();
        }
        if (args.length == 1) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : new String[]{"hud", "history", "stitch"}) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return new java.util.ArrayList<>();
    }
}
