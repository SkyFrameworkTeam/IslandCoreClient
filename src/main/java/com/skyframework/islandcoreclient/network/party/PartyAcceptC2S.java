package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record PartyAcceptC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyAcceptC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_accept_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyAcceptC2S> CODEC = PacketCodec.unit(new PartyAcceptC2S());

	@Override
	public CustomPayload.Id<PartyAcceptC2S> getId() {
		return ID;
	}
}
