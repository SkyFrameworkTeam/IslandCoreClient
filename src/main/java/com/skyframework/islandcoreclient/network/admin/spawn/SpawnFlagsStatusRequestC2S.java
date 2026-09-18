package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Empty on purpose, like SpawnStatusRequestC2S.
public record SpawnFlagsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagsStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_flags_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnFlagsStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnFlagsStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnFlagsStatusRequestC2S> getId() {
		return ID;
	}
}
