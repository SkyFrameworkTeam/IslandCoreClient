package com.skyframework.islandcoreclient.network.admin.spawn;

import com.skyframework.islandcoreclient.network.flag.FlagsStatusS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server record exactly. Same shape/entries as a normal island's own FlagsStatusS2C —
// a separate payload id so the client routes it to ClientSpawnFlagsCache instead of
// ClientIslandCache's own flags.
public record SpawnFlagsStatusS2C(List<FlagsStatusS2C.FlagEntry> flags) implements CustomPayload {

	public static final CustomPayload.Id<SpawnFlagsStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_flags_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<FlagsStatusS2C.FlagEntry>> FLAG_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagsStatusS2C.FlagEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnFlagsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_LIST_CODEC, SpawnFlagsStatusS2C::flags,
			SpawnFlagsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagsStatusS2C> getId() {
		return ID;
	}
}
