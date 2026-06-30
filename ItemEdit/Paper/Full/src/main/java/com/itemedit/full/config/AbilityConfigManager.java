package com.itemedit.full.config;

import com.itemedit.full.ItemEditFull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AbilityConfigManager {
    private final ItemEditFull plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();
    private final String[] categories = {
        "lava", "warden", "undead", "end", "nether", "general", "overworld", "mob", "expansion"
    };

    public AbilityConfigManager(ItemEditFull plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        File dir = new File(plugin.getDataFolder(), "abilities");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (String category : categories) {
            File file = new File(dir, category + ".yml");
            if (!file.exists()) {
                // Attempt to copy from resources if exists, otherwise create new empty file
                if (plugin.getResource("abilities/" + category + ".yml") != null) {
                    try {
                        plugin.saveResource("abilities/" + category + ".yml", false);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not save abilities/" + category + ".yml from resources: " + e.getMessage());
                    }
                } else {
                    try {
                        file.createNewFile();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not create abilities/" + category + ".yml: " + e.getMessage());
                    }
                }
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(category, config);
            files.put(category, file);
        }
    }

    public FileConfiguration getCategoryConfig(String category) {
        return configs.get(category.toLowerCase());
    }

    public void reload() {
        configs.clear();
        files.clear();
        setup();
    }

    public Object getParam(String abilityId, String param) {
        String path = "abilities." + abilityId + "." + param;
        for (FileConfiguration cfg : configs.values()) {
            if (cfg.contains(path)) {
                return cfg.get(path);
            }
        }
        return null;
    }

    public Map<String, FileConfiguration> getConfigs() {
        return configs;
    }
}
