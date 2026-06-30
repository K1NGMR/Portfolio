package com.liquade.inventoryinsurance;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

public class InsuranceCommand implements CommandExecutor, TabCompleter {
    private final InventoryInsurancePlugin plugin;
    private final InsuranceManager manager;

    public InsuranceCommand(InventoryInsurancePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getInsuranceManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "buy":
            case "purchase":
                handleBuy(sender, args);
                break;
            case "claim":
                handleClaim(sender);
                break;
            case "status":
            case "info":
                handleStatus(sender);
                break;
            case "remove":
            case "cancel":
                handleRemove(sender);
                break;
            case "balance":
            case "bal":
                handleBalance(sender, args);
                break;
            case "pay":
                handlePay(sender, args);
                break;
            case "eco":
                handleEco(sender, args);
                break;
            case "listpapi":
                handleListPapi(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Type /insurance for help.");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== InventoryInsurance Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/insurance buy <tier> " + ChatColor.WHITE + "- Buy or renew inventory insurance.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance claim " + ChatColor.WHITE + "- Retrieve insured items where you stand.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance remove " + ChatColor.WHITE + "- Cancel your current active insurance.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance status " + ChatColor.WHITE + "- Check your active insurance & cooldowns.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance balance [player] " + ChatColor.WHITE + "- Check internal economy balance.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance pay <player> <amount> " + ChatColor.WHITE + "- Send money to another player.");
        sender.sendMessage(ChatColor.YELLOW + "/insurance listpapi " + ChatColor.WHITE + "- List all PlaceholderAPI placeholders.");
        if (sender.hasPermission("inventoryinsurance.admin")) {
            sender.sendMessage(ChatColor.RED + "/insurance eco <give/take/set/reset> <player> <amount> " + ChatColor.WHITE + "- Manage player balance.");
            sender.sendMessage(ChatColor.RED + "/insurance reload " + ChatColor.WHITE + "- Reload plugin config.");
        }
        sender.sendMessage(ChatColor.GOLD + "==================================");
    }

