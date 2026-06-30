package com.itemedit.full;

import com.itemedit.full.ability.AbilityManager;
import com.itemedit.full.ability.lava.*;
import com.itemedit.full.command.ItemEditCommand;
import com.itemedit.full.config.WeaponConfigManager;
import com.itemedit.full.gui.ItemEditGui;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemEditFull extends JavaPlugin {
    private AbilityManager abilityManager;
    private ItemEditGui guiManager;
    private WeaponConfigManager weaponConfigManager;
    private com.itemedit.full.config.AbilityConfigManager abilityConfigManager;

    @Override
    public void onEnable() {
        // Save config
        saveDefaultConfig();

        // Initialize Ability Config Manager
        abilityConfigManager = new com.itemedit.full.config.AbilityConfigManager(this);
        abilityConfigManager.setup();

        // Initialize Weapon Config Manager
        weaponConfigManager = new WeaponConfigManager(this);
        weaponConfigManager.setup();

        // Initialize Ability Manager
        abilityManager = new AbilityManager(this);
        getServer().getPluginManager().registerEvents(abilityManager, this);

        // Initialize GUI Manager
        guiManager = new ItemEditGui(this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        // Register Abilities
        registerAbilities();

        // Register Command
        ItemEditCommand commandExecutor = new ItemEditCommand(this);
        getCommand("itemedit").setExecutor(commandExecutor);
        getCommand("itemedit").setTabCompleter(commandExecutor);

        getLogger().info("ItemEdit Paid (Full) version has been enabled!");
    }

    private void registerAbilities() {
        abilityManager.registerAbility(new LavaSpit(this));
        abilityManager.registerAbility(new LavaWalker(this));
        abilityManager.registerAbility(new HeatWave(this));
        abilityManager.registerAbility(new LavaPour(this));
        abilityManager.registerAbility(new LavaAbsorption(this));

        FireAura fireAura = new FireAura(this);
        abilityManager.registerAbility(fireAura);
        getServer().getPluginManager().registerEvents(fireAura, this);

        // Register all grouped abilities (115 premium + base)
        com.itemedit.full.ability.warden.WardenAbilities.register(this);
        com.itemedit.full.ability.undead.ZombieAbilities.register(this);
        com.itemedit.full.ability.undead.SkeletonAbilities.register(this);
        com.itemedit.full.ability.end.EndAbilities.register(this);
        com.itemedit.full.ability.nether.NetherAbilities.register(this);
        com.itemedit.full.ability.general.GeneralAbilities.register(this);
        com.itemedit.full.ability.general.OverworldAbilities.register(this);
        com.itemedit.full.ability.general.MobAbilities.register(this);
        com.itemedit.full.ability.general.NewExpansionAbilities.register(this);
        com.itemedit.full.ability.general.MoreExpansionAbilities.register(this);
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public ItemEditGui getGuiManager() {
        return guiManager;
    }

    public WeaponConfigManager getWeaponConfigManager() {
        return weaponConfigManager;
    }

    public com.itemedit.full.config.AbilityConfigManager getAbilityConfigManager() {
        return abilityConfigManager;
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemEdit Paid (Full) version has been disabled!");
    }
}
