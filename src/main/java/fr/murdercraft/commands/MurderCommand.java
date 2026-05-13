package fr.murdercraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.game.GamePhase;
import fr.murdercraft.roles.Role;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Commandes /murder :
 *   - /murder join             — Rejoindre le lobby
 *   - /murder leave            — Quitter le lobby
 *   - /murder start            — Démarrer la partie (OP requis)
 *   - /murder stop             — Arrêter la partie en cours (OP)
 *   - /murder status           — Voir l'état de la partie
 *   - /murder config <opt> <v> — Modifier la config (OP)
 *   - /murder kick <player>    — Retirer un joueur de la partie (OP)
 */
public class MurderCommand {

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            net.minecraft.command.CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("murder")
                .then(CommandManager.literal("join")
                        .executes(MurderCommand::onJoin))
                .then(CommandManager.literal("leave")
                        .executes(MurderCommand::onLeave))
                .then(CommandManager.literal("start")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(MurderCommand::onStart))
                .then(CommandManager.literal("stop")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(MurderCommand::onStop))
                .then(CommandManager.literal("status")
                        .executes(MurderCommand::onStatus))
                .then(CommandManager.literal("config")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("minPlayers")
                                .then(CommandManager.argument("value", IntegerArgumentType.integer(4, 64))
                                        .executes(ctx -> setMinPlayers(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(CommandManager.literal("duration")
                                .then(CommandManager.argument("value", IntegerArgumentType.integer(60, 3600))
                                        .executes(ctx -> setDuration(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(CommandManager.literal("show")
                                .executes(MurderCommand::showConfig)))
                .then(CommandManager.literal("kick")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MurderCommand::onKick)))
        );
    }

    private static int onJoin(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        boolean added = GameManager.get().addParticipant(p);
        if (added) {
            p.sendMessage(Text.translatable("murdercraft.command.joined").formatted(Formatting.GREEN), false);
        } else {
            p.sendMessage(Text.translatable("murdercraft.command.cant_join").formatted(Formatting.RED), false);
        }
        return 1;
    }

    private static int onLeave(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        GameManager.get().removeParticipant(p);
        p.sendMessage(Text.translatable("murdercraft.command.left").formatted(Formatting.YELLOW), false);
        return 1;
    }

    private static int onStart(CommandContext<ServerCommandSource> ctx) {
        boolean started = GameManager.get().startGame(ctx.getSource().getServer());
        if (started) {
            ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.command.starting").formatted(Formatting.GOLD), true);
        } else {
            ctx.getSource().sendError(Text.translatable("murdercraft.command.cant_start"));
        }
        return started ? 1 : 0;
    }

    private static int onStop(CommandContext<ServerCommandSource> ctx) {
        GameManager.get().stopGame(true);
        ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.command.stopped").formatted(Formatting.YELLOW), true);
        return 1;
    }

    private static int onStatus(CommandContext<ServerCommandSource> ctx) {
        GameManager gm = GameManager.get();
        GamePhase phase = gm.getPhase();
        ServerCommandSource src = ctx.getSource();

        src.sendFeedback(() -> Text.literal("═══ MurderCraft ═══").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> Text.translatable("murdercraft.status.phase",
                Text.translatable("murdercraft.phase." + phase.name().toLowerCase()).formatted(Formatting.AQUA)), false);
        src.sendFeedback(() -> Text.translatable("murdercraft.status.players",
                gm.getParticipants().size(), MurderCraftConfig.get().minPlayers), false);

        if (phase == GamePhase.IN_GAME) {
            src.sendFeedback(() -> Text.translatable("murdercraft.status.time_left",
                    gm.getGameTicksLeft() / 20).formatted(Formatting.WHITE), false);
            src.sendFeedback(() -> Text.translatable("murdercraft.status.murderers_alive",
                    gm.getRoleManager().countAliveByRole(Role.MURDERER)).formatted(Formatting.DARK_RED), false);
            src.sendFeedback(() -> Text.translatable("murdercraft.status.innocents_alive",
                    gm.getRoleManager().countAliveByRole(Role.INNOCENT)).formatted(Formatting.GREEN), false);
        }
        return 1;
    }

    private static int setMinPlayers(CommandContext<ServerCommandSource> ctx, int value) {
        MurderCraftConfig.get().minPlayers = value;
        MurderCraftConfig.save();
        ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.command.config_set", "minPlayers", value)
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setDuration(CommandContext<ServerCommandSource> ctx, int value) {
        MurderCraftConfig.get().gameDurationSeconds = value;
        MurderCraftConfig.save();
        ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.command.config_set", "gameDurationSeconds", value)
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> ctx) {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("═══ Config MurderCraft ═══").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> Text.literal("  minPlayers: " + cfg.minPlayers), false);
        src.sendFeedback(() -> Text.literal("  maxPlayers: " + cfg.maxPlayers), false);
        src.sendFeedback(() -> Text.literal("  gameDurationSeconds: " + cfg.gameDurationSeconds), false);
        src.sendFeedback(() -> Text.literal("  murdererCount: " + cfg.murdererCount), false);
        src.sendFeedback(() -> Text.literal("  detectiveCount: " + cfg.detectiveCount), false);
        src.sendFeedback(() -> Text.literal("  hiddenPistolSpawnDelaySeconds: " + cfg.hiddenPistolSpawnDelaySeconds), false);
        return 1;
    }

    private static int onKick(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        boolean removed = GameManager.get().removeParticipant(target);
        if (removed) {
            ctx.getSource().sendFeedback(() ->
                    Text.translatable("murdercraft.command.kicked", target.getName().getString())
                            .formatted(Formatting.YELLOW), true);
        }
        return removed ? 1 : 0;
    }
}
