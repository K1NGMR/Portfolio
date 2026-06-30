# ⚒️ ItemEdit — The Ultimate Visual Item & Custom Weapon Creator

Are you tired of configuring custom items using complicated YAML configurations, command lines, or external generators? Staring at console logs just to fix a single color code or custom model data ID is now a thing of the past.

**ItemEdit** is the ultimate game-changing plugin for Spigot/Paper Minecraft servers (1.20.4 - 1.21.x+). It introduces a fully interactive, drag-and-drop in-game chest GUI that lets you edit every single aspect of the item in your hand on the fly. Plus, it comes loaded with a modular backend weapon configuration system, custom parameter overrides, and over **400+ custom combat abilities**.

---

## 🎨 In-Game Screenshots

Here is a look at what ItemEdit looks like in action, featuring its clean chest GUI and some of the custom abilities you can summon:

![Banner](ItemEditFull_git/banner.png)

### 📸 Gameplay Gallery
*   **Slide 1**: Interactive Item Editor GUI
*   **Slide 2**: Assigning Stats & Attributes
*   **Slide 3**: Custom Ability Parameter Binds


---

## ✨ Features at a Glance

*   **🖥️ Fully Interactive GUI Editor (`/ie gui`)**: Make edits visually inside an intuitive chest interface. Set names, update lore, apply enchantments, toggle flags, and set custom model data without ever opening a configuration file.
*   **🔥 400+ Modular Combat Abilities**: Unleash warden sonic blasts, cosmic meteor strikes, phantom decoys, or lightning storms. Every ability is fully optimized to run efficiently with no lag.
*   **⚙️ Per-Weapon Configuration (`weapon.yml`)**: Define custom weapon presets. Assign base custom model data, default stats, and active abilities for each custom weapon key.
*   **🔧 Per-Item Custom Parameter Overrides (`/ie custom`)**: Tailor weapon specs per item! Want a legendary version of an existing sword with half the cooldown, double the radius, or triple the damage? Customize it instantly in-game.
*   **🛡️ Built-in Self-Damage Protection**: Rest easy knowing that players won’t blow themselves up or hurt themselves with their own custom lighting strikes, projectiles, or explosions.
*   **⚡ Premium Admin Commands**: Features full tab completion for all subcommands, enchantments, attributes, flags, abilities, and custom parameters.

---

## 🚀 Commands & Permissions

*   **`/ie gui`**: Opens the visual item editor menu (Requires holding an item).
*   **`/ie give [player] <weapon_key>`**: Gives a pre-configured weapon from `weapon.yml` to yourself or a targeted player (features auto-tab completion).
*   **`/ie custom <ability> <param> <value>`**: Overrides a specific parameter on the held item.
*   **`/ie reload`**: Instantly reloads `config.yml` and `weapon.yml`.
*   **`/ie ability <add/remove/clear/list>`**: Quick command-line binds for abilities.
*   **`/ie rename <name>`**: Fast item renaming with MiniMessage (RGB/gradient) and legacy color codes.
*   **`/ie lore <add/set/remove/clear>`**: Easily adjust item lore lines.
*   **`/ie flag <add/remove/clear> <flag>`**: Toggle item flags like hiding attributes or unbreakable tags.
*   **`/ie unbreakable <true/false>`**: Set the item to be unbreakable.

### 🛡️ Permissions:
*   `itemedit.use`: Allows players to use the GUI and general customization commands.
*   `itemedit.admin`: Allows reloading the configurations and giving items to players.

---

## 🔮 Categorized List of Abilities (400+ Total)

To help you design the ultimate custom weapons, here is a categorized summary of all the built-in, ready-to-use abilities in **ItemEdit Full**:

