package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose. Only succeeds if PartyDisbandRequestC2S was sent within the last 15s.
public record PartyDisbandConfirmC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyDisbandConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "party_disband_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyDisbandConfirmC2S> CODEC = PacketCodec.unit(new PartyDisbandConfirmC2S());

	@Override
	public CustomPayload.Id<PartyDisbandConfirmC2S> getId() {
		return ID;
	}
}
