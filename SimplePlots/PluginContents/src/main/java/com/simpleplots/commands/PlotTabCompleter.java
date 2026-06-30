package com.simpleplots.commands;

import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer for the /plot command suite.
 */
public class PlotTabCompleter implements TabCompleter {

    private final List<String> subCommands = Arrays.asList(
            "help", "gui", "menu", "claim", "auto", "set", "add", "trust", "deny", "remove", "untrust", "undeny",
            "flag", "biome", "merge", "unlink", "unclaim", "unmerge", "clear", "delete",
            "backup", "download", "delete-download", "chat", "toggle", "setup",
            "condense", "trim", "database", "confirm", "info", "worldtp",
            "transfer", "visit", "home", "near", "stats",
            "admin", "list", "setowner", "reload", "massunclaim",
            "rate", "comments", "copy", "paste", "middle", "block", "unblock", "joinsupportdc", "chest"
    );

    private final List<String> flags = Arrays.asList(
            "pvp", "pve", "fly", "explosion", "snow-melt", "leaf-decay", "crop-grow",
            "no-portals", "deny-portal-travel", "redstone", "villager-interact",
            "entity-change-block", "lectern-read-book", "gamemode", "greeting",
            "farewell", "music", "feed", "heal", "time", "weather"
    );

    private final List<String> booleanValues = Arrays.asList("true", "false");
    private final List<String> gamemodeValues = Arrays.asList("SURVIVAL", "CREATIVE", "ADVENTURE", "SPECTATOR");
    private final List<String> weatherValues = Arrays.asList("CLEAR", "DOWNFALL");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (args.length > 1) {
            String firstSub = args[0].toLowerCase();
            if (!hasSubcommandPermission(player, firstSub)) {
                return Collections.emptyList();
            }
        }

        if (args.length == 1) {
            String search = args[0].toLowerCase();
            return subCommands.stream()
                    .filter(s -> s.startsWith(search))
                    .filter(s -> hasSubcommandPermission(player, s))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String search = args[1].toLowerCase();

            switch (sub) {
                case "add":
                case "trust":
                case "deny":
                case "block":
                case "remove":
                case "untrust":
                case "undeny":
                case "unblock":
                case "transfer":
                case "visit":
                case "massunclaim":
                case "setowner":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                case "flag":
                    return Arrays.asList("set", "remove").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "biome":
                    return Arrays.stream(Biome.values())
                            .map(Enum::name)
                            .filter(b -> b.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                case "backup":
                    return Arrays.asList("save", "list", "load").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "toggle":
                    return Collections.singletonList("worldedit").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "database":
                    return Arrays.asList("convert", "import").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "worldtp":
                    return Bukkit.getWorlds().stream()
                            .map(org.bukkit.World::getName)
                            .filter(w -> w.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                case "set":
                    return Arrays.asList("owner", "home", "name").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "admin":
                    return Arrays.asList("claim", "unclaim", "clear", "trust", "untrust", "deny", "undeny").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "list":
                    List<String> listOptions = new ArrayList<>();
                    listOptions.add("all");
                    listOptions.add("unclaimed");
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        listOptions.add(onlinePlayer.getName());
                    }
                    return listOptions.stream()
                            .filter(s -> s.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                case "comments":
                case "comment":
                    return Arrays.asList("add", "clear", "read").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
                case "rate":
                    return Arrays.asList("1", "2", "3", "4", "5").stream()
                            .filter(s -> s.startsWith(search))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String subSub = args[1].toLowerCase();
            String search = args[2].toLowerCase();

            if (sub.equals("flag")) {
                return flags.stream()
                        .filter(f -> f.startsWith(search))
                        .collect(Collectors.toList());
            }
            if (sub.equals("database") && subSub.equals("convert")) {
                return Arrays.asList("sqlite", "mysql").stream()
                        .filter(db -> db.startsWith(search))
                        .collect(Collectors.toList());
            }
            if (sub.equals("set") && subSub.equals("owner")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(search))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String subSub = args[1].toLowerCase();
            String flag = args[2].toLowerCase();
            String search = args[3].toLowerCase();

            if (sub.equals("flag") && subSub.equals("set")) {
                if (flag.equals("gamemode")) {
                    return gamemodeValues.stream()
                            .filter(gm -> gm.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                }
                if (flag.equals("weather")) {
                    return weatherValues.stream()
                            .filter(w -> w.toLowerCase().startsWith(search))
                            .collect(Collectors.toList());
                }
                if (Arrays.asList("greeting", "farewell", "music", "time").contains(flag)) {
                    return Collections.emptyList();
                }
                // Default booleans
                return booleanValues.stream()
                        .filter(b -> b.startsWith(search))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private boolean hasSubcommandPermission(Player player, String sub) {
        if (player.hasPermission("plots.admin")) {
            return true;
        }
        switch (sub) {
            case "help":
            case "chest":
                return true;
            case "gui":
            case "menu":
                return player.hasPermission("plots.use");
            case "claim":
                return player.hasPermission("plots.claim");
            case "auto":
                return player.hasPermission("plots.auto");
            case "info":
                return player.hasPermission("plots.info");
            case "add":
                return player.hasPermission("plots.add");
            case "remove":
                return player.hasPermission("plots.remove");
            case "trust":
            case "untrust":
                return player.hasPermission("plots.trust");
            case "deny":
            case "undeny":
                return player.hasPermission("plots.deny");
            case "flag":
                return player.hasPermission("plots.flag.set") || player.hasPermission("plots.flag.remove");
            case "biome":
                return player.hasPermission("plots.biome");
            case "merge":
                return player.hasPermission("plots.merge");
            case "unlink":
            case "unmerge":
                return player.hasPermission("plots.unlink");
            case "clear":
                return player.hasPermission("plots.clear");
            case "delete":
            case "unclaim":
                return player.hasPermission("plots.delete");
            case "chat":
                return player.hasPermission("plots.chat");
            case "backup":
                return player.hasPermission("plots.backup.save") || player.hasPermission("plots.backup.list") || player.hasPermission("plots.backup.load");
            case "download":
            case "delete-download":
                return player.hasPermission("plots.download");
            case "transfer":
                return player.hasPermission("plots.transfer");
            case "visit":
                return player.hasPermission("plots.visit");
            case "home":
                return player.hasPermission("plots.home");
            case "sethome":
                return player.hasPermission("plots.sethome");
            case "near":
                return player.hasPermission("plots.near");
            case "stats":
                return player.hasPermission("plots.stats");
            case "rate":
                return player.hasPermission("plots.rate");
            case "comments":
                return player.hasPermission("plots.comments");
            case "copy":
                return player.hasPermission("plots.copy");
            case "paste":
                return player.hasPermission("plots.paste");
            case "middle":
                return player.hasPermission("plots.middle");
            case "block":
                return player.hasPermission("plots.block");
            case "unblock":
                return player.hasPermission("plots.unblock");
            case "joinsupportdc":
                return player.hasPermission("plots.joinsupportdc");
            case "admin":
            case "setup":
            case "condense":
            case "trim":
            case "database":
            case "worldtp":
            case "setowner":
            case "reload":
            case "massunclaim":
                return player.hasPermission("plots.admin");
            case "set":
                return player.hasPermission("plots.setowner") || player.hasPermission("plots.sethome") || player.hasPermission("plots.setname");
        }
        return false;
    }
}
