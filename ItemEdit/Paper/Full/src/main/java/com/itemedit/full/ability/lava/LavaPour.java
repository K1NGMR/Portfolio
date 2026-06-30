package com.itemedit.full.ability.lava;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.itemedit.full.utils.CompatRunnable;

public class LavaPour extends Ability {
    private final ItemEditFull plugin;

    public LavaPour(ItemEditFull plugin) {
        super("lava_pour", "Lava Pour", "Places a temporary lava block that disappears after a few seconds.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage("§cNo block in range to place lava.");
            return false;
        }

        Block lavaBlock = targetBlock.getRelative(org.bukkit.block.BlockFace.UP);
        if (lavaBlock.getType() != Material.AIR) {
            player.sendMessage("§cCannot place lava here.");
            return false;
        }

        double duration = getDoubleParam(plugin, item, "duration", 5.0);

        Material originalMaterial = lavaBlock.getType();
        lavaBlock.setType(Material.LAVA);
        lavaBlock.getWorld().playSound(lavaBlock.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 1.0f, 1.0f);

        new CompatRunnable() {
            @Override
            public void run() {
                if (lavaBlock.getType() == Material.LAVA) {
                    lavaBlock.setType(originalMaterial);
                    lavaBlock.getWorld().playSound(lavaBlock.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, lavaBlock.getLocation(), (long) (duration * 20));

        return true;
    }
}
