package com.itemedit.full.config;

import com.itemedit.full.ItemEditFull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class WeaponConfigManager {
    private final ItemEditFull plugin;
    private File file;
    private FileConfiguration config;

    public WeaponConfigManager(ItemEditFull plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        file = new File(plugin.getDataFolder(), "weapon.yml");
        if (!file.exists()) {
            if (plugin.getResource("weapon.yml") != null) {
                try {
                    plugin.saveResource("weapon.yml", false);
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not save weapon.yml from resources: " + e.getMessage());
                }
            } else {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create weapon.yml!");
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            setup();
        }
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save weapon.yml!");
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}
