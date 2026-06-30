package com.itemedit.light.ability.lava;

import com.itemedit.light.ItemEditLight;
import com.itemedit.light.ability.Ability;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FireAura extends Ability implements Listener {
    private final ItemEditLight plugin;
    private final Map<UUID, Long> activeAuras = new HashMap<>();

    public FireAura(ItemEditLight plugin) {
        super("fire_aura", "Fire Aura", "Grants Fire Resistance and burns attackers when you are damaged.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = plugin.getConfig().getDouble("abilities.fire_aura.duration", 15.0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) (duration * 20), 0));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.8f);

        activeAuras.put(player.getUniqueId(), System.currentTimeMillis() + (long) (duration * 1000));
        player.sendMessage("§6Fire Aura activated!");
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Long expireTime = activeAuras.get(player.getUniqueId());
        if (expireTime == null) {
            return;
        }

        if (System.currentTimeMillis() > expireTime) {
            activeAuras.remove(player.getUniqueId());
            return;
        }

        // Set attacker on fire
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();
            int fireTicks = plugin.getConfig().getInt("abilities.fire_aura.fire_ticks", 60);
            attacker.setFireTicks(fireTicks);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
        }
    }
}