    private void handleBuy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can buy insurance!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            String availableTiers = String.join(", ", manager.getTiers().keySet());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tiers}", availableTiers);
            manager.sendMessage(player, "invalid-tier", placeholders);
            return;
        }

        String tierStr = args[1].toLowerCase();
        TierConfig tierConfig = manager.getTierConfig(tierStr);
        if (tierConfig == null) {
            String availableTiers = String.join(", ", manager.getTiers().keySet());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tiers}", availableTiers);
            manager.sendMessage(player, "invalid-tier", placeholders);
            return;
        }

        // Custom permissions check per tier
        if (!tierConfig.getPermission().isEmpty() && !player.hasPermission(tierConfig.getPermission())) {
            manager.sendMessage(player, "no-permission", null);
            return;
        }

        double price = tierConfig.getPrice();
        Economy econ = plugin.getEconomy();

        if (econ != null) {
            if (!econ.has(player, price)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{price}", econ.format(price));
                placeholders.put("{balance}", econ.format(econ.getBalance(player)));
                manager.sendMessage(player, "insufficient-funds", placeholders);
                return;
            }
        }

        PlayerInsurance pi = manager.getOrCreatePlayerInsurance(player.getUniqueId());
        String currentTierId = pi.getTier();

        long durationMs = tierConfig.getDurationDays() * 24L * 60L * 60L * 1000L;
        long newExpiry;
        boolean isRenew = currentTierId.equalsIgnoreCase(tierConfig.getId());

        if (isRenew) {
            // Stack the duration
            long baseTime = pi.getExpiryTime() > System.currentTimeMillis() ? pi.getExpiryTime() : System.currentTimeMillis();
            newExpiry = baseTime + durationMs;
        } else {
            // Upgrade/change tier
            newExpiry = System.currentTimeMillis() + durationMs;
        }

        if (econ != null) {
            EconomyResponse response = econ.withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{error}", response.errorMessage);
                manager.sendMessage(player, "transaction-failed", placeholders);
                return;
            }
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tier}", tierConfig.getDisplayName());
            placeholders.put("{price}", econ.format(price));
            manager.sendMessage(player, isRenew ? "renew-success" : "purchase-success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tier}", tierConfig.getDisplayName());
            manager.sendMessage(player, isRenew ? "renew-success-free" : "purchase-success-free", placeholders);
        }

        pi.setTier(tierConfig.getId());
        pi.setExpiryTime(newExpiry);
        manager.saveData();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{date}", sdf.format(new Date(newExpiry)));
        placeholders.put("{days}", String.valueOf(tierConfig.getDurationDays()));
        manager.sendMessage(player, "expiry-date", placeholders);

        String pricePaid = (econ != null) ? econ.format(price) : "Free (No Economy)";
        plugin.getHistoryLogger().log(String.format(
                "Purchase: Player %s (%s) got %s insurance. Paid: %s. Expires: %s",
                player.getName(),
                player.getUniqueId(),
                tierConfig.getId(),
                pricePaid,
                sdf.format(new Date(newExpiry))
        ));
    }

    private void handleClaim(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("inventoryinsurance.use")) {
            manager.sendMessage(player, "no-permission", null);
            return;
        }

        PlayerInsurance insurance = manager.getOrCreatePlayerInsurance(player.getUniqueId());
        List<PlayerClaimData> claims = insurance.getPendingClaims();

        if (claims.isEmpty()) {
            manager.sendMessage(player, "no-claims-error", null);
            return;
        }

        // Check cooldown
        long cooldownMs = manager.getClaimCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - insurance.getLastClaimTime();
        if (elapsed < cooldownMs) {
            long remaining = cooldownMs - elapsed;
            long seconds = (remaining / 1000) % 60;
            long minutes = (remaining / (1000 * 60)) % 60;
            long hours = (remaining / (1000 * 60 * 60));

            String timeStr;
            if (hours > 0) {
                timeStr = String.format("%d hours, %d minutes", hours, minutes);
            } else if (minutes > 0) {
                timeStr = String.format("%d minutes, %d seconds", minutes, seconds);
            } else {
                timeStr = String.format("%d seconds", seconds);
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{time}", timeStr);
            manager.sendMessage(player, "claim-cooldown-error", placeholders);
            return;
        }

        List<ItemStack> dropped = new ArrayList<>();
        int totalItemsRestored = 0;
        int totalXpLevels = 0;

        for (PlayerClaimData claim : claims) {
            totalItemsRestored += claim.getItems().size();
            totalXpLevels += claim.getXpLevel();

            // Restore items to their exact slots
            for (Map.Entry<Integer, ItemStack> entry : claim.getItems().entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();
                if (item == null || item.getType().isAir()) continue;

                ItemStack current = getInventoryItem(player, slot);
                if (current == null || current.getType().isAir()) {
                    setInventoryItem(player, slot, item);
                } else {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
                    if (!left.isEmpty()) {
                        dropped.addAll(left.values());
                    }
                }
            }

            // XP restoration
            if (claim.getXpLevel() > 0 || claim.getXpProgress() > 0.0f) {
                int levelAddition = claim.getXpLevel();
                float currentExp = player.getExp();
                float addedExp = claim.getXpProgress();
                float finalExp = currentExp + addedExp;

                if (finalExp >= 1.0f) {
                    levelAddition += 1;
                    finalExp -= 1.0f;
                }

                // Keep exp progress safely between 0.0 and 0.999
                if (finalExp < 0.0f) finalExp = 0.0f;
                if (finalExp >= 1.0f) finalExp = 0.999f;

                player.setLevel(player.getLevel() + levelAddition);
                player.setExp(finalExp);
            }
        }

        // Drop leftover items on the ground
        if (!dropped.isEmpty()) {
            for (ItemStack left : dropped) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            manager.sendMessage(player, "claim-items-dropped", null);
        }

        // Update last claim timestamp and clear pending claims
        insurance.setLastClaimTime(System.currentTimeMillis());
        claims.clear();
        manager.saveData();

        // Log to history file
        plugin.getHistoryLogger().log(String.format(
                "Claim: Player %s (%s) claimed items at [%s, %.2f, %.2f, %.2f]. Restored %d items, %d levels XP.",
                player.getName(),
                player.getUniqueId(),
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                totalItemsRestored,
                totalXpLevels
        ));

        manager.sendMessage(player, "claim-success", null);
    }

    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have an insurance status!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("inventoryinsurance.use")) {
            manager.sendMessage(player, "no-permission", null);
            return;
        }

        PlayerInsurance pi = manager.getOrCreatePlayerInsurance(player.getUniqueId());
        String tierId = pi.getTier();
        Economy econ = plugin.getEconomy();

        manager.sendMessage(player, "status-header", null);
        if (tierId.equalsIgnoreCase("none")) {
            manager.sendMessage(player, "status-uninsured", null);
        } else {
            TierConfig tierConfig = manager.getTierConfig(tierId);
            String displayName = tierConfig != null ? tierConfig.getDisplayName() : tierId;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tier}", displayName);
            placeholders.put("{date}", formatDate(pi.getExpiryTime()));
            placeholders.put("{time}", formatTimeRemaining(pi.getExpiryTime()));
            manager.sendMessage(player, "status-tier", placeholders);
            manager.sendMessage(player, "status-expires", placeholders);
            manager.sendMessage(player, "status-remaining", placeholders);
        }

        if (econ != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{balance}", econ.format(econ.getBalance(player)));
            manager.sendMessage(player, "status-balance", placeholders);
        }

        // Claims info
        int pending = pi.getPendingClaims().size();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{claims}", String.valueOf(pending));
        manager.sendMessage(player, "status-pending", placeholders);

        long cooldownMs = manager.getClaimCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - pi.getLastClaimTime();
        if (elapsed < cooldownMs) {
            Map<String, String> cdPlaceholders = new HashMap<>();
            cdPlaceholders.put("{time}", formatTimeRemaining(pi.getLastClaimTime() + cooldownMs));
            manager.sendMessage(player, "status-cooldown-active", cdPlaceholders);
        } else {
            manager.sendMessage(player, "status-cooldown-ready", null);
        }
        manager.sendMessage(player, "status-footer", null);
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have an active insurance plan to remove!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("inventoryinsurance.use")) {
            manager.sendMessage(player, "no-permission", null);
            return;
        }

        PlayerInsurance pi = manager.getOrCreatePlayerInsurance(player.getUniqueId());
        String tierId = pi.getTier();

        if (tierId.equalsIgnoreCase("none")) {
            manager.sendMessage(player, "remove-no-active", null);
            return;
        }

        TierConfig tierConfig = manager.getTierConfig(tierId);
        String displayName = tierConfig != null ? tierConfig.getDisplayName() : tierId;

        pi.setTier("none");
        pi.setExpiryTime(0);
        manager.saveData();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{tier}", displayName);
        manager.sendMessage(player, "remove-success", placeholders);
        plugin.getHistoryLogger().log(String.format("Cancel: Player %s (%s) cancelled their %s insurance.", player.getName(), player.getUniqueId(), tierId));
    }

    private void handleBalance(CommandSender sender, String[] args) {
        Economy econ = plugin.getEconomy();
        if (econ == null) {
            sender.sendMessage(ChatColor.RED + "Economy support is currently unavailable.");
            return;
        }

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /insurance balance <player>");
                return;
            }
            Player player = (Player) sender;
            sender.sendMessage(ChatColor.GOLD + "[InventoryInsurance] " + ChatColor.YELLOW + "Your balance: " + ChatColor.GREEN + econ.format(econ.getBalance(player)));
        } else {
            if (!sender.hasPermission("inventoryinsurance.admin")) {
                manager.sendMessage(sender, "no-permission", null);
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "[InventoryInsurance] " + ChatColor.YELLOW + target.getName() + "'s balance: " + ChatColor.GREEN + econ.format(econ.getBalance(target)));
        }
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can pay other players!");
            return;
        }

        Player player = (Player) sender;
        Economy econ = plugin.getEconomy();
        if (econ == null) {
            sender.sendMessage(ChatColor.RED + "Economy support is currently unavailable.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /insurance pay <player> <amount>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot pay yourself!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount format.");
            return;
        }

        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive.");
            return;
        }

        if (!econ.has(player, amount)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds! You have " + econ.format(econ.getBalance(player)));
            return;
        }

        EconomyResponse withdrawRep = econ.withdrawPlayer(player, amount);
        if (!withdrawRep.transactionSuccess()) {
            player.sendMessage(ChatColor.RED + "Payment failed: " + withdrawRep.errorMessage);
            return;
        }

        EconomyResponse depositRep = econ.depositPlayer(target, amount);
        if (!depositRep.transactionSuccess()) {
            econ.depositPlayer(player, amount);
            player.sendMessage(ChatColor.RED + "Payment failed: Could not deposit to recipient.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Sent " + ChatColor.YELLOW + econ.format(amount) + ChatColor.GREEN + " to " + ChatColor.YELLOW + target.getName());
        if (target.isOnline()) {
            Player targetOnline = target.getPlayer();
            if (targetOnline != null) {
                targetOnline.sendMessage(ChatColor.GREEN + "Received " + ChatColor.YELLOW + econ.format(amount) + ChatColor.GREEN + " from " + ChatColor.YELLOW + player.getName());
            }
        }
    }

    private void handleEco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("inventoryinsurance.admin")) {
            manager.sendMessage(sender, "no-permission", null);
            return;
        }

        Economy econ = plugin.getEconomy();
        if (econ == null) {
            sender.sendMessage(ChatColor.RED + "Economy support is currently unavailable.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /insurance eco <give/take/set/reset> <player> [amount]");
            return;
        }

        String action = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        double amount = 0.0;
        if (!action.equals("reset")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Please specify an amount.");
                return;
            }
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount format.");
                return;
            }
            if (amount < 0.0) {
                sender.sendMessage(ChatColor.RED + "Amount must be positive.");
                return;
            }
        }

        switch (action) {
            case "give":
                econ.depositPlayer(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.YELLOW + econ.format(amount) + ChatColor.GREEN + " to " + ChatColor.YELLOW + target.getName() + ".");
                break;
            case "take":
                econ.withdrawPlayer(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Took " + ChatColor.YELLOW + econ.format(amount) + ChatColor.GREEN + " from " + ChatColor.YELLOW + target.getName() + ".");
                break;
            case "set":
                double curBal = econ.getBalance(target);
                if (curBal > amount) {
                    econ.withdrawPlayer(target, curBal - amount);
                } else if (curBal < amount) {
                    econ.depositPlayer(target, amount - curBal);
                }
                sender.sendMessage(ChatColor.GREEN + "Set balance of " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " to " + ChatColor.YELLOW + econ.format(amount) + ChatColor.GREEN + ".");
                break;
            case "reset":
                double curBalReset = econ.getBalance(target);
                double initial = manager.getInitialBalance();
                if (curBalReset > initial) {
                    econ.withdrawPlayer(target, curBalReset - initial);
                } else if (curBalReset < initial) {
                    econ.depositPlayer(target, initial - curBalReset);
                }
                sender.sendMessage(ChatColor.GREEN + "Reset balance of " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " to default: " + ChatColor.YELLOW + econ.format(initial) + ChatColor.GREEN + ".");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown action. Available: give, take, set, reset");
                break;
        }
    }

    private void handleListPapi(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== InventoryInsurance Placeholders ===");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_tier%" + ChatColor.WHITE + " - Current active insurance tier");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_tier_raw%" + ChatColor.WHITE + " - Raw ID of active insurance tier");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_expires%" + ChatColor.WHITE + " - Time remaining until insurance expires");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_expires_date%" + ChatColor.WHITE + " - Exact date/time of insurance expiration");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_has_insurance%" + ChatColor.WHITE + " - Whether player is insured (true/false)");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_cooldown%" + ChatColor.WHITE + " - Cooldown remaining before claiming");
        sender.sendMessage(ChatColor.YELLOW + "%inventoryinsurance_pending_claims%" + ChatColor.WHITE + " - Count of claims waiting to be redeemed");
        sender.sendMessage(ChatColor.GOLD + "=======================================");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("inventoryinsurance.admin")) {
            manager.sendMessage(sender, "no-permission", null);
            return;
        }

        manager.loadConfigSettings();
        manager.loadData();
        manager.sendMessage(sender, "reload-success", null);
    }

    private ItemStack getInventoryItem(Player player, int slot) {
        if (slot >= 0 && slot <= 35) {
            return player.getInventory().getItem(slot);
        } else if (slot == 36) {
            return player.getInventory().getBoots();
        } else if (slot == 37) {
            return player.getInventory().getLeggings();
        } else if (slot == 38) {
            return player.getInventory().getChestplate();
        } else if (slot == 39) {
            return player.getInventory().getHelmet();
        } else if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return null;
    }

    private void setInventoryItem(Player player, int slot, ItemStack item) {
        if (slot >= 0 && slot <= 35) {
            player.getInventory().setItem(slot, item);
        } else if (slot == 36) {
            player.getInventory().setBoots(item);
        } else if (slot == 37) {
            player.getInventory().setLeggings(item);
        } else if (slot == 38) {
            player.getInventory().setChestplate(item);
        } else if (slot == 39) {
            player.getInventory().setHelmet(item);
        } else if (slot == 40) {
            player.getInventory().setItemInOffHand(item);
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Never";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String formatTimeRemaining(long timestamp) {
        long remaining = timestamp - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / (1000 * 60)) % 60;
        long hours = (remaining / (1000 * 60 * 60)) % 24;
        long days = remaining / (1000 * 60 * 60 * 24);

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("buy");
            list.add("claim");
            list.add("status");
            list.add("remove");
            list.add("balance");
            list.add("pay");
            list.add("listpapi");
            if (sender.hasPermission("inventoryinsurance.admin")) {
                list.add("eco");
                list.add("reload");
            }
            return filterList(list, args[0]);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("buy") || sub.equals("purchase")) {
                list.addAll(manager.getTiers().keySet());
                return filterList(list, args[1]);
            } else if (sub.equals("eco")) {
                list.add("give");
                list.add("take");
                list.add("set");
                list.add("reset");
                return filterList(list, args[1]);
            } else if (sub.equals("pay") || sub.equals("balance") || sub.equals("bal")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
                return filterList(list, args[1]);
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("eco")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
                return filterList(list, args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterList(List<String> raw, String prefix) {
        if (prefix == null || prefix.isEmpty()) return raw;
        List<String> result = new ArrayList<>();
        for (String s : raw) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
