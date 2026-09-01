package com.skyframework.islandcoreclient.network.member;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server record exactly. targetUuid: the client already has this from its own
// IslandSnapshotS2C member list (an existing ALLY entry).
public record MemberAllyRemoveC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberAllyRemoveC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "member_ally_remove_c2s"));

	public static final PacketCodec<RegistryByteBuf, MemberAllyRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberAllyRemoveC2S::targetUuid,
			MemberAllyRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<MemberAllyRemoveC2S> getId() {
		return ID;
	}
}
