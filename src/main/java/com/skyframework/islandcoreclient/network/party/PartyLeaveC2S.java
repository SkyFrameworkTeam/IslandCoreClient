package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record PartyLeaveC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyLeaveC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_leave_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyLeaveC2S> CODEC = PacketCodec.unit(new PartyLeaveC2S());

	@Override
	public CustomPayload.Id<PartyLeaveC2S> getId() {
		return ID;
	}
}
