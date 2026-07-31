package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record IslandDeleteRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandDeleteRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_delete_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandDeleteRequestC2S> CODEC =
			PacketCodec.unit(new IslandDeleteRequestC2S());

	@Override
	public CustomPayload.Id<IslandDeleteRequestC2S> getId() {
		return ID;
	}
}