### 🌌 Cosmic & Void Abilities
*   **`cosmic_shower`** *(Cosmic Shower)*: Rains stardust on enemies.
*   **`cosmic_singularity`** *(Cosmic Singularity)*: Implodes targets within a radius.
*   **`cosmic_shield`** *(Cosmic Shield)*: Summons orbiting shields for defense.
*   **`cosmic_rift`** *(Cosmic Rift)*: Pulls enemies into a cosmic rift.
*   **`void_collapse`** *(Void Collapse)*: Creates a black hole pulling in nearby entities.
*   **`void_grasp`** *(Void Grasp)*: Grapples targets towards the caster.
*   **`void_warp`** *(Void Warp)*: Teleports the user through space.
*   **`void_slayer`** *(Void Slayer)*: Deals devastating void damage strike.
*   **`lunar_blessing`** *(Lunar Blessing)*: Increases stats and speed at night.
*   **`lunar_crescent`** *(Lunar Crescent)*: Launches a crescent wave of lunar energy.
*   **`solar_flare`** *(Solar Flare)*: Ignites all targets in front of you.

### 🌿 Earth & Nature Abilities
*   **`earthquake` / `earth_quake`** *(Earthquake)*: Slams the ground, dealing AoE damage and knockback.
*   **`entangle`** *(Entangle)*: Traps targeted entity in dense leaves.
*   **`leaf_gust`** *(Leaf Gust)*: Launches a leaf blast pushing nearby enemies back.
*   **`earth_shield`** *(Earth Shield)*: Surrounds the caster in rock, absorbing damage.
*   **`earth_wall`** *(Earth Wall)*: Spawns temporary walls block.
*   **`earth_tomb`** *(Earth Tomb)*: Pulls target underground.
*   **`earth_smash`** *(Earth Smash)*: Massive ground slam dealing blunt damage.
*   **`photosynthesis`** *(Photosynthesis)*: Slowly heals the player when standing in direct sunlight.
*   **`rock_slide`** *(Rock Slide)*: Drops heavy stones from the sky.
*   **`geyser`** *(Geyser)*: Spawns a high-pressure water jet that launches targets.
*   **`poison_ivy`** *(Poison Ivy)*: Creates a poison leaf trap.
*   **`bamboo_spear`** *(Bamboo Spear)*: Launches a fast bamboo projectile.
*   **`harvest_bloom`** *(Harvest Bloom)*: Automatically grows nearby crops.

### ❄️ Frost & Ice Abilities
*   **`ice_path`** *(Ice Path)*: Grants Speed, turning water to ice and lava to obsidian.
*   **`frostbite` / `ice_frostbite`** *(Frostbite)*: Fires a frost shard that damages and slows targets.
*   **`blizzard` / `ice_blizzard`** *(Blizzard)*: Spawns a swirling frost storm, slowing and damaging enemies.
*   **`blizzard_shield` / `ice_shield`** *(Blizzard Shield)*: Slows attackers and buffers damage.
*   **`avalanche`** *(Avalanche)*: Launches a rapid volley of freezing snowballs.
*   **`ice_barricade`** *(Ice Barricade)*: Creates an defensive ice wall in front of you.
*   **`ice_nova`** *(Ice Nova)*: Releases an expanding ring of frost.
*   **`ice_prison`** *(Ice Prison)*: Encases target in a block cage of ice.
*   **`glacier_crash`** *(Glacier Crash)*: Hits with glacier impact, freezing targets.
*   **`frost_giant_slam`** *(Frost Slam)*: Giant ground slam that inflicts extreme slowness.
*   **`stray_slowness_arrow`** *(Slowness Arrow)*: Fires slowness arrow shots.
*   **`stray_freeze_touch`** *(Freeze Touch)*: Freezes targets on contact.
*   **`stray_snow_storm`** *(Blizzard Storm)*: Shoots a wave of freezing snow.
*   **`snow_golem_snowball`** *(Snowball Barrage)*: Rapidly fires snowballs.
*   **`snow_golem_trail`** *(Snow Trail)*: Leaves a trail of snow.
*   **`snow_golem_freeze`** *(Snow Golem Freeze)*: Freeze blocks on touch.
*   **`ice_spike_summon`** *(Ice Spike)*: Summons spikes from the ground.
*   **`ice_skate`** *(Ice Skate)*: Enhances speed on ice blocks.

