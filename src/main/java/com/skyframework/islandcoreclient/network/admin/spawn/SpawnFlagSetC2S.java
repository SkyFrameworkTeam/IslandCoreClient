package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Network equivalent of a normal island's own FlagSetC2S,
// targeting the Spawn island instead.
public record SpawnFlagSetC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagSetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_flag_set_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnFlagSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnFlagSetC2S::flagId,
			PacketCodecs.STRING, SpawnFlagSetC2S::value,
			SpawnFlagSetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagSetC2S> getId() {
		return ID;
	}
}
