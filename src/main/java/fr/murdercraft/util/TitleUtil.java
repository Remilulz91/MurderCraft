package fr.murdercraft.util;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Helpers pour envoyer des titles/subtitles à l'écran des joueurs.
 *
 * Le système de title de Minecraft permet d'afficher un gros texte centré
 * et un sous-titre, avec contrôle des fade-in / stay / fade-out.
 */
public class TitleUtil {

    /**
     * Affiche un titre + sous-titre à l'écran du joueur.
     *
     * @param fadeInTicks  durée du fade-in (par défaut 10)
     * @param stayTicks    durée d'affichage stable (par défaut 60)
     * @param fadeOutTicks durée du fade-out (par défaut 10)
     */
    public static void sendTitle(ServerPlayerEntity player, Text title, Text subtitle,
                                  int fadeInTicks, int stayTicks, int fadeOutTicks) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeInTicks, stayTicks, fadeOutTicks));
        if (subtitle != null) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        }
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    /** Version simplifiée avec timings standards (10/60/10 ticks). */
    public static void sendTitle(ServerPlayerEntity player, Text title, Text subtitle) {
        sendTitle(player, title, subtitle, 10, 60, 10);
    }

    /** Version pour un titre dramatique et long (15/100/20). */
    public static void sendDramaticTitle(ServerPlayerEntity player, Text title, Text subtitle) {
        sendTitle(player, title, subtitle, 15, 100, 20);
    }

    /** Affiche un message dans la barre d'action (au-dessus de l'hotbar). */
    public static void sendActionBar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }
}
