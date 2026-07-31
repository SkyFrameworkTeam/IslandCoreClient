package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. targetName instead of a UUID: the client has no reliable
// way to know an offline player's UUID up front; the server resolves it (online players first,
// then its offline profile cache).
public record MemberInviteC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_invite_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberInviteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, MemberInviteC2S::targetName,
			MemberInviteC2S::new
	);

	@Override
	public CustomPayload.Id<MemberInviteC2S> getId() {
		return ID;
	}
}
