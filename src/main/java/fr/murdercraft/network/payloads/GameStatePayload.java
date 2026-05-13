package fr.murdercraft.network.payloads;

import fr.murdercraft.MurderCraft;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet S->C : informe le client de l'état actuel de la partie.
 *   - phase : LOBBY / STARTING / IN_GAME / ENDING
 *   - secondsLeft : temps restant en secondes
 *   - winResult : résultat de la partie (uniquement si phase == ENDING), "" sinon
 */
public record GameStatePayload(String phase, int secondsLeft, String winResult) implements CustomPayload {

    public static final Identifier IDENTIFIER = MurderCraft.id("game_state");
    public static final Id<GameStatePayload> ID = new Id<>(IDENTIFIER);

    public static final PacketCodec<PacketByteBuf, GameStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, GameStatePayload::phase,
            PacketCodecs.INTEGER, GameStatePayload::secondsLeft,
            PacketCodecs.STRING, GameStatePayload::winResult,
            GameStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
