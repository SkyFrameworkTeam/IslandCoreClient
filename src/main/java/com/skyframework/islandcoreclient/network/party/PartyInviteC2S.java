package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. targetName: same offline-name-resolution reasoning as MemberInviteC2S.
public record PartyInviteC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<PartyInviteC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_invite_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyInviteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyInviteC2S::targetName,
			PartyInviteC2S::new
	);

	@Override
	public CustomPayload.Id<PartyInviteC2S> getId() {
		return ID;
	}
}
