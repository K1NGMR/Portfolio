package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.ArrayList;

public class Messages {

    public static String get(String key) {
        SimplePlots plugin = SimplePlots.getInstance();
        if (plugin == null) return "";
        FileConfiguration config = plugin.getMessagesFile();
        if (config == null) return "";
        
        String val = config.getString(key);
        if (val == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', val);
    }

    public static List<String> getList(String key) {
        SimplePlots plugin = SimplePlots.getInstance();
        List<String> result = new ArrayList<>();
        if (plugin == null) return result;
        FileConfiguration config = plugin.getMessagesFile();
        if (config == null) return result;

        List<String> list = config.getStringList(key);
        if (list == null || list.isEmpty()) {
            return result;
        }

        for (String line : list) {
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    public static String get(String key, Object... replacements) {
        String message = get(key);
        if (message.isEmpty()) return message;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = String.valueOf(replacements[i]);
                String replacement = String.valueOf(replacements[i + 1]);
                message = message.replace(target, replacement);
            }
        }
        return message;
    }

    public static void send(CommandSender sender, String key, Object... replacements) {
        String msg = get(key, replacements);
        if (msg != null && !msg.isEmpty() && !msg.equalsIgnoreCase("none") && !msg.trim().isEmpty()) {
            sender.sendMessage(msg);
        }
    }
}
