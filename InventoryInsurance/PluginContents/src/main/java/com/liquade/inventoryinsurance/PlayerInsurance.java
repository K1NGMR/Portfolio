package com.liquade.inventoryinsurance;

import java.util.ArrayList;
import java.util.List;

public class PlayerInsurance {
    private final String uuid;
    private String tier;
    private long expiryTime;
    private long lastClaimTime;
    private double balance;
    private final List<PlayerClaimData> pendingClaims;

    public PlayerInsurance(String uuid) {
        this.uuid = uuid;
        this.tier = "none";
        this.expiryTime = 0;
        this.lastClaimTime = 0;
        this.balance = 0.0;
        this.pendingClaims = java.util.Collections.synchronizedList(new ArrayList<>());
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTier() {
        // Automatically check if insurance expired and downgrade if so
        if (tier != null && !tier.equalsIgnoreCase("none") && hasExpired()) {
            tier = "none";
            expiryTime = 0;
        }
        return tier;
    }

    public void setTier(String tier) {
        this.tier = (tier == null) ? "none" : tier;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public long getLastClaimTime() {
        return lastClaimTime;
    }

    public void setLastClaimTime(long lastClaimTime) {
        this.lastClaimTime = lastClaimTime;
    }

    public List<PlayerClaimData> getPendingClaims() {
        return pendingClaims;
    }

    public boolean hasExpired() {
        return expiryTime > 0 && System.currentTimeMillis() > expiryTime;
    }

    public boolean isInsured() {
        return !getTier().equalsIgnoreCase("none");
    }
}
