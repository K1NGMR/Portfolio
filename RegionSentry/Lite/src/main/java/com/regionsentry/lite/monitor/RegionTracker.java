package com.regionsentry.lite.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionTracker {
    private static final int ROLLING_WINDOW_SIZE = 40; // 40 entries = ~10 seconds at 5-tick intervals

    private final String regionId;
    private long threadId = -1;
    private String threadName = "Unknown";
    private final Set<ChunkKey> chunks = ConcurrentHashMap.newKeySet();

    private final java.util.concurrent.atomic.AtomicInteger chunksLoaded = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger chunksGenerated = new java.util.concurrent.atomic.AtomicInteger(0);
    private double chunksLoadedPerSec = 0.0;
    private double chunksGeneratedPerSec = 0.0;

    private final java.util.concurrent.atomic.AtomicInteger boundaryCrossings = new java.util.concurrent.atomic.AtomicInteger(0);
    private double boundaryCrossingsPerMin = 0.0;
    private volatile long lastTickTimestamp = 0;

    private final Map<java.util.UUID, org.bukkit.Location> lastPlayerLocations = new ConcurrentHashMap<>();
    private String fragmentationRisk = "LOW";
    private boolean mergeWarning = false;

    private long lastThreadId = -1;
    private long lastTickTime = 0;
    private long lastCpuTime = -2;

    private final double[] tpsHistory = new double[ROLLING_WINDOW_SIZE];
    private final double[] msptHistory = new double[ROLLING_WINDOW_SIZE];
    private int historyIndex = 0;
    private int historyCount = 0;

    private int lastEntityCount = 0;
    private int lastPlayerCount = 0;

    public RegionTracker(String regionId) {
        this.regionId = regionId;
    }

    public String getRegionId() {
        return regionId;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public Set<ChunkKey> getChunks() {
        return chunks;
    }

    public void updateMetrics(int entities, int players) {
        this.lastEntityCount = entities;
        this.lastPlayerCount = players;

        long now = System.nanoTime();
        Thread currentThread = Thread.currentThread();
        long currentThreadId = currentThread.getId();
        this.threadName = currentThread.getName();
        this.threadId = currentThreadId;

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long cpuNow = bean.isThreadCpuTimeSupported() ? bean.getThreadCpuTime(currentThreadId) : -1;

        if (lastThreadId == currentThreadId && lastTickTime != 0) {
            long timeElapsed = now - lastTickTime;
            double seconds = timeElapsed / 1_000_000_000.0;
            if (seconds > 0) {
                this.chunksLoadedPerSec = chunksLoaded.getAndSet(0) / seconds;
                this.chunksGeneratedPerSec = chunksGenerated.getAndSet(0) / seconds;
                this.boundaryCrossingsPerMin = (boundaryCrossings.getAndSet(0) / seconds) * 60.0;
            }

            // 5 ticks is the expected period
            double tps = (5.0 * 1_000_000_000.0) / timeElapsed;
            if (tps > 20.0) tps = 20.0;
            if (tps < 0.0) tps = 0.0;

            double mspt = 0.0;
            if (cpuNow != -1 && lastCpuTime != -2) {
                long cpuElapsed = cpuNow - lastCpuTime;
                mspt = (cpuElapsed / 1_000_000.0) / 5.0;
            } else {
                // Fallback to wall-clock average per tick if CPU time is not supported
                mspt = (timeElapsed / 1_000_000.0) / 5.0;
                if (tps >= 19.9) {
                    mspt = Math.min(mspt, 10.0); // estimate low active time if at 20 TPS
                }
            }

            addHistory(tps, mspt);
        } else {
            // Reset counters on thread hop or first run
            this.chunksLoadedPerSec = 0.0;
            this.chunksGeneratedPerSec = 0.0;
            this.boundaryCrossingsPerMin = 0.0;
        }

        this.lastThreadId = currentThreadId;
        this.lastTickTime = now;
        this.lastCpuTime = cpuNow;
    }

    public void incrementChunksLoaded() {
        chunksLoaded.incrementAndGet();
    }

    public void incrementChunksGenerated() {
        chunksGenerated.incrementAndGet();
    }

    public double getChunksLoadedPerSec() {
        return chunksLoadedPerSec;
    }

    public double getChunksGeneratedPerSec() {
        return chunksGeneratedPerSec;
    }

    private synchronized void addHistory(double tps, double mspt) {
        tpsHistory[historyIndex] = tps;
        msptHistory[historyIndex] = mspt;
        historyIndex = (historyIndex + 1) % ROLLING_WINDOW_SIZE;
        if (historyCount < ROLLING_WINDOW_SIZE) {
            historyCount++;
        }
    }

    public synchronized double getAverageTPS() {
        if (historyCount == 0) return 20.0;
        double sum = 0;
        for (int i = 0; i < historyCount; i++) {
            sum += tpsHistory[i];
        }
        return sum / historyCount;
    }

    public synchronized double getAverageMSPT() {
        if (historyCount == 0) return 1.0;
        double sum = 0;
        for (int i = 0; i < historyCount; i++) {
            sum += msptHistory[i];
        }
        return sum / historyCount;
    }

    public int getEntityCount() {
        return lastEntityCount;
    }

    public int getPlayerCount() {
        return lastPlayerCount;
    }

    public double getThreadUtilization() {
        return (getAverageMSPT() / 50.0) * 100.0;
    }

    public void incrementBoundaryCrossings() {
        boundaryCrossings.incrementAndGet();
    }

    public double getBoundaryCrossingsPerMin() {
        return boundaryCrossingsPerMin;
    }

    public long getLastTickTimestamp() {
        return lastTickTimestamp;
    }

    public void setLastTickTimestamp(long lastTickTimestamp) {
        this.lastTickTimestamp = lastTickTimestamp;
    }

    public org.bukkit.Location getLastPlayerLocation(java.util.UUID uuid) {
        return lastPlayerLocations.get(uuid);
    }

    public void setPlayerLocation(java.util.UUID uuid, org.bukkit.Location loc) {
        lastPlayerLocations.put(uuid, loc);
    }

    public void removePlayerLocation(java.util.UUID uuid) {
        lastPlayerLocations.remove(uuid);
    }

    public void cleanUpInactivePlayers(java.util.Set<java.util.UUID> active) {
        lastPlayerLocations.keySet().retainAll(active);
    }

    public String getFragmentationRisk() {
        return fragmentationRisk;
    }

    public void setFragmentationRisk(String risk) {
        this.fragmentationRisk = risk;
    }

    public boolean isMergeWarning() {
        return mergeWarning;
    }

    public void setMergeWarning(boolean warning) {
        this.mergeWarning = warning;
    }
}
