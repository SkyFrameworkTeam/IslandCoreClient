package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly: the server acts on the connection's own
// player, never on client-supplied data.
public record IslandCreateC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandCreateC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_create_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandCreateC2S> CODEC = PacketCodec.unit(new IslandCreateC2S());

	@Override
	public CustomPayload.Id<IslandCreateC2S> getId() {
		return ID;
	}
}