### ⚡ Sky, Wind & Lightning Abilities
*   **`thunderbolt` / `sky_lightning_strike`** *(Thunderbolt)*: Strikes target block with lightning.
*   **`overload`** *(Overload)*: Speeds up the caster and grants bonus true damage.
*   **`chain_lightning` / `lightning_chain`** *(Chain Lightning)*: Strikes lightning that jumps between targets.
*   **`wind_blade` / `wind_gust_slash`** *(Wind Blade)*: Launches a wind blade projectile that pierces targets.
*   **`tornado_leap` / `breeze_leap`** *(Tornado Leap)*: Leaps high into the air, pushing back enemies.
*   **`wind_walk`** *(Wind Walk)*: Speed and Jump boost.
*   **`wind_shockwave` / `breeze_deflect`** *(Wind Shockwave)*: Deflects oncoming arrows and projectiles.
*   **`tempest` / `tempest_strike`** *(Tempest)*: Strikes lightning in targeted area, creating storm clouds.
*   **`lightning_storm`** *(Lightning Storm)*: Calls down a continuous area lightning storm.
*   **`lightning_dash`** *(Lightning Dash)*: Blinks forward, leaving a lightning strike path.
*   **`wind_push` / `breeze_gust`** *(Wind Push)*: Strong cone wind push.
*   **`wind_pull`** *(Wind Pull)*: Vacuum draw pulling enemies to you.
*   **`wind_cyclone`** *(Wind Cyclone)*: Summons a localized tornado.
*   **`sky_zephyr`** *(Sky Zephyr)*: Lifts target entities into the air.
*   **`breeze_wind_charge`** *(Wind Charge)*: Fires wind blasts.

### 🔥 Nether, Fire & Lava Abilities
*   **`fire_aura` / `blaze_aura` / `nether_heat`** *(Fire Aura)*: Burns nearby targets and grants fire resistance.
*   **`heat_wave` / `fire_nova`** *(Heat Wave)*: Expanding ring of fire that burns nearby enemies.
*   **`lava_absorption`** *(Lava Absorption)*: Absorbs nearby lava to heal and gain fire resistance.
*   **`lava_pour`** *(Lava Pour)*: Places a temporary lava block.
*   **`lava_spit` / `magma_spit`** *(Lava Spit)*: Launches a fireball or spit that burns enemies.
*   **`lava_walker`** *(Lava Walker)*: Walk on lava by turning it to obsidian/magma.
*   **`wither_decay` / `wither_cleave`** *(Wither Decay)*: Launches a wither skull or slash that inflicts decay.
*   **`ghast_blast` / `ghast_fireball`** *(Ghast Blast)*: Fires a large fireball that explodes on impact.
*   **`blaze_rod_volley` / `blaze_barrage`** *(Blaze Volley)*: Fires a burning fireball cone spread.
*   **`wither_blast`** *(Wither Blast)*: Releases an explosion around you, applying wither.
*   **`meteor_strike` / `meteor_strike_small` / `meteor_strike_large` / `meteor_strike_gigantic` / `huge_meteorstrike` / `massive_meteorstrike` / `galaxy_meteorstrike`** *(Meteor Strike)*: Call down meteor strikes of various sizes up to galaxy-tier showers.
*   **`magma_shield`** *(Magma Shield)*: Shield that defends against fire and damage.
*   **`magma_jump`** *(Magma Jump)*: Vault high using thermal energy.
*   **`magma_trail`** *(Magma Trail)*: Leaves a blazing path of fire blocks.
*   **`magma_fist`** *(Magma Fist)*: Strikes with fiery impact.
*   **`blaze_speed`** *(Blaze Speed)*: High velocity flame dash.
*   **`blaze_flight`** *(Blaze Flight)*: Propels the player upwards using fire.
*   **`ghast_float`** *(Ghast Float)*: Grants safe slow falling.
*   **`ghast_scream`** *(Ghast Scream)*: Screams, knocking back targets.
*   **`ghast_tear`** *(Ghast Tear)*: Quick health restoration.
*   **`nether_quake`** *(Nether Quake)*: Slam waves of nether energy.
*   **`nether_portal_rift`** *(Nether Rift)*: Teleport rift dash.
*   **`nether_soul_drain`** *(Nether Soul Drain)*: Drains life force from targets.

