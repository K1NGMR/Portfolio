package com.itemedit.light.ability.lava;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;

public class LavaSpit extends Ability {
    private final ItemEditLight plugin;

    public LavaSpit(ItemEditLight plugin) {
        super("lava_spit", "Lava Spit", "Launches a fireball that burns enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.setYield(1.0f);
        fireball.setIsIncendiary(true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        return true;
    }
}
