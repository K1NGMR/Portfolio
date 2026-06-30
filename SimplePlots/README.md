SimplePlots is a highly customizable, modern, lightweight, and extremely robust plot management plugin for Bukkit/Spigot/Paper servers. It provides players with absolute creative freedom over their own private land cells, while equipping server admins with powerful security controls, customization switches, and robust anti-exploit protection.

---

## ✨ Key Features

- **Plot Claiming & Auto-Matching**: Claim individual plot cells or let the plugin automatically search for and allocate the closest available plot.
- **Dynamic Merging & Transitive Splitting**: Connect adjacent plots into massive multi-cell clusters. Border walls and road dividers are cleared automatically. Auto-detects complex multi-plot shapes (like L-shapes) to complete square clusters seamlessly.
- **Full Custom Flag Manager**: Set environmental rules, entry access limits, PvP/PvE combat, custom greetings/farewell titles, custom permanent time/weather, elytra restrictions, and more.
- **Salvation Plot Chests**: Items in containers (chests, shulker boxes, barrels, furnaces, dispensers, etc.) located in road zones during merges are automatically swept and saved into a paginated virtual GUI (`/plot chest`) so no blocks are lost.
- **Environmental Security Shield**: Full protection against griefing including TNT cannons, piston push/pull overrides, dispenser item-spitting, tree growth block clipping, elytra fly-ins, and crawl/trapdoor phase glitches.
- **Guest Book & Ratings**: Allow guests to rate plots (1-5 stars) and leave comments (guestbooks) on plot properties.
- **Layout backup & Schematics**: Save, download, backup, and copy-paste layout schematics easily. Fully protected against chest/container duplication exploits.

---

## 🛠️ Commands & Permissions Reference

All commands require the base permission defined in `permissions.yml` (default is `plots.use`). Subcommands are dynamically hidden from tab completion and help menus if the player does not possess the required permission.

| Subcommand | Description | Default Permission |
| :--- | :--- | :--- |
| `/plot claim` | Claims the current plot cell you are standing on. | `plots.claim` |
| `/plot auto` | Claims and teleports to the nearest available plot cell. | `plots.auto` |
| `/plot info` | View details (owner, members, ratings, flags) of the current plot. | `plots.info` |
| `/plot gui` / `/plot menu` | Opens the main plot management GUI. | `plots.use` |
| `/plot add <player>` | Add player as member (can build only when owner is online). | `plots.add` |
| `/plot remove <player>` | Remove a player from added, trusted, or denied lists. | `plots.remove` |
| `/plot trust <player>` | Grants trust (can build even when owner is offline). | `plots.trust` |
| `/plot untrust <player>` | Revokes trust from a player. | `plots.trust` |
| `/plot block <player>` | Bans a player from entering or teleporting to the plot. | `plots.block` |
| `/plot unblock <player>` | Lifts a block/ban from a player. | `plots.unblock` |
| `/plot biome <biome>` | Sets the plot biome (also selectable via GUI). | `plots.biome` |
| `/plot flag <set/remove> <flag> [value]` | Sets or removes custom plot flags. | `plots.flag` |
| `/plot merge` | Merges with adjacent plots you own, or sends a merge request to adjacent owners. | `plots.merge` |
| `/plot merge accept` | Accepts a pending merge request from an adjacent plot owner. | `plots.merge` |
| `/plot merge deny` | Denies a pending merge request from an adjacent plot owner. | `plots.merge` |
| `/plot unlink` | Splits a merged cluster back into individual cells. | `plots.unlink` |
| `/plot clear` | Resets the plot blocks back to the default generated floor. | `plots.clear` |
| `/plot delete` | Unclaims the plot and completely clears its blocks. | `plots.delete` |
| `/plot copy` | Copies the plot blocks to a temporary clipboard file. | `plots.copy` |
| `/plot paste` | Pastes the copied clipboard blocks onto another owned plot. | `plots.paste` |
| `/plot backup <save/list/load>` | Manually backup or restore plot schematics. | `plots.backup` |
| `/plot visit <player>` | Teleports to a player's plot. | `plots.visit` |
| `/plot home` | Teleports to one of your claimed plots. | `plots.home` |
| `/plot sethome` | Sets a custom teleport location inside the plot. | `plots.sethome` |
| `/plot middle` | Teleports to the exact center of the plot/cluster. | `plots.middle` |
| `/plot chest` | Opens a paginated GUI to claim items salvaged during merges. | *No Permission Required (false)* |
| `/plot joinsupportdc` | Sends a clickable link to the Support Discord. | `plots.joinsupportdc` |
| `/plot admin panel` | Opens the graphical Admin Control panel (e.g. toggle road builds). | `plots.admin` |
| `/plot admin <command>` | Access the administrator override suite. | `plots.admin` |

---

## 📈 PlaceholderAPI Placeholders

Exposes the following placeholders (all prefixed with `%plot_<name>%` or `%plots_<name>%`) for scoreboards, tablists, chat formats, and holograms:

### Player & Account Info
* `%plot_owned%`: Number of plot clusters owned by the player.
* `%plot_allowed_plot_count%` (Aliases: `%plot_limit%`, `%plot_plots_limit%`): Maximum plot limit for the player.

### Plot Details (Standing on a Plot)
* `%plot_id%` (Aliases: `%plot_current_id%`, `%plot_plots_current_id%`): Coordinates of the current plot.
* `%plot_owner%`: Name of the owner of the current plot.
* `%plot_biome%`: Biome name at the player's current location.
* `%plot_flags%`: Active flags on the plot (formatted as key=value).
- `%plot_is_claimed%`: Returns `"true"` if the plot is claimed, otherwise `"false"`.
- `%plot_is_merged%`: Returns `"true"` if the plot is merged in any direction, otherwise `"false"`.
- `%plot_merged_directions%`: Directions in which the plot is merged (e.g. `N, S, E, W`).
- `%plot_members_count%`: Combined total of trusted + added members on the plot.
- `%plot_trusted_count%`: Number of trusted members on the current plot.
- `%plot_allowed_count%`: Number of added/allowed members on the current plot.
- `%plot_denied_count%`: Number of denied/blocked players on the current plot.
- `%plot_rating%` (Aliases: `%plot_average_rating%`, `%plot_rating_average%`): Average star rating of the plot.
- `%plot_ratings_count%`: Total reviews/ratings submitted for the plot.

---

## ⚙️ Configuration Files

SimplePlots is **100% customizable** through its configuration files:
1. **`config.yml`**: Controls anti-grief flags, elytra shields, rating scales, copy-paste limits, and container dupe protections.
2. **`messages.yml`**: Customize all chat responses, warning messages, and hover tooltips with complete hex color support.
3. **`worlds.yml`**: Customize generator sizes (plot size, road width, heights), block materials (floor block, road, wall, bedrock, border), and world-specific setups.
4. **`permissions.yml`**: Maps any subcommand to a custom permission node. Setting a permission value to `"false"` opens it up to all players globally.

---

## 🚀 Installation & Setup

1. Copy the compiled `SimplePlots.jar` from the target directory into your server's `plugins/` directory.
2. Ensure you have dependencies installed (e.g. **WorldEdit/FastAsyncWorldEdit** for backup schematics, and **PlaceholderAPI** for placeholders).
3. Start the server to generate configuration templates, customize them inside the generated folder, and run `/plot reload` to hot-reload config changes!
