package com.equinox.commands;

import com.equinox.Equinox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class EquinoxCommand implements CommandExecutor, Listener {

    private final Equinox plugin;

    public EquinoxCommand(Equinox plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("equinox.admin")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage("§a[Equinox] Configuration reloaded successfully.");
            return true;
        }

        sender.sendMessage("§5§l================= §d§lEquinox Admin §5§l=================");
        sender.sendMessage("§d/equinox reload §7- Reload the plugin configuration.");
        sender.sendMessage("§5================================================");
        return true;
    }
}
