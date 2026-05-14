# MurderCraft — Quick Setup Guide

This project is ready to be opened in IntelliJ IDEA. Follow the steps below.

## 1. Install prerequisites

- **JDK 21**: https://adoptium.net/temurin/releases/?version=21
- **IntelliJ IDEA Community**: https://www.jetbrains.com/idea/download/
- **Git**: https://git-scm.com/downloads

## 2. Open the project in IntelliJ

1. Launch IntelliJ IDEA
2. `File → Open...`
3. Select the `MurderCraft` folder (NOT a subfolder)
4. IntelliJ detects a Gradle project → **Trust Project**
5. Wait 5–10 minutes: Gradle downloads Minecraft, Fabric Loom, mappings, and all dependencies

## 3. Configure the Java version

`File → Project Structure → SDKs` → add your JDK 21
`File → Project Structure → Project → SDK` → select Java 21

## 4. Generate the Gradle wrapper (if missing)

If IntelliJ complains that `gradle-wrapper.jar` is missing:

**Option A** — Let IntelliJ use its local Gradle:
- `Settings → Build → Gradle → Use Gradle from: → Specified location`

**Option B** — Generate the wrapper from the command line (if Gradle is installed):
```bash
gradle wrapper --gradle-version 8.10
```

**Option C** — Download the official wrapper.jar:
- Copy `gradle-wrapper.jar` from any Fabric mod, or download from https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar
- Place it at `gradle/wrapper/gradle-wrapper.jar`

## 5. Launch Minecraft in dev mode

Once the project is imported, IntelliJ will have generated the run configurations:

- **runClient**: launches Minecraft 1.21.1 with the mod active (client mode)
- **runServer**: launches a Minecraft 1.21.1 dedicated server with the mod

To launch multiple clients, right-click on `runClient` → `Modify Run Configuration` → `Allow multiple instances`.

## 6. Test a game

1. Launch `runServer` (eula is auto-accepted in dev mode)
2. Launch `runClient` twice (two MC windows)
3. On the server, type `op <your_username>` to become OP
4. On each client: `Multiplayer → Direct Connect → localhost`
5. Once connected, all players type `/murder join`
6. When you have ≥ 5 players (or the configured minimum): `/murder start`

> Tip: to test with fewer players: `/murder config minPlayers 2`

## 7. Build the final JAR

Two build variants are available:

```bash
./gradlew build                       # PUBLIC build (debug OFF by default)
./gradlew build -PbuildType=debug     # DEBUG build (debug ON, separate JAR)
```

The output `.jar` is in `build/libs/`:
- Public: `murdercraft-X.Y.Z.jar`
- Debug: `murdercraft-debug-X.Y.Z.jar`

Rename if needed and place into the `mods/` folder of a normal Minecraft Fabric instance.

## 8. Publishing on Modrinth

See the README.md, section "Automatic Modrinth publishing".

---

## Common problems

### "Cannot resolve symbol PlayerEntity / Item / ..."
Gradle hasn't finished downloading the Yarn mappings. Wait, or run `./gradlew genSources`.

### "Java 21 not found"
Make sure JAVA_HOME points to your JDK 21. On Windows: `System → Environment Variables`.

### "Mod Menu / Cloth Config ClassNotFoundException"
These mods are declared as "recommended", not required. The mod works without them but loses the config screen. They are included as dev dependencies in `build.gradle`.

### Build fails with "incompatible types: Reference<SoundEvent>"
Some sound events in Yarn 1.21.1 are `RegistryEntry.Reference<SoundEvent>` and need `.value()` to extract the actual `SoundEvent`. If you see this error, add `.value()` to the affected `SoundEvents.XXX` call.
