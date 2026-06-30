# 🛡️ InventoryInsurance

InventoryInsurance is a fully customizable Minecraft Spigot/Paper plugin designed for economy and survival servers. It allows players to purchase time-bound, custom-tiered insurance plans using in-game currency. When a player dies under an active insurance plan, their items and experience are saved to a secure claim queue, preserving their exact slot positioning. Insured players can then retrieve their belongings by running `/insurance claim` right where they stand.

---

## ✨ Features

- **🏆 Fully Dynamic Insurance Tiers:** Define custom tiers (e.g. Bronze, Silver, Gold, Platinum) with customizable display names, prices, durations (in days), and permission nodes.
- **🎒 Slot-by-Slot Inventory & Armor Preservation:** Restores items, armor, and offhand slots back to their exact locations on claim. If the target slot is already occupied, the item is placed into the first free slot or dropped safely at their feet.
- **⏱️ Claim Cooldowns:** A configurable claim cooldown timer to prevent exploit loops.
- **🚫 Item Blacklist:** Keep specific items (like TNT, Lava Buckets, etc.) dropping on the ground, even for fully insured players.
- **💰 Dynamic Vault Economy Lookup:** Integrates automatically with any Vault economy provider (EssentialsX, CMI, etc.).
- **💳 Built-In Fallback Economy:** Functions out-of-the-box even without an external economy plugin, storing balances inside the plugin database (`players.yml`).
- **❌ Free / Permission-Only Mode:** Disable the economy component completely via config to offer free insurance as rank perks.
- **💬 100% Customizable Localized Messages:** Every message, prefix, header, and hover prompt is customizable with color code (`&`) support.
- **🧩 PlaceholderAPI (PAPI) Support:** Expose active insurance plans, cooldowns, claims count, and balances to other plugins, boards, or menus.
- **📜 claims.log Audit Trail:** Clean admin log entries for all purchases, claims, and cancellations to prevent staff/player abuse.

---

## 🧩 PlaceholderAPI (PAPI) Placeholders

| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%inventoryinsurance_tier%` | Colored display name of the player's active tier | `🥇 Premium` or `Uninsured` |
| `%inventoryinsurance_tier_raw%` | Raw config ID of the player's active tier | `premium` or `none` |
| `%inventoryinsurance_expires%` | Time remaining until plan expiration | `4 days, 3 hours, 12 minutes` |
| `%inventoryinsurance_expires_date%` | Exact expiration date-time | `2026-07-07 10:17:00` |
| `%inventoryinsurance_has_insurance%` | Boolean indicating if player is currently insured | `true` or `false` |
| `%inventoryinsurance_cooldown%` | Cooldown remaining before claiming items | `42 minutes, 15 seconds` or `Ready` |
| `%inventoryinsurance_balance%` | Formatted internal economy balance | `$1,250.00` |
| `%inventoryinsurance_balance_raw%` | Raw internal economy balance | `1250.00` |
| `%inventoryinsurance_pending_claims%` | Count of claims waiting to be claimed | `1` |

---

## 💻 Commands & Permissions

| Command | Subcommands / Args | Description | Permission Node | Default |
| :--- | :--- | :--- | :--- | :--- |
| `/insurance` | None | Displays help menu | `inventoryinsurance.use` | `true` |
| | `buy <tier>` | Purchases or renews a dynamic insurance tier | `inventoryinsurance.use` | `true` |
| | `claim` | Redeems insured items at your current location | `inventoryinsurance.use` | `true` |
| | `status` | Checks active plan details, expiration, cooldowns | `inventoryinsurance.use` | `true` |
| | `remove` | Cancels current active insurance plan | `inventoryinsurance.use` | `true` |
| | `balance [player]` | Checks internal economy balance | `inventoryinsurance.use` / `inventoryinsurance.admin` (others) | `true` / `op` |
| | `pay <player> <amount>` | Pays money to another player | `inventoryinsurance.use` | `true` |
| | `eco <give/take/set/reset> <player> <amount>` | Modifies player balance (Admin) | `inventoryinsurance.admin` | `op` |
| | `listpapi` | Lists all available PlaceholderAPI placeholders | `inventoryinsurance.use` | `true` |
| | `reload` | Reloads configurations and players database | `inventoryinsurance.admin` | `op` |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# ==============================================================================
# InventoryInsurance - Configuration File
# ==============================================================================

# Cooldown (in seconds) that a player must wait between claims.
# 3600 = 1 hour, 86400 = 24 hours. Set to 0 to disable.
claim-cooldown: 3600

# Blacklisted materials that will NEVER be covered by insurance.
# If a player dies with these items, they will always drop on the ground.
# Use official Bukkit Material names (e.g. NETHERITE_SWORD, DIAMOND_BLOCK).
blacklisted-items:
  - "TNT"
  - "LAVA_BUCKET"

# ==============================================================================
# Dynamic Insurance Tiers Configuration
# ==============================================================================
# You can define as many custom tiers as you like!
# Internally, the tier ID is the key (e.g. basic, standard, premium).
# Each tier has customizable display names, pricing, duration, and coverages.
tiers:
  basic:
    display-name: "&b🥉 Basic"
    price: 1000.0
    duration-days: 7
    save-hotbar: true
    save-inventory: false
    save-armor: false
    save-offhand: false
    save-xp: false
    xp-restore-percentage: 0
    permission: "inventoryinsurance.tier.basic" # Optional permission to purchase
  standard:
    display-name: "&e🥈 Standard"
    price: 5000.0
    duration-days: 7
    save-hotbar: true
    save-inventory: true
    save-armor: false
    save-offhand: false
    save-xp: false
    xp-restore-percentage: 0
    permission: "inventoryinsurance.tier.standard"
  premium:
    display-name: "&a🥇 Premium"
    price: 10000.0
    duration-days: 7
    save-hotbar: true
    save-inventory: true
    save-armor: true
    save-offhand: true
    save-xp: true
    xp-restore-percentage: 100
    permission: "inventoryinsurance.tier.premium"

# ==============================================================================
# Economy Settings
# ==============================================================================
economy:
  # If false, economy deductions are disabled and all insurance is free.
  enabled: true
  
  # Settings for the built-in fallback economy (only active if no other economy provider is found)
  initial-balance: 1000.0
  currency-symbol: "$"
  currency-name-singular: "Dollar"
  currency-name-plural: "Dollars"

# ==============================================================================
# Messages Customization
# ==============================================================================
# Supports ChatColor formatting using '&'.
# Placeholders:
#   {prefix} - The prefix defined below
#   {tier} - The display name of the tier
#   {price} - The formatted cost of the tier
#   {balance} - The player's formatted balance
#   {date} - Formatted expiration date
#   {days} - Remaining duration in days
#   {time} - Formatted time remaining (e.g. 2h 15m)
#   {claims} - Pending claims count
#   {error} - Detailed transaction error
#   {player} - Player name
messages:
  prefix: "&6[&eInventoryInsurance&6] &r"
  no-permission: "{prefix}&cYou do not have permission to buy insurance."
  invalid-tier: "{prefix}&cInvalid tier! Available: &e{tiers}"
  insufficient-funds: "{prefix}&cYou do not have enough money! Costs: &e{price}&c, you have: &a{balance}&c."
  transaction-failed: "{prefix}&cTransaction failed: &7{error}"
  purchase-success: "{prefix}&aYou have purchased {tier} &ainsurance for &e{price}&a!"
  renew-success: "{prefix}&aYou have renewed your {tier} &ainsurance for &e{price}&a!"
  purchase-success-free: "{prefix}&aYou have purchased {tier} &ainsurance!"
  renew-success-free: "{prefix}&aYou have renewed your {tier} &ainsurance!"
  expiry-date: "{prefix}&aIt will expire on: &e{date} &7(in {days} days)"
  claim-cooldown-error: "{prefix}&cYou must wait &e{time} &cbefore filing another claim."
  no-claims-error: "{prefix}&cYou do not have any pending claims to retrieve."
  claim-success: "{prefix}&aYour insured items have been successfully restored!"
  claim-items-dropped: "{prefix}&6Some items did not fit in your inventory and were dropped at your feet."
  remove-success: "{prefix}&aYour {tier} &ainsurance has been successfully removed."
  remove-no-active: "{prefix}&cYou do not have any active insurance to remove."
  reload-success: "{prefix}&aConfiguration and database reloaded successfully!"
  status-header: "&6=== Your Insurance Status ==="
  status-uninsured: "&eStatus: &cUninsured"
  status-tier: "&eTier: {tier}"
  status-expires: "&eExpires: &b{date}"
  status-remaining: "&eTime Remaining: &b{time}"
  status-balance: "&eYour Balance: &a{balance}"
  status-pending: "&ePending Claims: &6{claims} death claim(s)"
  status-cooldown-active: "&eClaim Cooldown: &cActive (expires in {time})"
  status-cooldown-ready: "&eClaim Cooldown: &aReady"
  status-footer: "&6============================="
```

---

## 🛠️ Compilation & Build

To compile the plugin from source, clone this repository and compile using Maven:

```powershell
# Set JAVA_HOME to JDK 17+
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17" # Update to your local JDK path

# Run Maven Package
mvn clean package
```

The output JAR will be generated inside the `/target/` directory:
`target/InventoryInsurance-1.0.0.jar`
