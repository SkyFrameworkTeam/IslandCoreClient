package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server record exactly. Covers both /island untrust and /island kick server-side —
// the server decides which applies from the target's current role, so the client only needs one
// "remove this member" button regardless of role.
public record MemberRemoveC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberRemoveC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_remove_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberRemoveC2S::targetUuid,
			MemberRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<MemberRemoveC2S> getId() {
		return ID;
	}
}
