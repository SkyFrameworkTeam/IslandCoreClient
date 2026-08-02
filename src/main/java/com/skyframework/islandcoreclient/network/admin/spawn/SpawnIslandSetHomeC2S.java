package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnIslandSetHomeC2S exactly: empty, no coordinates. The
// server reads the ACTUAL sender's position itself — the client must NEVER send a BlockPos here.
public record SpawnIslandSetHomeC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnIslandSetHomeC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_island_set_home_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnIslandSetHomeC2S> CODEC =
			PacketCodec.unit(new SpawnIslandSetHomeC2S());

	@Override
	public CustomPayload.Id<SpawnIslandSetHomeC2S> getId() {
		return ID;
	}
}
