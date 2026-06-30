package com.itemedit.full.command;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import com.itemedit.full.utils.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Registry;

import java.util.*;
import java.util.stream.Collectors;

public class ItemEditCommand implements CommandExecutor, TabCompleter {
    private final ItemEditFull plugin;

    public ItemEditCommand(ItemEditFull plugin) {
        this.plugin = plugin;
    }

    private void msg(Player p, String key, Object... args) {
        p.sendMessage(LanguageManager.getMessage(p, key, args));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("itemedit.use")) {
            msg(player, "no_permission");
            return true;
        }

        if (args.length == 0) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                msg(player, "hold_item");
                return true;
            }
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!player.hasPermission("itemedit.admin")) {
                msg(player, "no_permission_reload");
                return true;
            }
            plugin.reloadConfig();
            plugin.getWeaponConfigManager().reload();
            plugin.getAbilityConfigManager().reload();
            msg(player, "reload_success");
            return true;
        }

        if (sub.equals("give")) {
            handleGive(player, args);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            msg(player, "hold_item");
            return true;
        }

        switch (sub) {
            case "rename":
                handleRename(player, item, args);
                break;
            case "lore":
                handleLore(player, item, args);
                break;
            case "enchant":
                handleEnchant(player, item, args);
                break;
            case "unbreakable":
                handleUnbreakable(player, item, args);
                break;
            case "flag":
                handleFlag(player, item, args);
                break;
            case "attribute":
                handleAttribute(player, item, args);
                break;
            case "ability":
                handleAbility(player, item, args);
                break;
            case "custom":
                handleCustom(player, item, args);
                break;
            case "gui":
                plugin.getGuiManager().openMainMenu(player);
                break;
            case "hidetooltips":
                handleHideTooltips(player, item, args);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§lItemEdit Full Commands:");
        player.sendMessage("§e/ie gui §7- Opens the visual editing GUI.");
        player.sendMessage("§e/ie rename <name> §7- Renames the held item.");
        player.sendMessage("§e/ie lore add <text> §7- Adds a line of lore.");
        player.sendMessage("§e/ie lore set <line> <text> §7- Sets a specific line of lore.");
        player.sendMessage("§e/ie lore remove <line> §7- Removes a line of lore.");
        player.sendMessage("§e/ie lore clear §7- Clears all lore.");
        player.sendMessage("§e/ie enchant <enchantment> <level> §7- Enchants the item.");
        player.sendMessage("§e/ie unbreakable <true/false> §7- Sets unbreakable state.");
        player.sendMessage("§e/ie flag <add/remove/clear> <flag> §7- Manages item flags.");
        player.sendMessage("§e/ie attribute <add/remove/clear> <attr> [val] §7- Manages attributes.");
        player.sendMessage("§e/ie ability <add/remove/clear/list> [ability] §7- Manages item abilities.");
        player.sendMessage("§e/ie custom <ability> <param> <value> §7- Overrides ability values.");
        player.sendMessage("§e/ie hidetooltips [true/false] §7- Hides or shows item tooltips.");
        player.sendMessage("§e/ie give [player] <weapon_key> §7- Gives a weapon from weapon.yml.");
        player.sendMessage("§e/ie reload §7- Reloads config.yml and weapon.yml files.");
    }

    private Component parseText(String text) {
        String translated = ChatColor.translateAlternateColorCodes('&', text);
        if (text.contains("<") && text.contains(">")) {
            return MiniMessage.miniMessage().deserialize(text);
        }
        return LegacyComponentSerializer.legacySection().deserialize(translated);
    }

    private void saveItem(Player player, ItemStack item, ItemMeta meta) {
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    private void handleRename(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_rename");
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(parseText(name));
        saveItem(player, item, meta);
        msg(player, "rename_success");
    }

    private void handleLore(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_lore");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String operation = args[1].toLowerCase();
        switch (operation) {
            case "add":
                if (args.length < 3) {
                    msg(player, "usage_lore_add");
                    return;
                }
                String addedText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                lore.add(parseText(addedText));
                meta.lore(lore);
                saveItem(player, item, meta);
                msg(player, "lore_add_success");
                break;
            case "set":
                if (args.length < 4) {
                    msg(player, "usage_lore_set");
                    return;
                }
                try {
                    int line = Integer.parseInt(args[2]) - 1;
                    if (line < 0 || line > lore.size()) {
                        msg(player, "lore_invalid_line", "%size%", lore.size());
                        return;
                    }
                    String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    Component parsed = parseText(text);
                    if (line == lore.size()) {
                        lore.add(parsed);
                    } else {
                        lore.set(line, parsed);
                    }
                    meta.lore(lore);
                    saveItem(player, item, meta);
                    msg(player, "lore_set_success", "%line%", line + 1);
                } catch (NumberFormatException e) {
                    msg(player, "line_must_be_number");
                }
                break;
            case "remove":
                if (args.length < 3) {
                    msg(player, "usage_lore_remove");
                    return;
                }
                try {
                    int line = Integer.parseInt(args[2]) - 1;
                    if (line < 0 || line >= lore.size()) {
                        msg(player, "lore_out_of_bounds");
                        return;
                    }
                    lore.remove(line);
                    meta.lore(lore);
                    saveItem(player, item, meta);
                    msg(player, "lore_remove_success", "%line%", line + 1);
                } catch (NumberFormatException e) {
                    msg(player, "line_must_be_number");
                }
                break;
            case "clear":
                meta.lore(null);
                saveItem(player, item, meta);
                msg(player, "lore_clear_success");
                break;
            default:
                msg(player, "unknown_operation");
                break;
        }
    }

    private void handleEnchant(Player player, ItemStack item, String[] args) {
        if (args.length < 3) {
            msg(player, "usage_enchant");
            return;
        }

        String enchantName = args[1].toLowerCase();
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msg(player, "level_must_be_number");
            return;
        }

        Enchantment enchantment = null;
        for (Enchantment e : Enchantment.values()) {
            if (e.getName().equalsIgnoreCase(enchantName) || e.getKey().getKey().equalsIgnoreCase(enchantName)) {
                enchantment = e;
                break;
            }
        }

        if (enchantment == null) {
            msg(player, "enchant_not_found");
            return;
        }

        if (level <= 0) {
            item.removeEnchantment(enchantment);
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            msg(player, "enchant_remove_success", "%enchant%", enchantment.getKey().getKey());
        } else {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(enchantment, level, true);
            saveItem(player, item, meta);
            msg(player, "enchant_add_success", "%enchant%", enchantment.getKey().getKey(), "%level%", level);
        }
    }

    private void handleUnbreakable(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_unbreakable");
            return;
        }
        boolean state = Boolean.parseBoolean(args[1]);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(state);
        saveItem(player, item, meta);
        msg(player, "unbreakable_success", "%state%", state);
    }

    private void handleFlag(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_flag");
            return;
        }

        String operation = args[1].toLowerCase();
        ItemMeta meta = item.getItemMeta();

        if (operation.equalsIgnoreCase("clear")) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.removeItemFlags(flag);
            }
            saveItem(player, item, meta);
            msg(player, "flag_clear_success");
            return;
        }

        if (args.length < 3) {
            msg(player, "usage_flag_op");
            return;
        }

        String flagName = args[2].toUpperCase();
        ItemFlag flag = null;
        try {
            flag = ItemFlag.valueOf(flagName);
        } catch (IllegalArgumentException e) {
            for (ItemFlag f : ItemFlag.values()) {
                if (f.name().equalsIgnoreCase(flagName)) {
                    flag = f;
                    break;
                }
            }
        }

        if (flag == null) {
            msg(player, "invalid_flag");
            return;
        }

        if (operation.equalsIgnoreCase("add")) {
            meta.addItemFlags(flag);
            msg(player, "flag_add_success", "%flag%", flag.name());
        } else if (operation.equalsIgnoreCase("remove")) {
            meta.removeItemFlags(flag);
            msg(player, "flag_remove_success", "%flag%", flag.name());
        } else {
            msg(player, "unknown_operation");
            return;
        }
        saveItem(player, item, meta);
    }

    private List<Attribute> getAllAttributes() {
        List<Attribute> list = new ArrayList<>();
        try {
            Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            if (attributeClass.isEnum()) {
                for (Object val : (Object[]) attributeClass.getMethod("values").invoke(null)) {
                    list.add((Attribute) val);
                }
            } else {
                java.lang.reflect.Field field = Registry.class.getField("ATTRIBUTE");
                Registry<?> registry = (Registry<?>) field.get(null);
                for (Object obj : registry) {
                    if (obj instanceof Attribute) {
                        list.add((Attribute) obj);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return list;
    }

    private Attribute getAttributeByName(String name) {
        String upper = name.toUpperCase();
        for (Attribute a : getAllAttributes()) {
            if (a instanceof org.bukkit.Keyed) {
                String key = ((org.bukkit.Keyed) a).getKey().getKey().toUpperCase();
                if (key.equalsIgnoreCase(upper) || key.replace("GENERIC_", "").equalsIgnoreCase(upper)) {
                    return a;
                }
            }
            if (a.name().equalsIgnoreCase(upper) || a.name().replace("GENERIC_", "").equalsIgnoreCase(upper)) {
                return a;
            }
        }
        return null;
    }

    private void handleAttribute(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_attribute");
            return;
        }

        String operation = args[1].toLowerCase();
        ItemMeta meta = item.getItemMeta();

        if (operation.equalsIgnoreCase("clear")) {
            for (Attribute attr : getAllAttributes()) {
                meta.removeAttributeModifier(attr);
            }
            saveItem(player, item, meta);
            msg(player, "attribute_clear_success");
            return;
        }

        if (args.length < 3) {
            msg(player, "usage_attribute_op");
            return;
        }

        String attrName = args[2].toUpperCase();
        Attribute attribute = getAttributeByName(attrName);

        if (attribute == null) {
            msg(player, "invalid_attribute");
            return;
        }

        if (operation.equalsIgnoreCase("add")) {
            if (args.length < 4) {
                msg(player, "usage_attribute_add");
                return;
            }
            double val;
            try {
                val = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                msg(player, "value_must_be_number");
                return;
            }
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "ItemEditAttribute",
                    val,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            meta.addAttributeModifier(attribute, modifier);
            msg(player, "attribute_add_success", "%attr%", attribute.name(), "%val%", val);
        } else if (operation.equalsIgnoreCase("remove")) {
            meta.removeAttributeModifier(attribute);
            msg(player, "attribute_remove_success", "%attr%", attribute.name());
        } else {
            msg(player, "unknown_operation");
            return;
        }
        saveItem(player, item, meta);
    }

    private void handleHideTooltips(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        List<ItemFlag> hideFlags = new ArrayList<>();
        boolean allHidden = true;
        for (ItemFlag flag : ItemFlag.values()) {
            if (flag.name().startsWith("HIDE_")) {
                hideFlags.add(flag);
                if (!meta.hasItemFlag(flag)) {
                    allHidden = false;
                }
            }
        }

        boolean hide;
        if (args.length >= 2) {
            hide = Boolean.parseBoolean(args[1]);
        } else {
            hide = !allHidden;
        }

        for (ItemFlag flag : hideFlags) {
            if (hide) {
                meta.addItemFlags(flag);
            } else {
                meta.removeItemFlags(flag);
            }
        }
        saveItem(player, item, meta);
        msg(player, "hidetooltips_success", "%state%", hide);
    }

    private void handleAbility(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            msg(player, "usage_ability");
            return;
        }

        String operation = args[1].toLowerCase();
        if (operation.equalsIgnoreCase("list")) {
            msg(player, "ability_list_header");
            for (Ability ability : plugin.getAbilityManager().getRegisteredAbilities()) {
                player.sendMessage("§e- " + ability.getId() + " §7(" + ability.getName() + "): " + ability.getDescription());
            }
            return;
        }

        if (operation.equalsIgnoreCase("clear")) {
            plugin.getAbilityManager().setItemAbilities(item, new ArrayList<>());
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            msg(player, "ability_clear_success");
            return;
        }

        if (args.length < 3) {
            msg(player, "usage_ability_op");
            return;
        }

        String abilityId = args[2].toLowerCase();
        List<String> current = new ArrayList<>(plugin.getAbilityManager().getItemAbilities(item));

        if (operation.equalsIgnoreCase("add")) {
            Ability ability = plugin.getAbilityManager().getAbility(abilityId);
            if (ability == null) {
                msg(player, "ability_not_exist", "%ability%", abilityId);
                return;
            }
            String canonicalId = ability.getId().toLowerCase();
            if (current.contains(canonicalId)) {
                msg(player, "ability_already_has");
                return;
            }
            current.add(canonicalId);
            plugin.getAbilityManager().setItemAbilities(item, current);
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            msg(player, "ability_add_success", "%ability%", ability.getName());
        } else if (operation.equalsIgnoreCase("remove")) {
            Ability ability = plugin.getAbilityManager().getAbility(abilityId);
            String targetId = ability != null ? ability.getId().toLowerCase() : abilityId;
            if (!current.contains(targetId)) {
                msg(player, "ability_not_has");
                return;
            }
            current.remove(targetId);
            plugin.getAbilityManager().setItemAbilities(item, current);
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            msg(player, "ability_remove_success", "%ability%", abilityId);
        } else {
            msg(player, "ability_unknown_op");
        }
    }

    private void handleCustom(Player player, ItemStack item, String[] args) {
        if (args.length < 4) {
            msg(player, "usage_custom");
            player.sendMessage("§7Example: /ie custom lava_spit cooldown 2.5");
            return;
        }

        String abilityId = args[1].toLowerCase();
        String param = args[2].toLowerCase();
        String valStr = args[3].toLowerCase();

        Ability ability = plugin.getAbilityManager().getAbility(abilityId);
        if (ability == null) {
            msg(player, "ability_not_exist", "%ability%", abilityId);
            return;
        }

        List<String> itemAbilities = plugin.getAbilityManager().getItemAbilities(item);
        if (!itemAbilities.contains(abilityId)) {
            player.sendMessage("§cWarning: The item does not currently have this ability bound, but parameter will be set.");
        }

        try {
            if (valStr.contains(".") || param.equalsIgnoreCase("cooldown") || param.equalsIgnoreCase("damage") || param.equalsIgnoreCase("health_heal")) {
                double val = Double.parseDouble(valStr);
                ability.setCustomParam(plugin, item, param, val);
                msg(player, "custom_success", "%param%", param, "%ability%", ability.getName(), "%val%", val);
            } else {
                int val = Integer.parseInt(valStr);
                ability.setCustomParam(plugin, item, param, val);
                msg(player, "custom_success", "%param%", param, "%ability%", ability.getName(), "%val%", val);
            }
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
        } catch (NumberFormatException e) {
            msg(player, "value_must_be_number");
        }
    }

    private void handleGive(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LanguageManager.getMessage(sender, "usage_give"));
            return;
        }

        Player target = sender;
        String weaponKey;

        if (args.length >= 3) {
            target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(LanguageManager.getMessage(sender, "player_not_found"));
                return;
            }
            weaponKey = args[2].toLowerCase();
        } else {
            weaponKey = args[1].toLowerCase();
        }

        org.bukkit.configuration.file.FileConfiguration weaponCfg = plugin.getWeaponConfigManager().getConfig();
        String path = "weapons." + weaponKey;
        if (!weaponCfg.contains(path)) {
            sender.sendMessage(LanguageManager.getMessage(sender, "give_not_found", "%weapon%", weaponKey));
            return;
        }

        String matStr = weaponCfg.getString(path + ".material", "BLAZE_ROD");
        org.bukkit.Material material = org.bukkit.Material.BLAZE_ROD;
        try {
            material = org.bukkit.Material.valueOf(matStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cWarning: Invalid material '" + matStr + "' in config. Using BLAZE_ROD.");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            sender.sendMessage("§cError: Could not generate item metadata.");
            return;
        }

        if (weaponCfg.contains(path + ".display-name")) {
            String dn = weaponCfg.getString(path + ".display-name");
            meta.displayName(parseText(dn));
        }

        if (weaponCfg.contains(path + ".lore")) {
            List<String> rawLore = weaponCfg.getStringList(path + ".lore");
            List<Component> parsedLore = rawLore.stream().map(this::parseText).collect(Collectors.toList());
            meta.lore(parsedLore);
        }

        if (weaponCfg.contains(path + ".custom-model-data")) {
            meta.setCustomModelData(weaponCfg.getInt(path + ".custom-model-data"));
        }

        NamespacedKey weaponKeyPdc = new NamespacedKey(plugin, "weapon_key");
        meta.getPersistentDataContainer().set(weaponKeyPdc, org.bukkit.persistence.PersistentDataType.STRING, weaponKey);
        item.setItemMeta(meta);

        List<String> abilities = weaponCfg.getStringList(path + ".abilities");
        if (abilities != null && !abilities.isEmpty()) {
            plugin.getAbilityManager().setItemAbilities(item, abilities);
        }

        target.getInventory().addItem(item);
        sender.sendMessage(LanguageManager.getMessage(sender, "give_success", "%weapon%", weaponKey, "%player%", target.getName()));
        if (!target.equals(sender)) {
            target.sendMessage(LanguageManager.getMessage(target, "give_success", "%weapon%", weaponKey, "%player%", target.getName())); // Note: Key can be reuse/adapted
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("rename", "lore", "enchant", "unbreakable", "flag", "attribute", "ability", "custom", "gui", "hidetooltips", "give", "reload"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "lore":
                    return filter(Arrays.asList("add", "set", "remove", "clear"), args[1]);
                case "enchant":
                    return filter(Arrays.stream(Enchantment.values()).map(e -> e.getKey().getKey()).collect(Collectors.toList()), args[1]);
                case "unbreakable":
                    return filter(Arrays.asList("true", "false"), args[1]);
                case "hidetooltips":
                    return filter(Arrays.asList("true", "false"), args[1]);
                case "flag":
                case "attribute":
                case "ability":
                    return filter(Arrays.asList("add", "remove", "clear", "list"), args[1]);
                case "custom":
                    List<String> customSugg = new ArrayList<>();
                    for (Ability ability : plugin.getAbilityManager().getRegisteredAbilities()) {
                        customSugg.add(ability.getId());
                        customSugg.add(ability.getClass().getSimpleName());
                    }
                    customSugg.add("gojo");
                    return filter(customSugg, args[1]);
                case "give":
                    List<String> suggestions = new ArrayList<>();
                    for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                    org.bukkit.configuration.file.FileConfiguration weaponCfg = plugin.getWeaponConfigManager().getConfig();
                    if (weaponCfg.contains("weapons")) {
                        org.bukkit.configuration.ConfigurationSection sec = weaponCfg.getConfigurationSection("weapons");
                        if (sec != null) {
                            suggestions.addAll(sec.getKeys(false));
                        }
                    }
                    return filter(suggestions, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String op = args[1].toLowerCase();
            if (sub.equalsIgnoreCase("flag")) {
                if (op.equalsIgnoreCase("add") || op.equalsIgnoreCase("remove")) {
                    return filter(Arrays.stream(ItemFlag.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
                }
            } else if (sub.equalsIgnoreCase("attribute")) {
                if (op.equalsIgnoreCase("add") || op.equalsIgnoreCase("remove")) {
                    List<String> names = getAllAttributes().stream()
                            .map(a -> {
                                if (a instanceof org.bukkit.Keyed) {
                                    return ((org.bukkit.Keyed) a).getKey().getKey().toUpperCase();
                                }
                                return a.name();
                            })
                             .collect(Collectors.toList());
                    return filter(names, args[2]);
                }
            } else if (sub.equalsIgnoreCase("ability")) {
                if (op.equalsIgnoreCase("add") || op.equalsIgnoreCase("remove")) {
                    List<String> suggestions = new ArrayList<>();
                    for (Ability ability : plugin.getAbilityManager().getRegisteredAbilities()) {
                        suggestions.add(ability.getId());
                        suggestions.add(ability.getClass().getSimpleName());
                    }
                    suggestions.add("gojo");
                    return filter(suggestions, args[2]);
                }
            } else if (sub.equalsIgnoreCase("custom")) {
                String abilityId = args[1].toLowerCase();
                Ability ability = plugin.getAbilityManager().getAbility(abilityId);
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(Arrays.asList("cooldown", "damage", "radius", "duration", "fire_ticks", "health_heal", "hunger_heal"));
                if (ability != null) {
                    String canonicalId = ability.getId().toLowerCase();
                    for (org.bukkit.configuration.file.FileConfiguration cfg : plugin.getAbilityConfigManager().getConfigs().values()) {
                        String path = "abilities." + canonicalId;
                        if (cfg.contains(path) && cfg.isConfigurationSection(path)) {
                            org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection(path);
                            if (sec != null) {
                                suggestions.addAll(sec.getKeys(false));
                            }
                        }
                    }
                }
                suggestions = suggestions.stream().distinct().collect(Collectors.toList());
                return filter(suggestions, args[2]);
            } else if (sub.equalsIgnoreCase("give")) {
                List<String> suggestions = new ArrayList<>();
                org.bukkit.configuration.file.FileConfiguration weaponCfg = plugin.getWeaponConfigManager().getConfig();
                if (weaponCfg.contains("weapons")) {
                    org.bukkit.configuration.ConfigurationSection sec = weaponCfg.getConfigurationSection("weapons");
                    if (sec != null) {
                        suggestions.addAll(sec.getKeys(false));
                    }
                }
                return filter(suggestions, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
