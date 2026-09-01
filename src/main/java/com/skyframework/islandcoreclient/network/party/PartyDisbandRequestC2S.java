package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose. Arms the server's 15s disband confirmation window — mirrors the two-step
// IslandDeleteRequestC2S/IslandDeleteConfirmC2S flow, just shorter.
public record PartyDisbandRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyDisbandRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "party_disband_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyDisbandRequestC2S> CODEC = PacketCodec.unit(new PartyDisbandRequestC2S());

	@Override
	public CustomPayload.Id<PartyDisbandRequestC2S> getId() {
		return ID;
	}
}
