package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. targetPartyName: parties have no client-side UUID cache to
// pick from, resolved server-side by name.
public record PartyAllyAddC2S(String targetPartyName) implements CustomPayload {
	public static final CustomPayload.Id<PartyAllyAddC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_ally_add_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyAllyAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyAllyAddC2S::targetPartyName,
			PartyAllyAddC2S::new
	);

	@Override
	public CustomPayload.Id<PartyAllyAddC2S> getId() {
		return ID;
	}
}
