# Changelog

## [0.5.0-beta.7] - Spawn dispersion + survival enforcement

### Fixed
- **"You are no longer invulnerable!" message spam**: the action bar message
  showing immunity expiration was being sent every server tick (20 times/second
  effectively, since the check ran every 20 ticks). Now sent **once** when the
  immunity expires, then the entry is removed from the tracking map.
- **World border not setting up in debug mode**: `/murder debug start` did not
  trigger `setupWorldBorder()` (only normal `/murder start` did). The world
  border is now also set up when starting a debug game.

### Added
- **Minimum spawn distance between players**: random teleport now ensures
  players don't spawn too close to each other. Scales with player count:
  - 2 players: 80 blocks minimum
  - 3-4 players: 60 blocks
  - 5-8 players: 40 blocks
  - 9-16 players: 30 blocks
  - 17+ players: 20 blocks
  If no position satisfying the constraint can be found after 60 attempts,
  the constraint is relaxed instead of failing.
- **Survival gamemode enforced at game start**: all participants are
  automatically switched to survival mode when a round starts, including
  when starting from a debug build. Ensures everyone takes damage equally
  and the game can actually be tested.

## [0.5.0-beta.6] - Random spawn + spawn immunity + world border

### Added

- **Random spawn at round start**: at the beginning of every round, all participants
  are teleported to random valid land positions within a configurable radius of
  the world spawn (`/setworldspawn`). Ocean, river, and deep ocean biomes are
  automatically excluded so nobody spawns in water.
- **Spawn height + damage immunity**: players spawn 25 blocks (configurable) above
  ground level, with full damage immunity for 8 seconds (configurable). This
  prevents fall damage, lava deaths, and assassination during the chaotic first
  moments of a round.
- **Action bar countdown**: shows "🛡 Damage immunity: Xs" while the buffer is
  active, followed by "⚠ You are no longer invulnerable!" when it expires.
- **Square world border**: a configurable square world border is automatically
  set up at session start, centered on the world spawn. Default size: 500 blocks
  across. The original world border configuration is saved and restored when
  the session ends.
- New config options: `randomSpawnEnabled`, `randomSpawnRadius`, `randomSpawnHeight`,
  `spawnImmunitySeconds`, `useWorldBorder`, `worldBorderSize`.

### Why
Prevents trolling and griefing where innocents would run far away and hide. The
world border forces the action to happen in a defined zone. Random spawn means
players don't all clump at the lobby — they have to navigate to find each other,
which mirrors the GMod Murder experience.

## [0.5.0-beta.5] - Custom items cannot be dropped manually

### Added
- **Manual drop prevention**: pressing Q or dragging out of inventory no longer
  drops the Murderer's Knife, Detective's Pistol, Hidden Pistol or Mystery Token.
  The item is instantly returned to the player with the message
  "⛔ This item cannot be dropped" in the action bar.
- **Death cleanup**: when a player dies, all custom items in their inventory are
  removed before the vanilla drop logic, ensuring nothing leaks via death drops.
  (The Detective's pistol-drop-on-death mechanic still works correctly — it's
  handled explicitly with ammo preservation before the cleanup.)

### Why
Prevents accidental losses (item dropped into lava, off a cliff, into water with
zombies, etc.) and stops players from breaking the game state by intentionally
discarding their role items.

### Internal logic
Detection uses Minecraft's `retainOwnership` flag on ItemEntity: a player-initiated
drop sets the owner UUID, while mod-spawned drops (`GameManager.dropHiddenPistolAt`)
leave it null. The intercept only fires on player-initiated drops.

## [0.5.0-beta.4] - 2-bullet pistol ammo system

### Added (Squeezie ruleset)
- **Pistols now have only 2 bullets** (configurable via the `MAX_AMMO` constant).
  Each pistol — the Detective's starting pistol AND the hidden pistol that drops
  mid-round — starts with 2 bullets. When both are spent, the pistol becomes
  useless (just plays a dry click sound).
- **Ammo persists across drops and pickups**: if a Detective fires one bullet
  and then dies (or friendly-fires, or gets auto-ejected as a Murderer), the
  dropped pistol retains its remaining 1 bullet. The next innocent who picks it
  up inherits that count.
- **Ammo displayed in tooltip**: hover over the pistol in your inventory to see
  "Bullets: X/2" with color coding (green when loaded, red when empty).
- **Action bar feedback after each shot**: shows "🔫 Bullets remaining: X/2"
- **Dry-click sound** (block dispenser fail) when trying to fire an empty pistol.

### Why this rule matters
This dramatically raises the tension for the Detective role. You can't just
spray-and-pray — every shot must count. Misidentifying a Murderer or accidentally
shooting an Innocent (which still loses you the pistol permanently anyway) now
also wastes a precious bullet.

## [0.5.0-beta.3] - Proper attribution

### Changed
- **Credits and attribution corrected**: The ruleset implemented in this mod is
  the variant designed by **Squeezie & Théodore Bonnet**, not the original
  Garry's Mod Murder ruleset. Updated:
  - Mod description (visible in Mod Menu)
  - README.md credits section
  - GitHub repository description (manual update by maintainer)
  - Modrinth project description (manual update by maintainer)
- GMod Murder is still mentioned as the genre origin, but Squeezie & Théodore
  Bonnet now receive primary credit for the specific rules this mod implements.

