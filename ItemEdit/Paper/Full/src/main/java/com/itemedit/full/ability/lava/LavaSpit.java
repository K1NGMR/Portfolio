package com.itemedit.full.ability.lava;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;

public class LavaSpit extends Ability {
    private final ItemEditFull plugin;

    public LavaSpit(ItemEditFull plugin) {
        super("lava_spit", "Lava Spit", "Launches a fireball that burns enemies.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        double yield = getDoubleParam(plugin, item, "yield", 1.0);
        boolean incendiary = getBooleanParam(plugin, item, "incendiary", true);
        fireball.setYield((float) yield);
        fireball.setIsIncendiary(incendiary);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        return true;
    }
}
