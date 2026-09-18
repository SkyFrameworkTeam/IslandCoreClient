package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Network equivalent of a normal island's own FlagSetPresetC2S,
// targeting the Spawn island instead.
public record SpawnFlagSetPresetC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagSetPresetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_flag_set_preset_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnFlagSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnFlagSetPresetC2S::flagId,
			PacketCodecs.STRING, SpawnFlagSetPresetC2S::preset,
			SpawnFlagSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagSetPresetC2S> getId() {
		return ID;
	}
}
