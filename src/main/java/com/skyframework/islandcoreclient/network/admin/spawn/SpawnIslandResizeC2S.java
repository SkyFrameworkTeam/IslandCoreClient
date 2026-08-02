package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnIslandResizeC2S exactly.
public record SpawnIslandResizeC2S(int newSize) implements CustomPayload {

	public static final CustomPayload.Id<SpawnIslandResizeC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_island_resize_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnIslandResizeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpawnIslandResizeC2S::newSize,
			SpawnIslandResizeC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnIslandResizeC2S> getId() {
		return ID;
	}
}
