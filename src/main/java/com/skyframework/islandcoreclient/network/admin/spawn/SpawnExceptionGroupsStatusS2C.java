package com.skyframework.islandcoreclient.network.admin.spawn;

import com.skyframework.islandcoreclient.network.flag.ExceptionGroupsStatusS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server record exactly. Same shape/entries as a normal island's own
// ExceptionGroupsStatusS2C — a separate payload id so the client routes it to
// ClientSpawnFlagsCache instead of ClientIslandCache's own exceptionGroups.
public record SpawnExceptionGroupsStatusS2C(List<ExceptionGroupsStatusS2C.GroupEntry> groups) implements CustomPayload {

	public static final CustomPayload.Id<SpawnExceptionGroupsStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_exception_groups_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<ExceptionGroupsStatusS2C.GroupEntry>> GROUP_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, ExceptionGroupsStatusS2C.GroupEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupsStatusS2C> CODEC = PacketCodec.tuple(
			GROUP_LIST_CODEC, SpawnExceptionGroupsStatusS2C::groups,
			SpawnExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnExceptionGroupsStatusS2C> getId() {
		return ID;
	}
}