### 👁️ Warden & Sculk Abilities
*   **`sonic_boom` / `warden_sonic_strike`** *(Sonic Boom)*: Launches a linear sonic blast that deals magic damage and high knockback.
*   **`sculk_infestation` / `warden_sculk_infection`** *(Sculk Infestation)*: Releases sculk energy, causing nearby enemies to wither and go dark.
*   **`warden_sonic_clap`** *(Sonic Clap)*: Knocks back and blinds enemies in a short cone in front of you.
*   **`sculk_sensor_trap`** *(Sculk Sensor Trap)*: Places an invisible trap. Explodes with sculk energy when enemies step on it.
*   **`warden_rage`** *(Warden Rage)*: Shrouds you in sculk energy, granting Strength II and Resistance II.
*   **`sculk_shrieker_sound`** *(Sculk Shriek)*: Releases a deafening sound blast.
*   **`sculk_sensor_ping`** *(Sensor Ping)*: Detects motion and highlights targets.
*   **`sculk_blindness_aura`** *(Sculk Darkness)*: Applies darkness aura to nearby entities.
*   **`sculk_infection_strike`** *(Sculk Infection)*: Infests targets on melee hit.
*   **`sculk_vein_spread`** *(Sculk Spread)*: Spreads sculk veins on the ground.
*   **`sculk_catalyst_heal`** *(Sculk Catalyst)*: Drains XP from targets to heal.
*   **`warden_darkness_burst`** *(Darkness Burst)*: Shrouds nearby enemies in darkness.
*   **`warden_sculk_step`** *(Sculk Step)*: Enhances speed on sculk blocks.
*   **`warden_vibration_sense`** *(Vibration Sense)*: Highlights targets through walls.

### 💀 Undead & Skeleton/Zombie Abilities
*   **`zombie_swarm`** *(Zombie Swarm)*: Summons 2 helper zombies equipped with helmets.
*   **`undead_bite`** *(Undead Bite)*: Bites targets, stealing saturation and healing you.
*   **`zombie_infection`** *(Undead Infection)*: If infected target dies within 10s, they rise as a helper zombie.
*   **`zombie_rage`** *(Zombie Rage)*: Deals double damage but you take 1.5x damage.
*   **`undead_call`** *(Undead Call)*: Pulls nearby vanilla zombies towards you.
*   **`bone_shield`** *(Bone Shield)*: Bone shards that absorb your next incoming damage.
*   **`arrow_hail`** *(Arrow Hail)*: Rains a volley of arrows targeting the location you look at.
*   **`skeleton_archers`** *(Skeleton Archers)*: Summons 2 helper skeleton archers.
*   **`bone_trap`** *(Bone Trap)*: Locks the targeted entity in a cage of bones.
*   **`wither_skeleton_strike`** *(Wither Strike)*: Next melee hit deals bonus damage and Wither.

### 🌊 Ocean & Aquatic Abilities
*   **`drowned_whirlpool`** *(Drowned Whirlpool)*: Water whirlpool pull.
*   **`guardian_laser_burst` / `guardian_beam`** *(Laser Burst)*: Escalating magic beam damage.
*   **`elder_guardian_fatigue_blast` / `elder_guardian_fatigue`** *(Fatigue Blast)*: Disorienting waves of mining fatigue.
*   **`elder_guardian_ghost`** *(Elder Curse)*: Displays elder guardian screen curse.
*   **`drowned_trident`** *(Drowned Trident)*: Launches tridents.
*   **`drowned_water_leap`** *(Water Leap)*: Leaps out or inside water.
*   **`drowned_conduit_power`** *(Conduit Grace)*: Grants water breathing and strength under water.
*   **`drowned_depth_strider`** *(Drowned Speed)*: Agile swimming.
*   **`dolphin_grace`** *(Dolphin Grace)*: High aquatic speed.
*   **`dolphin_leap`** *(Dolphin Leap)*: Launch out of water.
*   **`dolphin_sonic`** *(Dolphin Sonic)*: Sonic burst.
*   **`turtle_shell`** *(Turtle Shell)*: High defense when crouching.
*   **`turtle_fortitude`** *(Turtle Fortitude)*: Resistance to knockback.
*   **`squid_ink` / `squid_blindness`** *(Squid Ink)*: Blinds targets.
*   **`squid_propulsion`** *(Ink Jets)*: Rapid dash backwards.
*   **`fish_agility`** *(Fish Agility)*: Swift aquatic speed.
*   **`fish_water_breathing`** *(Water Gills)*: Breathe underwater.
*   **`ocean_tempest`** *(Water Tempest)*: Launches entities in water.
*   **`ocean_tsunami`** *(Tsunami Wave)*: Launches wave push.

