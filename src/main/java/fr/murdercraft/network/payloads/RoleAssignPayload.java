package fr.murdercraft.network.payloads;

import fr.murdercraft.MurderCraft;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet S->C : informe le client du rôle attribué à son joueur.
 * Envoyé au début de la partie et à chaque changement (ex: promotion en détective).
 */
public record RoleAssignPayload(String role) implements CustomPayload {

    public static final Identifier IDENTIFIER = MurderCraft.id("role_assign");
    public static final Id<RoleAssignPayload> ID = new Id<>(IDENTIFIER);

    public static final PacketCodec<PacketByteBuf, RoleAssignPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, RoleAssignPayload::role,
            RoleAssignPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
