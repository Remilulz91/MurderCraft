# Changelog

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
