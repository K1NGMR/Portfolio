# 👤 Developer & Builder Portfolio | K1NGMR

Welcome to my portfolio! I am an **18-year-old Minecraft Developer, Builder, and Systems Designer** with over **3+ years of experience** owning, developing, and building Minecraft servers. 

During my time as a server owner, I designed and coded custom systems to create unique, engaging gameplay experiences. I developed custom fishing plugins, NPC interactions, and advanced custom weapons—which eventually inspired me to design **ItemEdit (Revamped)**.

Below is an overview of my core skills, experience, and the 5 major plugins featured in this repository.


## 🛠️ Skills & Expertise

*   **💻 Programming & Systems Development:**
    *   Java (Spigot, Paper, and Folia APIs)
    *   Multi-threaded server environments (Folia regional ticking)
    *   Packet-level programming and channel injection (Netty, ProtocolLib)
    *   Database management (SQLite, YAML databases)
    *   Build automation (Maven)
*   **🧠 Critical Thinking & Game Design:**
    *   Designing anti-grief systems, security suites, and anti-exploit protections
    *   Configuring complex economy systems (Vault integration & custom fallbacks)
    *   Structuring user-friendly, responsive in-game GUI interfaces (Inventory GUIs)
*   **🏗️ Game Design & Building:**
    *   3+ years of Minecraft server map construction and structural building
    *   Integrating custom gameplay mechanics (custom mobs, combat systems, region-locked mechanics)


## 💼 Experience

### **Server Owner & Lead Developer** (3+ Years)
*   Managed, built, and maintained a custom Spigot/Paper Minecraft server.
*   Coded and deployed unique gameplay features including custom Fishing Plugins, NPC suites, and an interactive Custom Weapons system.
*   Created server spawns, builds, and custom region maps to enhance immersion.
*   Developed high-performance administration and optimization systems to ensure a lag-free experience for players.


## 📂 Featured Projects

Here is a breakdown of the five plugins in this repository, showing what each does and how they are structured:

### 1. ⚒️ ItemEdit (Revamped)
*An advanced in-game visual item and custom weapon creator.*
*   **Visual Chest Editor (`/ie gui`):** Drag-and-drop chest interface that allows admins to edit item names, lore, enchantments, flags (like unbreakable/hide attributes), and custom model data IDs on the fly without editing YAML configurations.
*   **400+ Modular Combat Abilities:** Includes pre-coded, highly optimized abilities such as *Cosmic Showers, Earthquakes, Blizzard Storms, Void Grasp*, and *Lunar Blessings*.
*   **Weapon Presets (`weapon.yml`):** Define default custom weapons with base custom model data, statistics, and preset abilities.
*   **Per-Item Overrides (`/ie custom`):** Overwrite specific parameters (like cooldown, damage, radius, or range) on a per-item basis.
*   **Built-in Safety:** Protection rules to prevent self-damage from custom weapon explosions or lightning strikes.

### 2. 🗺️ SimplePlots
*A complete, high-performance, and feature-rich PlotSquared v7 replica.*
*   **Plot Management:** Standard claim, auto-claim, visit, rating (1-5 stars), and guest comment book systems.
*   **Dynamic Merging & Transitive Splitting:** Allows players to merge adjacent plots into massive custom-shaped clusters. It automatically clears intermediate border walls and roads.
*   **Salvation Chest System:** When roads are cleared during a plot merge, any containers (chests, shulker boxes, etc.) are safely swept, and their items are stored in a virtual GUI (`/plot chest`) so players never lose blocks.
*   **Environmental Security Shield:** Full protection against griefing including TNT cannons, piston push/pull overrides, dispenser exploits, tree growth block-clipping, elytra fly-ins, and phase glitching.
*   **Layout Backups:** Save, download, and copy-paste layout schematics safely utilizing WorldEdit hooks.

### 3. 🟢 RegionSentry (Lite & Pro)
*Next-generation multi-threaded performance monitoring and automated lag mitigation suite designed specifically for Folia servers.*
*   **Thread-Level Telemetry:** Monitors regional MSPT (Milliseconds Per Tick) and TPS (Ticks Per Second) across Folia's independent ticking threads.
*   **Interactive Dashboard (`/regionsentry`):** Real-time, color-coded GUI dashboard sorting the heaviest region threads to the top for quick admin diagnostics.
*   **Adaptive Ticking Manager (Pro):** Automatically lowers simulation distance and throttles mob AI ("Brain Freeze") inside strained threads, restoring them once performance stabilizes.
*   **AFK Region Optimizer (Pro):** Automatically dials down simulation distance and freezes mob AI when all players in a region thread are idle.
*   **Tiered Despawn Engine (Pro):** Multi-phase entity cleanup to clear non-essential passive and hostile mobs when a thread breaches performance thresholds.
*   **SQLite Logbook & Packet Sentry (Pro):** Logs performance spikes (>35ms) to a database and uses Netty channel injection to detect packet-flooding exploits.

### 4. 🛡️ InventoryInsurance
*A customizable inventory protection plugin featuring claims and economy-linked tiers.*
*   **Dynamic Insurance Tiers:** Define custom ranks/tiers (Bronze, Silver, Gold, Platinum) with customizable display names, pricing, durations (in days), and permission limits.
*   **Armor & Slot Preservation:** On death, insured player inventories are saved to a claims queue, preserving their exact slot positioning. They can retrieve their belongings using `/insurance claim`.
*   **Claim Cooldowns & Blacklists:** Configurable cooldowns to prevent death-loop exploits, and an item blacklist (e.g. TNT, lava buckets) that will always drop on the ground.
*   **Dual Economy Support:** Integrates with Vault economy providers, or falls back to a built-in player database (`players.yml`) storing balances if no economy plugin is present.
*   **PAPI & Audit Logs:** PlaceholderAPI integration to display active policies, and a `claims.log` file to trace all purchases and claims.

### 5. 🌌 Equinox (Currently in the works)
*High-performance, packet-level server security and administration suite.*
*   **Advanced Anti-Freecam:** Includes packet-level checks to prevent players from using client-side freecam to bypass blocks.
*   **Cave Reachability & Line of Sight:** Monitors player block interactions (mining, opening chests, clicking blocks) utilizing 3D raycasting and path reachability tests. Instantly blocks interactions made through solid walls.
