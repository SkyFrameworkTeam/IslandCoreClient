package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record PartyStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "party_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyStatusRequestC2S> CODEC =
			PacketCodec.unit(new PartyStatusRequestC2S());

	@Override
	public CustomPayload.Id<PartyStatusRequestC2S> getId() {
		return ID;
	}
}
