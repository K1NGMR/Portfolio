package com.liquade.inventoryinsurance;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InsurancePlaceholderExpansion extends PlaceholderExpansion {
    private final InventoryInsurancePlugin plugin;

    public InsurancePlaceholderExpansion(InventoryInsurancePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "inventoryinsurance";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Liquade";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getRequiredPlugin() {
        return "InventoryInsurance";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerInsurance pi = plugin.getInsuranceManager().getOrCreatePlayerInsurance(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "tier": {
                String activeTierId = pi.getTier();
                if (activeTierId.equalsIgnoreCase("none")) {
                    return "Uninsured";
                }
                TierConfig tier = plugin.getInsuranceManager().getTierConfig(activeTierId);
                return tier != null ? org.bukkit.ChatColor.translateAlternateColorCodes('&', tier.getDisplayName()) : activeTierId;
            }
            case "tier_raw":
                return pi.getTier();
            case "expires":
                return formatTimeRemaining(pi.getExpiryTime());
            case "expires_date":
                return formatDate(pi.getExpiryTime());
            case "has_insurance":
                return String.valueOf(pi.isInsured());
            case "cooldown": {
                long cooldownMs = plugin.getInsuranceManager().getClaimCooldownSeconds() * 1000L;
                long elapsed = System.currentTimeMillis() - pi.getLastClaimTime();
                if (elapsed < cooldownMs) {
                    return formatTimeRemaining(pi.getLastClaimTime() + cooldownMs);
                } else {
                    return "Ready";
                }
            }
            case "balance": {
                double bal = getSafeBalance(player);
                return getSafeFormat(bal);
            }
            case "balance_raw": {
                double bal = getSafeBalance(player);
                return String.format("%.2f", bal);
            }
            case "pending_claims":
                return String.valueOf(pi.getPendingClaims().size());
            default:
                return null;
        }
    }

    @Override
    public String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String params) {
        return onRequest(player, params);
    }

    private double getSafeBalance(OfflinePlayer player) {
        Economy econ = plugin.getEconomy();
        if (econ == null) return 0.0;
        try {
            return econ.getBalance(player);
        } catch (Throwable t) {
            try {
                if (player.getName() != null) {
                    return econ.getBalance(player.getName());
                }
            } catch (Throwable ignored) {}
        }
        return 0.0;
    }

    private String getSafeFormat(double amount) {
        Economy econ = plugin.getEconomy();
        if (econ == null) return "$" + String.format("%.2f", amount);
        try {
            return econ.format(amount);
        } catch (Throwable t) {
            return "$" + String.format("%.2f", amount);
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Never";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String formatTimeRemaining(long timestamp) {
        long remaining = timestamp - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / (1000 * 60)) % 60;
        long hours = (remaining / (1000 * 60 * 60)) % 24;
        long days = remaining / (1000 * 60 * 60 * 24);

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
