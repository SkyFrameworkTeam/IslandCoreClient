package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server record exactly. targetUuid: the client already has this from its own
// IslandSnapshotS2C member list.
public record MemberTrustC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberTrustC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_trust_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberTrustC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberTrustC2S::targetUuid,
			MemberTrustC2S::new
	);

	@Override
	public CustomPayload.Id<MemberTrustC2S> getId() {
		return ID;
	}
}
