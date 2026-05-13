# Setup MurderCraft — Guide rapide

Ce projet est prêt à être ouvert dans IntelliJ IDEA. Suit les étapes ci-dessous.

## 1. Installer les prérequis

- **JDK 21** : https://adoptium.net/temurin/releases/?version=21
- **IntelliJ IDEA Community** : https://www.jetbrains.com/idea/download/
- **Git** : https://git-scm.com/downloads

## 2. Ouvrir le projet dans IntelliJ

1. Lance IntelliJ IDEA
2. `File → Open...`
3. Sélectionne le dossier `MurderCraft` (PAS un sous-dossier)
4. IntelliJ détecte un projet Gradle → **Trust Project**
5. Attendre 5-10 min : Gradle télécharge Minecraft, Fabric Loom, les mappings et toutes les dépendances

## 3. Régler la version Java

`File → Project Structure → SDKs` → ajouter ton JDK 21
`File → Project Structure → Project → SDK` → sélectionner Java 21

## 4. Générer le wrapper Gradle (si manquant)

Si IntelliJ se plaint que `gradle-wrapper.jar` est manquant :

**Option A** — Laisser IntelliJ utiliser son Gradle local :
- `Settings → Build → Gradle → Use Gradle from: → Specified location`
- Pointer vers une installation Gradle locale (ou laisser "wrapper")

**Option B** — Générer le wrapper en ligne de commande (si tu as Gradle installé) :
```bash
gradle wrapper --gradle-version 8.10
```

**Option C** — Télécharger le wrapper.jar officiel :
- Copie le fichier `gradle-wrapper.jar` depuis n'importe quel projet Fabric, ou télécharge-le depuis https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar
- Place-le dans `gradle/wrapper/gradle-wrapper.jar`

## 5. Lancer Minecraft en mode dev

Une fois le projet importé, IntelliJ aura généré automatiquement les configurations de run :

- **runClient** : lance Minecraft 1.21.1 avec le mod activé (mode client)
- **runServer** : lance un serveur Minecraft 1.21.1 dédié avec le mod

Tu peux lancer plusieurs clients en cliquant droit sur `runClient` → `Modify Run Configuration` → `Allow multiple instances`.

## 6. Tester une partie

1. Lance `runServer` (Eula = true automatiquement)
2. Lance `runClient` deux fois (deux fenêtres MC)
3. Sur le serveur, tape `op <ton_pseudo>` pour devenir OP
4. Sur chaque client : `Multiplayer → Direct Connect → localhost`
5. Une fois connectés, tapez `/murder join` chacun
6. Quand vous êtes ≥ 5 (ou le min configuré) : `/murder start`

> Astuce : pour tester avec moins de joueurs : `/murder config minPlayers 2`

## 7. Build le jar final

```bash
./gradlew build        # Linux/Mac
gradlew.bat build      # Windows
```

Le `.jar` se trouve dans `build/libs/murdercraft-0.1.0.jar`. Renomme-le si besoin et place-le dans `mods/` d'une instance Minecraft Fabric normale.

## 8. Publication sur Modrinth

Lis le README.md, section "Publication sur Modrinth".

---

## Problèmes courants

### "Cannot resolve symbol PlayerEntity / Item / ..."
Gradle n'a pas fini de télécharger les mappings Yarn. Attendre, ou `gradlew genSources`.

### "Java 21 not found"
Vérifier que JAVA_HOME pointe vers ton JDK 21. Sous Windows : `Système → Variables d'environnement`.

### "Mod Menu / Cloth Config ClassNotFoundException"
Ces deux mods sont déclarés comme "recommandés", pas requis. Le mod fonctionne sans eux mais sans écran de config. Pour les inclure en dev, ils sont déjà dans `build.gradle`.
