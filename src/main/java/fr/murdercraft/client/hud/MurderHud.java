package fr.murdercraft.client.hud;

import fr.murdercraft.client.MurderCraftClientState;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GamePhase;
import fr.murdercraft.game.WinResult;
import fr.murdercraft.roles.Role;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD personnalisé de MurderCraft :
 *   - Bandeau supérieur central : rôle + objectif (visible quelques secondes au début)
 *   - Coin haut-gauche : timer
 *   - Coin haut-droit : nombre de vivants par rôle (selon le rôle du joueur)
 *   - Bannière de fin de partie (animation)
 *
 * Design : minimaliste, moderne, avec ombres et couleurs des rôles.
 */
public class MurderHud {

    private static final int PRIMARY_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x99000000;       // Semi-transparent black
    private static final int ACCENT_COLOR = 0xFFFFAA00;   // Gold accent

    public static void register() {
        HudRenderCallback.EVENT.register(MurderHud::render);
    }

    private static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (!MurderCraftConfig.get().showCustomHud) return;

        GamePhase phase = MurderCraftClientState.getPhase();
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        switch (phase) {
            case STARTING -> renderStarting(ctx, tr, sw, sh);
            case IN_GAME -> renderInGame(ctx, tr, sw, sh);
            case ENDING -> renderEnding(ctx, tr, sw, sh);
            default -> { /* LOBBY : pas de HUD particulier */ }
        }
    }

    private static void renderStarting(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        int sec = MurderCraftClientState.getSecondsLeft();
        Text msg = Text.translatable("murdercraft.hud.starting_in", sec).formatted(Formatting.GOLD, Formatting.BOLD);
        int w = tr.getWidth(msg);
        int x = (sw - w) / 2;
        int y = sh / 3;
        drawPanel(ctx, x - 8, y - 4, w + 16, 14);
        ctx.drawText(tr, msg, x, y, ACCENT_COLOR, true);
    }

    private static void renderInGame(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        Role role = MurderCraftClientState.getMyRole();
        int sec = MurderCraftClientState.getSecondsLeft();
        int round = MurderCraftClientState.getCurrentRound();
        int maxRounds = MurderCraftClientState.getMaxRounds();

        // === Scoreboard style à droite, centré verticalement ===
        // Header + lignes d'info
        Text header = Text.literal("MurderCraft").formatted(Formatting.GOLD, Formatting.BOLD);

        java.util.List<Text> lines = new java.util.ArrayList<>();

        if (round > 0 && maxRounds > 0) {
            lines.add(Text.translatable("murdercraft.hud.round", round, maxRounds)
                    .formatted(Formatting.YELLOW));
        }

        lines.add(Text.translatable("murdercraft.hud.role_label").append(": ")
                .append(role.getDisplayName()));

        String timeStr = formatTime(sec);
        Formatting timeColor = sec <= 30 ? Formatting.RED : (sec <= 60 ? Formatting.GOLD : Formatting.WHITE);
        lines.add(Text.translatable("murdercraft.hud.time").append(": ")
                .append(Text.literal(timeStr).formatted(timeColor)));

        // Calcul largeur du panel (max texte + padding)
        int maxWidth = tr.getWidth(header);
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, tr.getWidth(line));
        }
        int padding = 8;
        int panelWidth = maxWidth + padding * 2;
        int lineHeight = 11;
        int headerHeight = 14;
        int panelHeight = headerHeight + (lines.size() * lineHeight) + padding;

        // Position : à droite, centré verticalement
        int panelX = sw - panelWidth - 4;
        int panelY = (sh - panelHeight) / 2;

        // Fond semi-transparent
        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_COLOR);
        // Bordure haute dorée
        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + 1, ACCENT_COLOR);
        // Bordure basse dorée
        ctx.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, ACCENT_COLOR);
        // Bordure gauche subtile
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelHeight, ACCENT_COLOR);

        // Header centré
        int headerX = panelX + (panelWidth - tr.getWidth(header)) / 2;
        ctx.drawText(tr, header, headerX, panelY + 4, 0xFFFFFFFF, true);

        // Séparateur sous le header
        ctx.fill(panelX + padding, panelY + headerHeight - 1,
                panelX + panelWidth - padding, panelY + headerHeight, 0x66FFAA00);

        // Lignes (alignées à droite, comme un vrai scoreboard Minecraft)
        int textY = panelY + headerHeight + 2;
        for (Text line : lines) {
            int textX = panelX + panelWidth - tr.getWidth(line) - padding;
            ctx.drawText(tr, line, textX, textY, 0xFFFFFFFF, true);
            textY += lineHeight;
        }
    }

    private static void renderEnding(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        WinResult result = MurderCraftClientState.getLastWinResult();
        if (result == null) return;

        long elapsed = System.currentTimeMillis() - MurderCraftClientState.getWinResultShownAt();
        // Animation : grossissement & fade-in sur 1 sec, statique 8 sec, fade-out 1 sec
        float alpha = 1.0f;
        if (elapsed < 1000) {
            alpha = elapsed / 1000.0f;
        } else if (elapsed > 9000) {
            alpha = Math.max(0, 1.0f - (elapsed - 9000) / 1000.0f);
        }
        if (alpha <= 0) return;

        Text title = result.getDisplayText();
        // Affichage gros et centré
        int scale = 3;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1);
        int w = tr.getWidth(title);
        int x = (sw / scale - w) / 2;
        int y = sh / scale / 3;
        int colorWithAlpha = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        ctx.drawText(tr, title, x, y, colorWithAlpha | 0xFF000000, true);
        ctx.getMatrices().pop();
    }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }

    /** Panneau de fond semi-transparent. */
    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG_COLOR);
        // Bordure dorée fine en haut
        ctx.fill(x, y, x + w, y + 1, ACCENT_COLOR);
    }
}