## [0.5.0-beta.2] - Config security fix

### Security
- **Debug flags are now ignored in PUBLIC builds** even if set to `true` in the config
  file. Previously, a server admin could edit `config/murdercraft.json` to set
  `enableDebugCommands: true` or `debugAllowMobDamage: true` and effectively turn
  the public build into a debug build. Now, the public build is truly locked down.
- A warning is logged on startup if the config file contains debug flags but they
  are being ignored.

### Changed
- The "Debug" category in the Mod Menu config screen is now hidden in PUBLIC builds
  (only visible in DEBUG builds).

## [0.5.0-beta.1] - First public beta release

This is the **first public beta** of MurderCraft. The mod is feature-complete
but has not yet been thoroughly battle-tested in real multiplayer matches.
Please report any bugs you find on the GitHub issue tracker.

### Added
- **Dual build system**: PUBLIC build (production) and DEBUG build (testing)
- The PUBLIC build has debug commands and mob damage **OFF by default**
- The DEBUG build has both enabled by default for solo testing
- Build type is detected at runtime via `murdercraft.build.properties` resource

### Fixed
- **Infinite pickup loop**: when a murderer received a pistol, the inventory check
  would drop it at their feet, where Minecraft auto-pickup would re-grab it,
  spamming chat and inventory. Now the pistol is thrown forward with velocity,
  has a 3-second pickup delay, and a 3-second per-player cooldown between ejections.

### Changed
- **HUD relocated to the right side** (vanilla scoreboard style), vertically centered.
  Shows "MurderCraft" header + round + role + time, right-aligned text on a panel
  with golden borders.
- All project documentation translated to English (README, SETUP, CHANGELOG)

## [0.4.0] - Phase C - Polish & juiciness

### Added
- **Title overlays** on screen for: role assignment, round start, end of round, task completion
- **Murderer awareness**: subtle soul-flame particles appear above other murderers, visible only to fellow murderers — finally lets the murderer team coordinate
- **Better gunshot effects**: combo of 2 sounds + muzzle flash particles (smoke + flame) for a real "bang" feel
- **Better knife effects**: combo of 2 sounds + blood particles at impact point
- **Role-specific briefing sounds**: wither ambient for murderer, bell for detective, toast for innocents
- **End-of-round sounds**: triumphant sound for innocent wins, wither death for murderer wins
- **Kill confirm sound** for detective when shooting a murderer (level-up sound)
- **Security flag** `enableDebugCommands` in config — disable to prevent OPs from cheating with `/murder debug` on production servers

### Changed
- Pistol sound is now a layered explosion + firecharge (was firework rocket)
- Knife sound is now trident hit + wool break (was lava extinguish)

## [0.3.0] - Phase B - Tasks & Reveal mechanic

### Added
- **Task system** activated from round 3 onwards
- **Exploration Task**: a glowing chest with a Mystery Token spawns 15-40 blocks from a random participant. First to grab the token wins a clue.
- **Crafting Task**: a random recipe (torch, ladder, bread, etc.) is announced. First to craft the target wins a clue.
- **Mystery Token** new item with custom golden texture
- **Reveal mechanic**: task winner gets a 30-second window to use `/murder task reveal <player>` to learn that player's role (privately)
- New command `/murder task reveal <player>`

### Fixed
- **Time-out win condition**: when the timer expires with surviving innocents, INNOCENTS WIN (was incorrectly: murderers win). Matches the Squeezie / Théodore Bonnet ruleset.

## [0.2.0] - Phase A - Ruleset alignment (Squeezie / Théodore Bonnet variant)

### Added
- **Multi-round session system**: 4 rounds per session by default (matching the Squeezie / Théodore Bonnet variant), configurable
- **15 minutes default round duration** (was 8 min)
- **Player scaling up to 32** (was 16)
- **Round counter in HUD** showing "Round X/Y" during play
- **Session final score** with cumulative wins per team
- **Murderers cannot keep a pistol**: any pistol in their inventory is auto-dropped
- **Detective pistol drop on friendly fire**: pistol falls to the ground (visible glow), pickable by innocents only
- **Detective pistol drop on death**: same mechanic when detective dies
- **Auto-promotion**: an innocent walking over a hidden pistol becomes Detective instantly (no right-click needed)
- New config options: `maxRounds`, `interRoundSeconds`

### Fixed
- Pistol drop now properly drops a HIDDEN_PISTOL entity instead of just removing it from inventory

## [0.1.0] - 2026-05-13 — Initial release

### Added
- Système de rôles : Meurtrier, Justicier, Innocent, Spectateur
- 3 items custom : Couteau du Meurtrier, Pistolet du Justicier, Pistolet Caché
- GameManager complet avec phases (Lobby → Starting → InGame → Ending)
- Conditions de victoire (élimination de tous les meurtriers OU temps écoulé)
- HUD personnalisé (rôle + timer + bannière de fin)
- Commandes `/murder` (join, leave, start, stop, status, config, kick)
- Configuration GUI via Cloth Config + Mod Menu
- Spawn aléatoire du pistolet caché qui promeut un innocent en justicier
- Tir ami sur innocent = perte définitive du pistolet
- Multilingue : Français + Anglais
- GitHub Actions : build CI + publication automatique Modrinth

### Known issues
- Textures items sont des placeholders (à dessiner)
- Pas encore de sons custom
