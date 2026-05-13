# Changelog

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
