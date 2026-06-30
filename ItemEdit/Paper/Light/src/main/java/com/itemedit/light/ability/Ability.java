package com.itemedit.light.ability;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Ability {
    private final String id;
    private final String name;
    private final String description;

    public Ability(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Executes the ability when a player right-clicks with the item.
     * @param player the player who triggered the ability
     * @param item the item containing the ability
     * @return true if the ability was successfully triggered (cooldown will be applied), false otherwise.
     */
    public abstract boolean trigger(Player player, ItemStack item);
}
