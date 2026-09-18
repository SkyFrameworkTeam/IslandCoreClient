package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Empty on purpose, like SpawnFlagsStatusRequestC2S.
public record SpawnExceptionGroupsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnExceptionGroupsStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_exception_groups_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupsStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnExceptionGroupsStatusRequestC2S> getId() {
		return ID;
	}
}
