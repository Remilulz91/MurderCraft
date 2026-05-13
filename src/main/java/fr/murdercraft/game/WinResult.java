package fr.murdercraft.game;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Résultat d'une partie : qui a gagné ?
 */
public enum WinResult {
    INNOCENTS_WIN("innocents", Formatting.GREEN),
    MURDERERS_WIN("murderers", Formatting.DARK_RED),
    DRAW("draw", Formatting.YELLOW),
    CANCELED("canceled", Formatting.GRAY);

    private final String id;
    private final Formatting color;

    WinResult(String id, Formatting color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public Formatting getColor() {
        return color;
    }

    public Text getDisplayText() {
        return Text.translatable("murdercraft.win." + id).formatted(color, Formatting.BOLD);
    }
}
