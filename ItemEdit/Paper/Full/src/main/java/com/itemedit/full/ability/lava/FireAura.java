package com.itemedit.full.ability.lava;

import com.itemedit.full.ItemEditFull;
import com.itemedit.full.ability.Ability;
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
    private final ItemEditFull plugin;
    private final Map<UUID, AuraData> activeAuras = new HashMap<>();

    private static class AuraData {
        final long expireTime;
        final int fireTicks;

        AuraData(long expireTime, int fireTicks) {
            this.expireTime = expireTime;
            this.fireTicks = fireTicks;
        }
    }

    public FireAura(ItemEditFull plugin) {
        super("fire_aura", "Fire Aura", "Grants Fire Resistance and burns attackers when you are damaged.");
        this.plugin = plugin;
    }

    @Override
    public boolean trigger(Player player, ItemStack item) {
        double duration = getDoubleParam(plugin, item, "duration", 15.0);
        int fireTicks = getIntParam(plugin, item, "fire_ticks", 60);

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) (duration * 20), 0));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.8f);

        activeAuras.put(player.getUniqueId(), new AuraData(
                System.currentTimeMillis() + (long) (duration * 1000),
                fireTicks
        ));
        player.sendMessage("§6Fire Aura activated!");
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        AuraData data = activeAuras.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        if (System.currentTimeMillis() > data.expireTime) {
            activeAuras.remove(player.getUniqueId());
            return;
        }

        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();
            attacker.setFireTicks(data.fireTicks);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
        }
    }
}
