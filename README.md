# MurderCraft

> A multiplayer Minecraft mod inspired by **Garry's Mod Murder**.
> 2 hidden murderers, 1 armed detective, innocents who must survive.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-orange)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Public%20Beta-yellow)](https://modrinth.com/mod/murdercraft)

---

## ✦ Concept

At the start of each round, **three roles** are randomly assigned:

| Role | Count | Weapon | Goal |
|------|-------|--------|------|
| ☠ **Murderer** | 2 (configurable) | Knife (one-shot) | Discreetly eliminate all innocents. **Murderers cannot keep a pistol.** The two murderers don't know each other directly — but they can spot each other thanks to the soul-flame awareness particles. |
| ⚭ **Detective** | 1 | Pistol | Identify the murderers and shoot them. **Shooting an innocent makes you lose your pistol permanently** (dropped on the ground for any innocent to pick up). |
| ☺ **Innocent** | The rest | None | Survive. Help the detective. **Find the hidden pistol** that drops mid-game to become the new Detective. |

### Session structure

A session consists of **4 rounds**. Each round lasts up to **15 minutes**. The team with the most round wins wins the session.

### Tasks (from round 3)

Starting at round 3, **one task** appears per round. Any participant can attempt it:
- **Exploration**: a glowing chest appears nearby with a Mystery Token inside
- **Crafting**: an item is announced (torch, bread, etc.) — first to craft it wins

The winner of the task receives a **30-second window** to use `/murder task reveal <player>` and learn that player's role **privately**. Whether to share the info with others (in voice chat) is entirely up to the player.

### Win conditions
- **Innocents (+ Detective) win** if both murderers are eliminated **OR** the timer runs out
- **Murderers win** if they kill every non-murderer before the timer ends

---

## ⚙ Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Download the latest version from [Modrinth](https://modrinth.com/mod/murdercraft)
3. Place the `.jar` file in your `mods/` folder
4. Install required dependencies:
   - [Fabric API](https://modrinth.com/mod/fabric-api) (**required**)
   - [Cloth Config](https://modrinth.com/mod/cloth-config) (recommended — config GUI)
   - [Mod Menu](https://modrinth.com/mod/modmenu) (recommended — access config in-game)

### Multiplayer
The mod must be installed on **both the server AND every client**. The mod loads in singleplayer but cannot start a match alone (minimum 4 players required, recommended 5+).

---

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/murder join` | Join the lobby | Everyone |
| `/murder leave` | Leave the lobby | Everyone |
| `/murder status` | Show current game state | Everyone |
| `/murder task reveal <player>` | Reveal a player's role (only valid after winning a task) | Everyone |
| `/murder start` | Start the session | OP |
| `/murder stop` | Stop the current game | OP |
| `/murder config show` | Print current config | OP |
| `/murder config minPlayers <n>` | Change min players | OP |
| `/murder config duration <s>` | Change round duration | OP |
| `/murder kick <player>` | Remove a player from lobby | OP |

### Debug commands (debug build only)

The debug build adds `/murder debug ...` commands for testing in solo. **Not available in the public build.**

---

## ⚒ Build from source

### Requirements
- [JDK 21](https://adoptium.net/)
- [IntelliJ IDEA Community](https://www.jetbrains.com/idea/) (recommended)

### Manual build

```bash
./gradlew build                       # Public build (debug OFF)
./gradlew build -PbuildType=debug     # Debug build (debug ON, separate JAR)
```

The output `.jar` is generated in `build/libs/`:
- Public: `murdercraft-X.Y.Z.jar`
- Debug: `murdercraft-debug-X.Y.Z.jar`

### First setup (IntelliJ)
1. Open IntelliJ IDEA
2. `File → Open` → select the `MurderCraft` folder
3. Wait for Gradle to finish importing (it downloads Minecraft + Fabric Loom)
4. Run the `runClient` or `runServer` configuration

See [SETUP.md](SETUP.md) for the full step-by-step guide.

---

## 📦 Automatic Modrinth publishing

GitHub Actions automatically publishes to Modrinth when you push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Required setup:
1. Create your project on [Modrinth](https://modrinth.com/)
2. Generate a Modrinth token at https://modrinth.com/settings/pats
3. Add the token as a GitHub secret named `MODRINTH_TOKEN`

The workflow builds both PUBLIC and DEBUG variants. Only the PUBLIC variant is uploaded to Modrinth. The DEBUG variant is attached to the GitHub Release.

---

## 🎯 Roadmap

- [x] Three-role system (Murderer / Detective / Innocent)
- [x] Knife + pistol + hidden pistol
- [x] GameManager with phases (Lobby → Starting → InGame → Ending)
- [x] Custom HUD (right-side scoreboard style)
- [x] Configuration GUI (Cloth Config)
- [x] Multilingual (FR / EN)
- [x] 4-round session like GMod Murder
- [x] 15-minute rounds
- [x] Pistol drop rules (death, friendly fire, murderer pickup prevention)
- [x] Auto-promotion of innocent picking up hidden pistol
- [x] Task system (Exploration + Crafting) from round 3
- [x] Reveal mechanic via `/murder task reveal`
- [x] Title overlays on screen for important events
- [x] Murderer awareness particles (visible only to fellow murderers)
- [x] Better sounds and particles for weapons
- [x] Public/Debug build variants
- [ ] Custom OGG sound files (currently uses vanilla sounds)
- [ ] Persistent statistics (kills, wins, MVP)
- [ ] Dedicated map integration
- [ ] Forge / NeoForge support

---

## 🐛 Reporting bugs

This is a **public beta**. Bugs are expected. Please report them on the [GitHub Issues page](https://github.com/Remilulz91/MurderCraft/issues) with:
- Mod version (e.g., 0.5.0-beta.1)
- Minecraft version
- Steps to reproduce
- Crash log / video if applicable

---

## 📜 License

Released under the [MIT License](LICENSE) — fork, modify, redistribute freely with attribution.

---

## 🙌 Credits

- **Concept origin**: Garry's Mod — Murder
- **Mod design & development**: Remilulz_91
- **Gameplay inspirations**: UHC, UHC Werewolves, Hypixel Murder Mystery
