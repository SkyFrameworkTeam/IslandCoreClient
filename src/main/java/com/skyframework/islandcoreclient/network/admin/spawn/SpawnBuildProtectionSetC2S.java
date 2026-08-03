package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnBuildProtectionSetC2S exactly: enabled (boolean).
public record SpawnBuildProtectionSetC2S(boolean enabled) implements CustomPayload {

	public static final CustomPayload.Id<SpawnBuildProtectionSetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_build_protection_set_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnBuildProtectionSetC2S::enabled,
			SpawnBuildProtectionSetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnBuildProtectionSetC2S> getId() {
		return ID;
	}
}
