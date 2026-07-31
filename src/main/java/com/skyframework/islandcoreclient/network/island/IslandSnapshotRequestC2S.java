package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record IslandSnapshotRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandSnapshotRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_snapshot_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandSnapshotRequestC2S> CODEC =
			PacketCodec.unit(new IslandSnapshotRequestC2S());

	@Override
	public CustomPayload.Id<IslandSnapshotRequestC2S> getId() {
		return ID;
	}
}