### 🏹 Combat Class & Utility Skills
*   **`assassin_backstab`** *(Assassin Backstab)*: Critical damage when striking from behind.
*   **`assassin_smoke_bomb`** *(Smoke Bomb)*: Blinds targets and vanishes.
*   **`assassin_poison_dart`** *(Poison Dart)*: Shoot single target poison dart.
*   **`tank_provoke`** *(Tank Provoke)*: Taunts enemies.
*   **`tank_immovability`** *(Immovability)*: Ultimate knockback protection.
*   **`tank_last_stand`** *(Last Stand)*: Instant absorption hearts shield.
*   **`berserker_bloodlust`** *(Bloodlust Strength)*: Temporary strength boost.
*   **`berserker_charge`** *(Berserker Charge)*: Leaps forward.
*   **`berserker_rage`** *(Berserker Rage)*: Attack damage scales up as health gets lower.
*   **`healer_circle`** *(Healing Circle)*: Creates healing ground circle.
*   **`healer_purify`** *(Holy Purify Debuffs)*: Clears negative effects.
*   **`healer_resurrection`** *(Resurrection Guard)*: Grants a shield that saves you from death.
*   **`archer_volley`** *(Arrow Volley)*: Launches an arrow wave.
*   **`archer_snipe`** *(Archer Snipe)*: Ranged bonus.
*   **`archer_escape`** *(Archer Escape)*: Launch backward.
*   **`wizard_mana_shield`** *(Mana Shield)*: Absorbs damage using XP.
*   **`wizard_teleport`** *(Wizard Teleport)*: Short blink.
*   **`wizard_spell_steal`** *(Spell Steal)*: Copy targets buffs.
*   **`paladin_smite`** *(Paladin Smite)*: Holy fire strike.
*   **`paladin_shield`** *(Paladin Shield)*: Instantly buffs resistance.
*   **`paladin_aura`** *(Holy Paladin Aura)*: Resistance aura for allies.
*   **`summoner_skeleton` / `summoner_golem` / `summoner_wolfpack`** *(Summoner)*: Summon skeleton archers, iron golems, or wolves.
*   **`brawler_uppercut`** *(Brawler Uppercut)*: Launches targeted entity high into the air.
*   **`brawler_tackle`** *(Brawler Tackle)*: Tackle and pins targets.
*   **`brawler_shockwave`** *(Shockwave Slam)*: Knocks surrounding targets back.
*   **`ninja_teleport`** *(Ninja Teleport)*: Warp directly behind your target.
*   **`ninja_dodge`** *(Ninja Dodge)*: Grants a passive chance to dodge attacks.
*   **`ninja_star_volley`** *(Shuriken Volley)*: Rapidly throws shurikens.
*   **`grappling_hook`** *(Grappling Hook)*: Launches a grappling hook.
*   **`magnet_chest`** *(Magnet Chest)*: Draws all nearby items.
*   **`mineral_sense` / `miner_sense`** *(Mineral Sense)*: Highlights hidden ore blocks and grants haste.
*   **`super_drill`** *(Super Drill)*: Mines in a 3x3 pattern.

---

## 🛠️ Installation & Setup

1.  Download the **ItemEdit.jar** file.
2.  Place the jar file into your server's `plugins` folder.
3.  Restart or load the server.
4.  Open `plugins/ItemEdit/weapon.yml` to view/design your custom weapon keys.
5.  Run `/ie reload` to apply settings!
