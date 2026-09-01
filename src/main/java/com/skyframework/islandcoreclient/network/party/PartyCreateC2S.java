package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly.
public record PartyCreateC2S(String name) implements CustomPayload {
	public static final CustomPayload.Id<PartyCreateC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_create_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyCreateC2S::name,
			PartyCreateC2S::new
	);

	@Override
	public CustomPayload.Id<PartyCreateC2S> getId() {
		return ID;
	}
}
