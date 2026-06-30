package com.liquade.inventoryinsurance;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InsuranceListener implements Listener {
    private final InventoryInsurancePlugin plugin;
    private final InsuranceManager manager;

    public InsuranceListener(InventoryInsurancePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getInsuranceManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerInsurance insurance = manager.getOrCreatePlayerInsurance(player.getUniqueId());
        String activeTierId = insurance.getTier();

        // If player has no active insurance, do nothing
        if (activeTierId == null || activeTierId.equalsIgnoreCase("none")) {
            return;
        }

        TierConfig tier = manager.getTierConfig(activeTierId);
        if (tier == null) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> insuredItems = new HashMap<>();
        int insuredXpLevel = 0;
        float insuredXpProgress = 0.0f;

        // 1. Save hotbar (slots 0 to 8)
        if (tier.isSaveHotbar()) {
            for (int i = 0; i <= 8; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    if (manager.isBlacklisted(item)) {
                        continue;
                    }
                    insuredItems.put(i, item.clone());
                }
            }
        }

        // 2. Save general inventory (slots 9 to 35)
        if (tier.isSaveInventory()) {
            for (int i = 9; i <= 35; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    if (manager.isBlacklisted(item)) {
                        continue;
                    }
                    insuredItems.put(i, item.clone());
                }
            }
        }

        // 3. Save armor (slots 36 to 39)
        if (tier.isSaveArmor()) {
            ItemStack[] armor = inv.getArmorContents(); // boots, leggings, chestplate, helmet
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item != null && !item.getType().isAir()) {
                    if (manager.isBlacklisted(item)) {
                        continue;
                    }
                    insuredItems.put(36 + i, item.clone());
                }
            }
        }

        // 4. Save offhand (slot 40)
        if (tier.isSaveOffhand()) {
            ItemStack offhand = inv.getItemInOffHand();
            if (offhand != null && !offhand.getType().isAir()) {
                if (!manager.isBlacklisted(offhand)) {
                    insuredItems.put(40, offhand.clone());
                }
            }
        }

        // 5. Save XP
        if (tier.isSaveXp()) {
            int xpPercentage = tier.getXpRestorePercentage();
            if (xpPercentage > 0) {
                int totalLevel = player.getLevel();
                float progress = player.getExp();

                insuredXpLevel = (int) (totalLevel * (xpPercentage / 100.0));
                insuredXpProgress = progress;

                // Clear the dropped XP so they cannot duplicate it
                event.setDroppedExp(0);
            }
        }

        // If we insured anything (items or XP)
        if (!insuredItems.isEmpty() || insuredXpLevel > 0) {
            List<ItemStack> drops = event.getDrops();
            
            // Remove the insured items from the death drops list
            for (ItemStack insured : insuredItems.values()) {
                removeMatchingDrop(drops, insured);
            }

            // Create a pending claim
            PlayerClaimData claim = new PlayerClaimData(
                    insuredItems,
                    insuredXpLevel,
                    insuredXpProgress,
                    System.currentTimeMillis(),
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ()
            );
            insurance.getPendingClaims().add(claim);
            manager.saveData();

            // Log event
            plugin.getHistoryLogger().log(String.format(
                    "Death: Player %s (%s) died at [%s, %.2f, %.2f, %.2f]. Insured Tier: %s. Saved %d items, %d levels XP.",
                    player.getName(),
                    player.getUniqueId(),
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    tier.getId(),
                    insuredItems.size(),
                    insuredXpLevel
            ));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{tier}", tier.getDisplayName());
            manager.sendMessage(player, "death-saved", placeholders);
        } else {
            manager.sendMessage(player, "death-empty", null);
        }
    }

    private void removeMatchingDrop(List<ItemStack> drops, ItemStack insured) {
        if (insured == null) return;
        int needed = insured.getAmount();
        Iterator<ItemStack> iter = drops.iterator();
        while (iter.hasNext() && needed > 0) {
            ItemStack drop = iter.next();
            if (drop != null && drop.isSimilar(insured)) {
                if (drop.getAmount() <= needed) {
                    needed -= drop.getAmount();
                    iter.remove();
                } else {
                    drop.setAmount(drop.getAmount() - needed);
                    needed = 0;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        float exp = player.getExp();
        if (exp < 0.0f || exp >= 1.0f || Float.isNaN(exp) || Float.isInfinite(exp)) {
            plugin.getLogger().warning("Corrected invalid XP progress for player " + player.getName() + " (" + exp + " -> 0.0)");
            player.setExp(0.0f);
        }
    }
}
