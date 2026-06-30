package com.itemedit.light;

import com.itemedit.light.ability.AbilityManager;
import com.itemedit.light.ability.lava.*;
import com.itemedit.light.command.ItemEditCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemEditLight extends JavaPlugin {
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        // Save config
        saveDefaultConfig();

        // Initialize Ability Manager
        abilityManager = new AbilityManager(this);
        getServer().getPluginManager().registerEvents(abilityManager, this);

        // Register Abilities
        registerAbilities();

        // Register Command
        ItemEditCommand commandExecutor = new ItemEditCommand(this);
        getCommand("itemedit").setExecutor(commandExecutor);
        getCommand("itemedit").setTabCompleter(commandExecutor);

        getLogger().info("ItemEdit Light has been enabled!");
    }

    private void registerAbilities() {
        abilityManager.registerAbility(new LavaSpit(this));
        abilityManager.registerAbility(new LavaWalker(this));
        abilityManager.registerAbility(new HeatWave(this));
        abilityManager.registerAbility(new LavaPour(this));

        FireAura fireAura = new FireAura(this);
        abilityManager.registerAbility(fireAura);
        getServer().getPluginManager().registerEvents(fireAura, this);

        // Register new grouped abilities (45)
        com.itemedit.light.ability.warden.WardenAbilities.register(this);
        com.itemedit.light.ability.undead.ZombieAbilities.register(this);
        com.itemedit.light.ability.undead.SkeletonAbilities.register(this);
        com.itemedit.light.ability.end.EndAbilities.register(this);
        com.itemedit.light.ability.nether.NetherAbilities.register(this);
        com.itemedit.light.ability.general.GeneralAbilities.register(this);
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemEdit Light has been disabled!");
    }
}
