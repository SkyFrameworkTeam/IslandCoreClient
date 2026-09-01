package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server record exactly. targetUuid: the client already has this from its own
// PartyStatusS2C member list.
public record PartyKickC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<PartyKickC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_kick_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyKickC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, PartyKickC2S::targetUuid,
			PartyKickC2S::new
	);

	@Override
	public CustomPayload.Id<PartyKickC2S> getId() {
		return ID;
	}
}
