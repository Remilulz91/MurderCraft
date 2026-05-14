package fr.murdercraft.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fr.murdercraft.MurderCraft;
import fr.murdercraft.config.MurderCraftConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

/**
 * Intégration avec Mod Menu : ajoute un bouton de config dans la liste des mods.
 * Génère un écran de configuration GUI complet avec Cloth Config.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MurderCraftConfig cfg = MurderCraftConfig.get();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("config.murdercraft.title"))
                    .setSavingRunnable(MurderCraftConfig::save);

            ConfigEntryBuilder entry = builder.entryBuilder();

            // === Catégorie : Partie ===
            ConfigCategory game = builder.getOrCreateCategory(Text.translatable("config.murdercraft.category.game"));

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.minPlayers"), cfg.minPlayers)
                    .setMin(4).setMax(64).setDefaultValue(5)
                    .setTooltip(Text.translatable("config.murdercraft.minPlayers.tooltip"))
                    .setSaveConsumer(v -> cfg.minPlayers = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.maxPlayers"), cfg.maxPlayers)
                    .setMin(0).setMax(128).setDefaultValue(16)
                    .setSaveConsumer(v -> cfg.maxPlayers = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.gameDuration"), cfg.gameDurationSeconds)
                    .setMin(60).setMax(3600).setDefaultValue(480)
                    .setSaveConsumer(v -> cfg.gameDurationSeconds = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.countdown"), cfg.startCountdownSeconds)
                    .setMin(0).setMax(60).setDefaultValue(10)
                    .setSaveConsumer(v -> cfg.startCountdownSeconds = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.hiddenPistolDelay"), cfg.hiddenPistolSpawnDelaySeconds)
                    .setMin(0).setMax(600).setDefaultValue(60)
                    .setSaveConsumer(v -> cfg.hiddenPistolSpawnDelaySeconds = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.maxRounds"), cfg.maxRounds)
                    .setMin(1).setMax(10).setDefaultValue(4)
                    .setTooltip(Text.translatable("config.murdercraft.maxRounds.tooltip"))
                    .setSaveConsumer(v -> cfg.maxRounds = v).build());

            game.addEntry(entry.startIntField(Text.translatable("config.murdercraft.interRoundSeconds"), cfg.interRoundSeconds)
                    .setMin(0).setMax(120).setDefaultValue(15)
                    .setSaveConsumer(v -> cfg.interRoundSeconds = v).build());

            // === Catégorie : Rôles ===
            ConfigCategory roles = builder.getOrCreateCategory(Text.translatable("config.murdercraft.category.roles"));

            roles.addEntry(entry.startIntField(Text.translatable("config.murdercraft.murdererCount"), cfg.murdererCount)
                    .setMin(1).setMax(8).setDefaultValue(2)
                    .setSaveConsumer(v -> cfg.murdererCount = v).build());

            roles.addEntry(entry.startIntField(Text.translatable("config.murdercraft.detectiveCount"), cfg.detectiveCount)
                    .setMin(0).setMax(4).setDefaultValue(1)
                    .setSaveConsumer(v -> cfg.detectiveCount = v).build());

            // === Catégorie : Gameplay ===
            ConfigCategory gameplay = builder.getOrCreateCategory(Text.translatable("config.murdercraft.category.gameplay"));

            gameplay.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.knifeOneShot"), cfg.knifeOneShot)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> cfg.knifeOneShot = v).build());

            gameplay.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.detectiveLosesGun"), cfg.detectiveLosesGunOnFriendlyFire)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> cfg.detectiveLosesGunOnFriendlyFire = v).build());

            gameplay.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.showNames"), cfg.showPlayerNamesIngame)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> cfg.showPlayerNamesIngame = v).build());

            // === Catégorie : Interface ===
            ConfigCategory ui = builder.getOrCreateCategory(Text.translatable("config.murdercraft.category.ui"));

            ui.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.showHud"), cfg.showCustomHud)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> cfg.showCustomHud = v).build());

            ui.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.showSubtitles"), cfg.showSubtitles)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> cfg.showSubtitles = v).build());

            // === Catégorie : Debug — uniquement visible en build DEBUG ===
            if (MurderCraft.isDebugBuild()) {
                ConfigCategory debug = builder.getOrCreateCategory(Text.translatable("config.murdercraft.category.debug"));

                debug.addEntry(entry.startBooleanToggle(Text.translatable("config.murdercraft.debugAllowMobDamage"), cfg.debugAllowMobDamage)
                        .setDefaultValue(false)
                        .setTooltip(Text.translatable("config.murdercraft.debugAllowMobDamage.tooltip"))
                        .setSaveConsumer(v -> cfg.debugAllowMobDamage = v).build());
            }

            return builder.build();
        };
    }
}
