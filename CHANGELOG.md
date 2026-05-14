# Changelog

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
- **Time-out win condition**: when the timer expires with surviving innocents, INNOCENTS WIN (was incorrectly: murderers win). Matches official GMod Murder rules.

## [0.2.0] - Phase A - Authentic GMod Murder rules

### Added
- **Multi-round session system**: 4 rounds per session by default (like GMod Murder), configurable
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
