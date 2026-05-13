package fr.murdercraft.roles;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Enum des 3 rôles possibles dans MurderCraft.
 */
public enum Role {
    MURDERER(
            "murderer",
            Formatting.DARK_RED,
            "☠"   // ☠
    ),
    DETECTIVE(
            "detective",
            Formatting.BLUE,
            "⚭"   // ⚭ (badge)
    ),
    INNOCENT(
            "innocent",
            Formatting.GREEN,
            "☺"   // ☺
    ),
    SPECTATOR(
            "spectator",
            Formatting.GRAY,
            "○"   // ○
    );

    private final String id;
    private final Formatting color;
    private final String icon;

    Role(String id, Formatting color, String icon) {
        this.id = id;
        this.color = color;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public Formatting getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    /** Texte localisé du nom du rôle, formaté avec sa couleur. */
    public Text getDisplayName() {
        return Text.translatable("murdercraft.role." + id).formatted(color);
    }

    /** Description longue du rôle (briefing au début de partie). */
    public Text getDescription() {
        return Text.translatable("murdercraft.role." + id + ".description").formatted(color);
    }

    /** Le but du rôle à atteindre. */
    public Text getObjective() {
        return Text.translatable("murdercraft.role." + id + ".objective");
    }
}
