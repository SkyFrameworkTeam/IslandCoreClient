package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly.
public record PartyAllyRemoveC2S(String targetPartyName) implements CustomPayload {
	public static final CustomPayload.Id<PartyAllyRemoveC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_ally_remove_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyAllyRemoveC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyAllyRemoveC2S::targetPartyName,
			PartyAllyRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<PartyAllyRemoveC2S> getId() {
		return ID;
	}
}
