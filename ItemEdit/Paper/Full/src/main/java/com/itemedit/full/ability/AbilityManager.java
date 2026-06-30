package com.itemedit.full.ability;

import com.itemedit.full.ItemEditFull;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AbilityManager implements Listener {
    private final ItemEditFull plugin;
    private final Map<String, Ability> registry = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final NamespacedKey abilitiesKey;

    public AbilityManager(ItemEditFull plugin) {
        this.plugin = plugin;
        this.abilitiesKey = new NamespacedKey(plugin, "abilities");
    }

    public NamespacedKey getAbilitiesKey() {
        return abilitiesKey;
    }

    public boolean registerAbility(Ability ability) {
        registry.put(ability.getId().toLowerCase(), ability);
        return true;
    }

    public Collection<Ability> getRegisteredAbilities() {
        return registry.values();
    }

    public Ability getAbility(String id) {
        if (id == null) {
            return null;
        }
        String normalizedInput = id.toLowerCase().replace("_", "").replace(" ", "");
        Ability direct = registry.get(id.toLowerCase());
        if (direct != null) {
            return direct;
        }
        for (Ability ability : registry.values()) {
            String normalizedId = ability.getId().toLowerCase().replace("_", "").replace(" ", "");
            if (normalizedId.equals(normalizedInput)) {
                return ability;
            }
        }
        for (Ability ability : registry.values()) {
            String className = ability.getClass().getSimpleName().toLowerCase().replace("_", "").replace(" ", "");
            if (className.equals(normalizedInput)) {
                return ability;
            }
        }
        if (normalizedInput.equals("gojo")) {
            for (Ability ability : registry.values()) {
                if (ability.getClass().getSimpleName().equals("GojoInfinity")) {
                    return ability;
                }
            }
        }
        for (Ability ability : registry.values()) {
            if (ability.getName() != null) {
                String normalizedName = ability.getName().toLowerCase().replace("_", "").replace(" ", "");
                if (normalizedName.equals(normalizedInput)) {
                    return ability;
                }
            }
        }
        return null;
    }

    public List<String> getItemAbilities(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyList();
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(abilitiesKey, PersistentDataType.STRING)) {
            return Collections.emptyList();
        }
        String data = pdc.get(abilitiesKey, PersistentDataType.STRING);
        if (data == null || data.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(data.split(",")));
    }

    public void setItemAbilities(ItemStack item, List<String> abilities) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (abilities == null || abilities.isEmpty()) {
            pdc.remove(abilitiesKey);
        } else {
            pdc.set(abilitiesKey, PersistentDataType.STRING, String.join(",", abilities));
        }
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = player.getInventory().getItemInMainHand();
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (item == null || item.getType().isAir()) {
            return;
        }

        List<String> abilityIds = getItemAbilities(item);
        if (abilityIds.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (String abilityId : abilityIds) {
            Ability ability = getAbility(abilityId);
            if (ability == null) {
                continue;
            }

            // Check Cooldown (supports custom per-item cooldowns)
            double cooldownSec = ability.getDoubleParam(plugin, item, "cooldown", 5.0);

            Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            long cooldownEnd = playerCooldowns.getOrDefault(abilityId, 0L);

            if (now < cooldownEnd) {
                double remaining = (cooldownEnd - now) / 1000.0;
                player.sendActionBar(Component.text("§c" + ability.getName() + " is on cooldown! (" + String.format("%.1f", remaining) + "s remaining)"));
                event.setCancelled(true);
                return;
            }

            // Trigger the ability
            if (ability.trigger(player, item)) {
                // Apply Cooldown
                playerCooldowns.put(abilityId, now + (long) (cooldownSec * 1000));

                // Show vanilla sweep cooldown on the item type
                player.setCooldown(item.getType(), (int) (cooldownSec * 20));
                player.sendActionBar(Component.text("§aUsed " + ability.getName() + "!"));
            }
        }
    }

    @EventHandler
    public void onSelfDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Prevents damage from player's own projectiles (including fireball explosions)
            if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
                org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
                if (player.equals(proj.getShooter())) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Prevents damage from player's own AreaEffectClouds (e.g. DragonBreath)
            if (event.getDamager() instanceof org.bukkit.entity.AreaEffectCloud) {
                org.bukkit.entity.AreaEffectCloud cloud = (org.bukkit.entity.AreaEffectCloud) event.getDamager();
                if (player.equals(cloud.getSource())) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Prevents damage from self-source explosions or abilities
            if (player.equals(event.getDamager())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
