package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. targetName instead of a UUID: an ally doesn't have to
// already be a member the client has a UUID for — same reasoning as MemberInviteC2S.
public record MemberAllyAddC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<MemberAllyAddC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_ally_add_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberAllyAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, MemberAllyAddC2S::targetName,
			MemberAllyAddC2S::new
	);

	@Override
	public CustomPayload.Id<MemberAllyAddC2S> getId() {
		return ID;
	}
}
