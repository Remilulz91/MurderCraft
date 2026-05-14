package fr.murdercraft.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.game.WinResult;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.roles.Role;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Sous-commandes /murder debug ... — UTILISATION TEST UNIQUEMENT.
 *
 * Toutes ces commandes nécessitent OP (permission level 2). Elles permettent
 * de tester le mod en solo ou avec peu de joueurs sans respecter les règles
 * normales (min players, rôles, etc.).
 *
 * Sous-commandes :
 *   /murder debug start [role]      — Démarre une partie immédiatement (1+ joueur),
 *                                     optionnellement avec un rôle forcé pour soi
 *   /murder debug setrole <role>    — Change son propre rôle en cours de partie
 *   /murder debug giveitem <item>   — Se donner un item (knife/pistol/hidden)
 *   /murder debug spawnpistol       — Spawn le pistolet caché à 2 blocs devant soi
 *   /murder debug endwith <result>  — Force la fin de partie avec un résultat
 *   /murder debug info              — Affiche l'état interne de GameManager
 */
public class DebugCommand {

    private static final SuggestionProvider<ServerCommandSource> ROLE_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("murderer");
        builder.suggest("detective");
        builder.suggest("innocent");
        return builder.buildFuture();
    };

    /**
     * Construit le sous-arbre de commande "debug" à attacher sous /murder.
     */
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("debug")
                // Double condition : OP (level 2) ET flag enableDebugCommands dans la config
                .requires(src -> src.hasPermissionLevel(2)
                        && MurderCraftConfig.get().enableDebugCommands)

                // start [role]
                .then(CommandManager.literal("start")
                        .executes(ctx -> debugStart(ctx, null))
                        .then(CommandManager.argument("role", StringArgumentType.word())
                                .suggests(ROLE_SUGGESTIONS)
                                .executes(ctx -> debugStart(ctx, parseRole(StringArgumentType.getString(ctx, "role"))))))

                // setrole <role>
                .then(CommandManager.literal("setrole")
                        .then(CommandManager.argument("role", StringArgumentType.word())
                                .suggests(ROLE_SUGGESTIONS)
                                .executes(ctx -> setRole(ctx, parseRole(StringArgumentType.getString(ctx, "role"))))))

                // giveitem <knife|pistol|hidden>
                .then(CommandManager.literal("giveitem")
                        .then(CommandManager.literal("knife").executes(ctx -> giveItem(ctx, ModItems.KNIFE)))
                        .then(CommandManager.literal("pistol").executes(ctx -> giveItem(ctx, ModItems.PISTOL)))
                        .then(CommandManager.literal("hidden").executes(ctx -> giveItem(ctx, ModItems.HIDDEN_PISTOL))))

                // spawnpistol
                .then(CommandManager.literal("spawnpistol")
                        .executes(DebugCommand::spawnPistolNear))

                // endwith <innocents|murderers|draw>
                .then(CommandManager.literal("endwith")
                        .then(CommandManager.literal("innocents").executes(ctx -> forceEnd(ctx, WinResult.INNOCENTS_WIN)))
                        .then(CommandManager.literal("murderers").executes(ctx -> forceEnd(ctx, WinResult.MURDERERS_WIN)))
                        .then(CommandManager.literal("draw").executes(ctx -> forceEnd(ctx, WinResult.DRAW))))

                // toggle mobdamage — bascule debugAllowMobDamage
                .then(CommandManager.literal("toggle")
                        .then(CommandManager.literal("mobdamage")
                                .executes(DebugCommand::toggleMobDamage)))

                // info
                .then(CommandManager.literal("info")
                        .executes(DebugCommand::info));
    }

    private static int toggleMobDamage(CommandContext<ServerCommandSource> ctx) {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        cfg.debugAllowMobDamage = !cfg.debugAllowMobDamage;
        MurderCraftConfig.save();
        boolean state = cfg.debugAllowMobDamage;
        ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.debug.mobdamage.toggled",
                Text.literal(state ? "ON" : "OFF")
                        .formatted(state ? Formatting.GREEN : Formatting.GRAY))
                .formatted(Formatting.LIGHT_PURPLE), true);
        return 1;
    }

    private static Role parseRole(String s) {
        return switch (s.toLowerCase()) {
            case "murderer" -> Role.MURDERER;
            case "detective" -> Role.DETECTIVE;
            case "innocent" -> Role.INNOCENT;
            default -> null;
        };
    }

    // === Handlers ===

    private static int debugStart(CommandContext<ServerCommandSource> ctx, Role forcedRole) throws CommandSyntaxException {
        ServerPlayerEntity caller = ctx.getSource().getPlayerOrThrow();
        boolean ok = GameManager.get().debugStart(ctx.getSource().getServer(), caller, forcedRole);
        if (ok) {
            String roleStr = forcedRole != null ? forcedRole.name() : "random";
            caller.sendMessage(Text.translatable("murdercraft.debug.start.success", roleStr)
                    .formatted(Formatting.LIGHT_PURPLE), false);
        } else {
            ctx.getSource().sendError(Text.translatable("murdercraft.debug.start.fail"));
        }
        return ok ? 1 : 0;
    }

    private static int setRole(CommandContext<ServerCommandSource> ctx, Role role) throws CommandSyntaxException {
        if (role == null) {
            ctx.getSource().sendError(Text.translatable("murdercraft.debug.invalid_role"));
            return 0;
        }
        ServerPlayerEntity caller = ctx.getSource().getPlayerOrThrow();
        boolean ok = GameManager.get().setPlayerRoleAndItems(caller, role);
        if (ok) {
            caller.sendMessage(Text.translatable("murdercraft.debug.setrole.success")
                    .append(role.getDisplayName())
                    .formatted(Formatting.LIGHT_PURPLE), false);
        } else {
            ctx.getSource().sendError(Text.translatable("murdercraft.debug.setrole.fail"));
        }
        return ok ? 1 : 0;
    }

    private static int giveItem(CommandContext<ServerCommandSource> ctx, Item item) throws CommandSyntaxException {
        ServerPlayerEntity caller = ctx.getSource().getPlayerOrThrow();
        caller.getInventory().insertStack(new ItemStack(item));
        caller.sendMessage(Text.translatable("murdercraft.debug.giveitem.success",
                Text.translatable(item.getTranslationKey()))
                .formatted(Formatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int spawnPistolNear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity caller = ctx.getSource().getPlayerOrThrow();
        boolean ok = GameManager.get().forceSpawnHiddenPistolNear(caller);
        if (ok) {
            caller.sendMessage(Text.translatable("murdercraft.debug.spawnpistol.success")
                    .formatted(Formatting.LIGHT_PURPLE), false);
        } else {
            ctx.getSource().sendError(Text.translatable("murdercraft.debug.spawnpistol.fail"));
        }
        return ok ? 1 : 0;
    }

    private static int forceEnd(CommandContext<ServerCommandSource> ctx, WinResult result) {
        GameManager.get().forceEndGame(result);
        ctx.getSource().sendFeedback(() -> Text.translatable("murdercraft.debug.endwith.success",
                result.getDisplayText()).formatted(Formatting.LIGHT_PURPLE), true);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        GameManager gm = GameManager.get();
        ServerCommandSource src = ctx.getSource();

        src.sendFeedback(() -> Text.literal("═══ MurderCraft DEBUG ═══").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        src.sendFeedback(() -> Text.literal("Phase: ").append(Text.literal(gm.getPhase().name()).formatted(Formatting.AQUA)), false);
        src.sendFeedback(() -> Text.literal("Debug mode: ").append(
                Text.literal(gm.isDebugMode() ? "ON (win conditions disabled)" : "off")
                        .formatted(gm.isDebugMode() ? Formatting.LIGHT_PURPLE : Formatting.GRAY)), false);
        boolean mobDmg = MurderCraftConfig.get().debugAllowMobDamage;
        src.sendFeedback(() -> Text.literal("Mob damage: ").append(
                Text.literal(mobDmg ? "ON" : "off")
                        .formatted(mobDmg ? Formatting.LIGHT_PURPLE : Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("Participants: " + gm.getParticipants().size()), false);
        src.sendFeedback(() -> Text.literal("Min players (config): " + MurderCraftConfig.get().minPlayers), false);

        if (gm.isGameActive() || gm.getPhase().name().equals("ENDING")) {
            src.sendFeedback(() -> Text.literal("Murderers alive: " + gm.getRoleManager().countAliveByRole(Role.MURDERER))
                    .formatted(Formatting.DARK_RED), false);
            src.sendFeedback(() -> Text.literal("Detectives alive: " + gm.getRoleManager().countAliveByRole(Role.DETECTIVE))
                    .formatted(Formatting.BLUE), false);
            src.sendFeedback(() -> Text.literal("Innocents alive: " + gm.getRoleManager().countAliveByRole(Role.INNOCENT))
                    .formatted(Formatting.GREEN), false);
            src.sendFeedback(() -> Text.literal("Game ticks left: " + gm.getGameTicksLeft()
                    + " (" + (gm.getGameTicksLeft() / 20) + "s)"), false);
        }
        return 1;
    }
}
