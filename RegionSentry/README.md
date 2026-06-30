# <img src="Images/regionsentry_logo.png" width="48" align="center" alt="RegionSentry Logo"/> RegionSentry

**Next-Generation Multi-Threaded Performance Sentry for Folia Servers**

RegionSentry is the ultimate monitoring, profiling, and automated mitigation suite designed specifically for **Folia**-based Minecraft servers. 

Folia's multi-threaded architecture is incredibly powerful, but it completely breaks traditional, single-threaded performance monitoring tools. When your server lags, you need to know *which* region thread is struggling, *why* it's struggling, and *how* to fix it instantly. RegionSentry bridge this gap by monitoring, logging, and auto-mitigating performance bottlenecks on a thread-by-thread level.

---

## 🖥️ Interactive Performance Dashboard

RegionSentry features a live-updating in-game GUI grid (`/regionsentry`) that acts as your server's mission control. It displays every active region thread, color-coded by performance strain (Stable, Strained, or Lagging), and automatically sorts the heaviest threads to the top.

![RegionSentry Dashboard](Images/regionsentry_dashboard.png)

### What you can see in the dashboard:
* **Thread Telemetry:** Real-time TPS and MSPT (Milliseconds Per Tick) per thread.
* **Exact Boundaries:** Interactive coordinate listings showing exact chunk boundaries and block ranges.
* **Load Metrics:** Entity count, player counts, chunk loading rate, chunk generation rate, and boundary-crossing frequency.
* **Proactive Warning Flags:** Imminent thread collision warnings and packet spam flags.

---

## ⚡ Key Features

### 🟢 RegionSentry Lite (Core Performance Monitoring)
* **Real-time Thread Telemetry:** Monitors regional MSPT & TPS.
* **Performance-Sorted GUI Dashboard:** Instantly highlights lagging regions.
* **Dynamic Location HUD:** A togglable actionbar HUD (`/regionsentry hud`) that shows the current thread's performance at your feet.
* **Thread Collision Warnings:** Warns you when two high-performance region threads are on a collision course to merge.
* **Lag Machine Simulator:** Includes a built-in `/lagmachine` tool to safely test region behavior under stress.

### 🔴 RegionSentry Pro (Automated Mitigation & Diagnostics)
* **Interactive Dashboard Actions:** Left-click any region pane to teleport directly to the coordinates; right-click to open a localized mitigation menu.
* **Adaptive Ticking Manager:**
  * **Dynamic Simulation Distance:** Lowers simulation distance (down to a configurable target) only for players inside strained threads, restoring it when performance recovers.
  * **Mob AI Throttling ("Brain Freeze"):** Temporarily strips AI from ordinary mobs in lagging threads to conserve critical tick cycles, and "thaws" them once stabilized.
* **AFK Region Optimizer:** Detects when all players in a region thread are idle (AFK). It automatically dials down simulation distance and freezes AI in that region, waking it up the second activity is detected.
* **Tiered Despawn Engine:** A multi-phase cleanup engine that triggers when a region breaches thresholds. Phase 1 removes low-value passive entities (excluding custom-named or breeding animals); Phase 2 clears hostile mobs if the thread remains strained.
* **Packet Storm & Exploit Detector:** Uses version-independent Netty channel injection to measure incoming packet rates per player. Instantly warns admins and flags players in the GUI if they attempt packet-flooding exploits.
* **Historical SQLite Logbook:** Automatically logs significant regional MSPT spikes (>35ms), recording the world, block coordinates, average MSPT, and a list of players in the region. View hotspots at any time in-game with `/regionsentry history`.
* **Thread Stitching:** Manually merge performance pools for adjacent region threads with `/regionsentry stitch <id1> <id2>`.

---

## 🛠️ Commands & Permissions

| Command | Description | Permission | Availability |
| :--- | :--- | :--- | :--- |
| `/regionsentry` | Opens the graphical thread monitor dashboard. | `regionsentry.admin` | Lite & Pro |
| `/regionsentry hud` | Toggles the real-time performance HUD on your screen. | `regionsentry.admin` | Lite & Pro |
| `/regionsentry history` | Opens the GUI logbook showing past regional lag spikes. | `regionsentry.admin` | Pro |
| `/regionsentry stitch <id1> <id2>` | Stitches two region threads together. | `regionsentry.admin` | Pro |
| `/lagmachine` | Admin command to simulate regional lag. | `regionsentry.admin` | Lite & Pro |

---

## 📈 Designed for Production
RegionSentry is built with performance in mind. All database transactions (SQLite logging and cleanups), packet rate checks, and telemetry aggregations run asynchronously off the main server tick threads, ensuring that the tool monitoring your server never becomes the source of lag itself.
