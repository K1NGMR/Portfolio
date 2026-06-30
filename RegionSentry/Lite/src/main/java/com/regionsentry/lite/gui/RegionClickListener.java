package com.regionsentry.lite.gui;

import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.entity.Player;

public interface RegionClickListener {
    void onLeftClick(Player player, RegionTracker tracker);
    void onRightClick(Player player, RegionTracker tracker);
}
