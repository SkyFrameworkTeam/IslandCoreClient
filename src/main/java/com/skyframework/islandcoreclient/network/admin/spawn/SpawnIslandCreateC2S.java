package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnIslandCreateC2S exactly.
public record SpawnIslandCreateC2S(int size) implements CustomPayload {

	public static final CustomPayload.Id<SpawnIslandCreateC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_island_create_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnIslandCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpawnIslandCreateC2S::size,
			SpawnIslandCreateC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnIslandCreateC2S> getId() {
		return ID;
	}
}
