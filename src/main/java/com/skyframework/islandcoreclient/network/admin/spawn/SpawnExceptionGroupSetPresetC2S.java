package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Network equivalent of "/island admin spawn exceptions preset".
public record SpawnExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<SpawnExceptionGroupSetPresetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_exception_group_set_preset_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnExceptionGroupSetPresetC2S::groupId,
			PacketCodecs.STRING, SpawnExceptionGroupSetPresetC2S::preset,
			SpawnExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnExceptionGroupSetPresetC2S> getId() {
		return ID;
	}
}
