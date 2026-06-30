package com.liquade.inventoryinsurance;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class InsuranceManager {
    private final InventoryInsurancePlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    private final Map<String, PlayerInsurance> playerInsurances = new ConcurrentHashMap<>();
    private final Map<String, TierConfig> tiers = new ConcurrentHashMap<>();
    
    // Config values
    private long claimCooldownSeconds;
    private final Set<String> blacklistedItems = Collections.synchronizedSet(new HashSet<>());
    private double initialBalance;
    private String currencySymbol;
    private String currencyNameSingular;
    private String currencyNamePlural;
    private boolean economyEnabled;

    public InsuranceManager(InventoryInsurancePlugin plugin) {
        this.plugin = plugin;
        File dataDir = plugin.getDataFolder();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.dataFile = new File(dataDir, "players.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players.yml data file", e);
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public synchronized void loadConfigSettings() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.claimCooldownSeconds = config.getLong("claim-cooldown", 3600);

        this.blacklistedItems.clear();
        List<String> list = config.getStringList("blacklisted-items");
        for (String item : list) {
            if (item != null) {
                this.blacklistedItems.add(item.toUpperCase().trim());
            }
        }

        this.initialBalance = config.getDouble("economy.initial-balance", 1000.0);
        this.currencySymbol = config.getString("economy.currency-symbol", "$");
        this.currencyNameSingular = config.getString("economy.currency-name-singular", "Dollar");
        this.currencyNamePlural = config.getString("economy.currency-name-plural", "Dollars");
        this.economyEnabled = config.getBoolean("economy.enabled", true);

        // Load Tiers
        this.tiers.clear();
        org.bukkit.configuration.ConfigurationSection tiersSec = config.getConfigurationSection("tiers");
        if (tiersSec != null) {
            for (String key : tiersSec.getKeys(false)) {
                String path = "tiers." + key;
                String displayName = config.getString(path + ".display-name", key);
                double price = config.getDouble(path + ".price", 0.0);
                int durationDays = config.getInt(path + ".duration-days", 7);
                boolean saveHotbar = config.getBoolean(path + ".save-hotbar", true);
                boolean saveInventory = config.getBoolean(path + ".save-inventory", true);
                boolean saveArmor = config.getBoolean(path + ".save-armor", true);
                boolean saveOffhand = config.getBoolean(path + ".save-offhand", true);
                boolean saveXp = config.getBoolean(path + ".save-xp", true);
                int xpRestorePercentage = config.getInt(path + ".xp-restore-percentage", 100);
                String permission = config.getString(path + ".permission", "");

                TierConfig tier = new TierConfig(key, displayName, price, durationDays,
                        saveHotbar, saveInventory, saveArmor, saveOffhand, saveXp,
                        xpRestorePercentage, permission);
                this.tiers.put(key.toLowerCase(), tier);
            }
        }
    }

    public synchronized void loadData() {
        playerInsurances.clear();
        if (!dataFile.exists()) {
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        org.bukkit.configuration.ConfigurationSection section = dataConfig.getConfigurationSection("players");
        if (section == null) return;
        
        for (String uuidStr : section.getKeys(false)) {
            String path = "players." + uuidStr;
            PlayerInsurance insurance = new PlayerInsurance(uuidStr);
            insurance.setTier(section.getString(path + ".tier", "none"));
            insurance.setExpiryTime(section.getLong(path + ".expiry", 0));
            insurance.setLastClaimTime(section.getLong(path + ".last_claim", 0));
            insurance.setBalance(section.getDouble(path + ".balance", initialBalance));

            List<?> claimsList = section.getList(path + ".claims");
            if (claimsList != null) {
                for (Object obj : claimsList) {
                    if (obj instanceof Map) {
                        Map<?, ?> claimMap = (Map<?, ?>) obj;
                        
                        Object timeObj = claimMap.get("death_time");
                        long deathTime = timeObj instanceof Number ? ((Number) timeObj).longValue() : 0L;
                        
                        Object worldObj = claimMap.get("death_world");
                        String deathWorld = worldObj instanceof String ? (String) worldObj : "world";
                        
                        Object xObj = claimMap.get("death_x");
                        double deathX = xObj instanceof Number ? ((Number) xObj).doubleValue() : 0.0;
                        
                        Object yObj = claimMap.get("death_y");
                        double deathY = yObj instanceof Number ? ((Number) yObj).doubleValue() : 0.0;
                        
                        Object zObj = claimMap.get("death_z");
                        double deathZ = zObj instanceof Number ? ((Number) zObj).doubleValue() : 0.0;
                        
                        Object xpLvlObj = claimMap.get("xp_level");
                        int xpLevel = xpLvlObj instanceof Number ? ((Number) xpLvlObj).intValue() : 0;
                        
                        Object xpProgObj = claimMap.get("xp_progress");
                        float xpProgress = xpProgObj instanceof Number ? ((Number) xpProgObj).floatValue() : 0.0f;

                        Map<Integer, ItemStack> items = new HashMap<>();
                        Object itemsRaw = claimMap.get("items");
                        if (itemsRaw instanceof Map) {
                            Map<?, ?> itemsMap = (Map<?, ?>) itemsRaw;
                            for (Map.Entry<?, ?> entry : itemsMap.entrySet()) {
                                try {
                                    int slot = Integer.parseInt(entry.getKey().toString());
                                    Object itemObj = entry.getValue();
                                    if (itemObj instanceof ItemStack) {
                                        items.put(slot, (ItemStack) itemObj);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }

                        PlayerClaimData claim = new PlayerClaimData(items, xpLevel, xpProgress,
                                deathTime, deathWorld, deathX, deathY, deathZ);
                        insurance.getPendingClaims().add(claim);
                    }
                }
            }
            playerInsurances.put(uuidStr, insurance);
        }
    }

    public synchronized void saveData() {
        try {
            dataConfig.set("players", null); // Reset structure to avoid leftover keys
            for (PlayerInsurance insurance : playerInsurances.values()) {
                String uuid = insurance.getUuid();
                String path = "players." + uuid;
                dataConfig.set(path + ".tier", insurance.getTier());
                dataConfig.set(path + ".expiry", insurance.getExpiryTime());
                dataConfig.set(path + ".last_claim", insurance.getLastClaimTime());
                dataConfig.set(path + ".balance", insurance.getBalance());

                List<Map<String, Object>> claimsList = new ArrayList<>();
                for (PlayerClaimData claim : insurance.getPendingClaims()) {
                    Map<String, Object> claimMap = new HashMap<>();
                    claimMap.put("death_time", claim.getDeathTime());
                    claimMap.put("death_world", claim.getDeathWorld());
                    claimMap.put("death_x", claim.getDeathX());
                    claimMap.put("death_y", claim.getDeathY());
                    claimMap.put("death_z", claim.getDeathZ());
                    claimMap.put("xp_level", claim.getXpLevel());
                    claimMap.put("xp_progress", (double) claim.getXpProgress());
                    Map<String, Object> serializedItems = new HashMap<>();
                    for (Map.Entry<Integer, ItemStack> entry : claim.getItems().entrySet()) {
                        serializedItems.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    claimMap.put("items", serializedItems);
                    claimsList.add(claimMap);
                }
                dataConfig.set(path + ".claims", claimsList);
            }
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player insurance data", e);
        }
    }

    public synchronized PlayerInsurance getOrCreatePlayerInsurance(UUID uuid) {
        String uuidStr = uuid.toString();
        PlayerInsurance insurance = playerInsurances.get(uuidStr);
        if (insurance == null) {
            insurance = new PlayerInsurance(uuidStr);
            insurance.setBalance(initialBalance);
            playerInsurances.put(uuidStr, insurance);
        }
        return insurance;
    }

    public void sendMessage(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String raw = getRawMessage(messageKey, placeholders);
        if (raw == null || raw.isEmpty()) return;
        recipient.sendMessage(raw);
    }

    public String getRawMessage(String messageKey, Map<String, String> placeholders) {
        String raw = plugin.getConfig().getString("messages." + messageKey);
        if (raw == null) return "";
        
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        raw = raw.replace("{prefix}", prefix);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public synchronized boolean isBlacklisted(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return blacklistedItems.contains(item.getType().name());
    }

    public synchronized long getClaimCooldownSeconds() {
        return claimCooldownSeconds;
    }

    public synchronized Set<String> getBlacklistedItems() {
        return Collections.unmodifiableSet(blacklistedItems);
    }

    public synchronized double getInitialBalance() {
        return initialBalance;
    }

    public synchronized String getCurrencySymbol() {
        return currencySymbol;
    }

    public synchronized String getCurrencyNameSingular() {
        return currencyNameSingular;
    }

    public synchronized String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    public synchronized boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public synchronized Map<String, TierConfig> getTiers() {
        return Collections.unmodifiableMap(tiers);
    }

    public synchronized TierConfig getTierConfig(String id) {
        if (id == null) return null;
        return tiers.get(id.toLowerCase());
    }
}
