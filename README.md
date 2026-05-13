# MurderCraft

> Un mod Minecraft multijoueur inspiré du mode **Murder** de Garry's Mod et des UHC.
> 2 meurtriers cachés, 1 justicier armé, des innocents qui doivent survivre.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-orange)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## ✦ Concept

À chaque début de partie, **3 rôles** sont distribués aléatoirement :

| Rôle | Nombre | Arme | Objectif |
|------|--------|------|----------|
| ☠ **Meurtrier** | 2 (configurable) | Couteau (one-shot) | Éliminer discrètement tous les innocents. Les deux meurtriers **ne savent pas qui est l'autre**. |
| ⚭ **Justicier** | 1 | Pistolet | Identifier les meurtriers et les abattre. Tirer sur un innocent = perte définitive du pistolet ! |
| ☺ **Innocent** | Le reste | Aucune | Survivre, observer, prévenir le justicier. **Un pistolet caché** apparaît pendant la partie : le ramasser promeut en Justicier. |

### Conditions de victoire
- **Les Innocents (+ Justicier) gagnent** si tous les meurtriers sont éliminés.
- **Les Meurtriers gagnent** s'ils éliminent tous les innocents avant la fin du temps imparti.

---

## ⚙ Installation

1. Installe [Fabric Loader](https://fabricmc.net/use/) pour Minecraft 1.21.1
2. Télécharge la dernière version du mod sur [Modrinth](https://modrinth.com/mod/murdercraft)
3. Place le fichier `.jar` dans le dossier `mods/` de ton instance
4. Installe les dépendances :
   - [Fabric API](https://modrinth.com/mod/fabric-api) (**requis**)
   - [Cloth Config](https://modrinth.com/mod/cloth-config) (recommandé — config GUI)
   - [Mod Menu](https://modrinth.com/mod/modmenu) (recommandé — accès à la config in-game)

### Multijoueur
Le mod doit être installé **côté serveur ET côté client**. Le mod ne peut pas être lancé en solo (4 joueurs minimum requis).

---

## 🎮 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/murder join` | Rejoindre le lobby | Tous |
| `/murder leave` | Quitter le lobby | Tous |
| `/murder status` | Voir l'état de la partie | Tous |
| `/murder start` | Démarrer la partie | OP |
| `/murder stop` | Arrêter la partie en cours | OP |
| `/murder config show` | Afficher la config | OP |
| `/murder config minPlayers <n>` | Modifier le min joueurs | OP |
| `/murder config duration <s>` | Durée de la partie | OP |
| `/murder kick <player>` | Retirer un joueur | OP |

---

## ⚒ Compilation / Développement

### Prérequis
- [JDK 21](https://adoptium.net/) ou supérieur
- [IntelliJ IDEA Community](https://www.jetbrains.com/idea/) (recommandé)

### Build manuel
```bash
./gradlew build       # Linux/Mac
gradlew.bat build     # Windows
```
Le fichier `.jar` final est généré dans `build/libs/`.

### Premier setup (IntelliJ)
1. Ouvrir IntelliJ IDEA
2. `File → Open` → sélectionner le dossier `MurderCraft`
3. Attendre que Gradle finisse l'import (il téléchargera Minecraft + Fabric Loom automatiquement)
4. IntelliJ génèrera automatiquement `gradle-wrapper.jar` s'il manque
5. Lancer la config `runClient` ou `runServer`

> **Note** : Si la première fois tu vois une erreur "gradle-wrapper.jar manquant", ouvre un terminal et exécute `gradle wrapper --gradle-version 8.10` (nécessite Gradle installé globalement), ou demande à IntelliJ d'utiliser un Gradle local.

---

## 📦 Publication sur Modrinth (auto)

Le workflow GitHub Actions publie automatiquement sur Modrinth quand tu crées un tag :

```bash
git tag v0.2.0
git push origin v0.2.0
```

Pour que ça fonctionne :
1. Crée un compte sur [Modrinth](https://modrinth.com/)
2. Crée le projet sur Modrinth, note le **slug** (ex: `murdercraft`)
3. Mets à jour `projectId` dans `build.gradle`
4. Génère un **token** Modrinth : https://modrinth.com/settings/pats
5. Dans ton repo GitHub : `Settings → Secrets → Actions → New secret` → nom `MODRINTH_TOKEN`, valeur = ton token

---

## 🎯 Roadmap

- [x] Système de rôles (Meurtrier / Justicier / Innocent)
- [x] Couteau + pistolet + pistolet caché
- [x] GameManager avec phases (Lobby → Starting → InGame → Ending)
- [x] HUD personnalisé
- [x] Configuration GUI (Cloth Config)
- [x] Multilingue (FR / EN)
- [ ] Textures custom (placeholder pour l'instant — à dessiner)
- [ ] Sons custom (gunshot, knife, kill confirm)
- [ ] Système de skins (skin du meurtrier qui le démarque pour les meurtriers entre eux ?)
- [ ] Map dédiée intégrée
- [ ] Statistiques persistantes (kills, parties gagnées)

---

## 📜 License

[MIT](LICENSE) — utilise, modifie, redistribue librement.

---

## 🙌 Crédits

- Concept original : **Garry's Mod — Murder**
- Mod design & dev : **Remilulz_91**
- Inspirations gameplay : UHC, UHC Loup-Garou, Hypixel Murder Mystery
