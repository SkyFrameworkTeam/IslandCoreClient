package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record MemberInviteDeclineC2S() implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteDeclineC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_invite_decline_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberInviteDeclineC2S> CODEC =
			PacketCodec.unit(new MemberInviteDeclineC2S());

	@Override
	public CustomPayload.Id<MemberInviteDeclineC2S> getId() {
		return ID;
	}
}
