package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnBuildProtectionStatusRequestC2S exactly: empty, no
// fields.
public record SpawnBuildProtectionStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnBuildProtectionStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_build_protection_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnBuildProtectionStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnBuildProtectionStatusRequestC2S> getId() {
		return ID;
	}
}
