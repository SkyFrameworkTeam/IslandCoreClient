package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnStatusRequestC2S exactly: empty, no fields.
public record SpawnStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnStatusRequestC2S> getId() {
		return ID;
	}
}
