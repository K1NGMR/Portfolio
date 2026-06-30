package com.liquade.inventoryinsurance;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryInsurancePlugin extends JavaPlugin {
    private InsuranceManager insuranceManager;
    private HistoryLogger historyLogger;

    @Override
    public void onEnable() {
        // Save default config.yml if not already present
        saveDefaultConfig();

        // Initialize history logger
        this.historyLogger = new HistoryLogger(this);

        // Setup Vault check
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin was not found! This plugin requires Vault to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize manager and load data
        this.insuranceManager = new InsuranceManager(this);
        this.insuranceManager.loadConfigSettings();
        this.insuranceManager.loadData();

        // Register built-in economy fallback if enabled and no other provider is registered yet
        if (insuranceManager.isEconomyEnabled()) {
            if (getEconomy() == null) {
                getLogger().info("No Vault economy provider (like EssentialsX, CMI, etc.) was found! Registering built-in internal economy...");
                registerInternalEconomy();
            }
        }

        // Register event listener
        getServer().getPluginManager().registerEvents(new InsuranceListener(this), this);

        // Register commands
        InsuranceCommand insCmd = new InsuranceCommand(this);
        getCommand("insurance").setExecutor(insCmd);
        getCommand("insurance").setTabCompleter(insCmd);

        // Setup PlaceholderAPI Expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new InsurancePlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI found! Registered expansion placeholders.");
        }

        getLogger().info("InventoryInsurance has been successfully enabled!");
    }

    private void registerInternalEconomy() {
        InternalEconomy internalEco = new InternalEconomy(this);
        getServer().getServicesManager().register(Economy.class, internalEco, this, org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (insuranceManager != null) {
            // Save data on plugin disable / reload
            insuranceManager.saveData();
        }
        getLogger().info("InventoryInsurance has been disabled.");
    }

    public Economy getEconomy() {
        if (insuranceManager == null || !insuranceManager.isEconomyEnabled()) {
            return null;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return (rsp != null) ? rsp.getProvider() : null;
    }

    public InsuranceManager getInsuranceManager() {
        return insuranceManager;
    }

    public HistoryLogger getHistoryLogger() {
        return historyLogger;
    }
}
