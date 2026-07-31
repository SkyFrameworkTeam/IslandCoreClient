package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record MemberInviteAcceptC2S() implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteAcceptC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_invite_accept_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberInviteAcceptC2S> CODEC =
			PacketCodec.unit(new MemberInviteAcceptC2S());

	@Override
	public CustomPayload.Id<MemberInviteAcceptC2S> getId() {
		return ID;
	}
}
