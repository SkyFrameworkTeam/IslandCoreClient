package com.skyframework.islandcoreclient.network.admin.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Mirrors the server's net.admin.island.AdminIslandListS2C exactly: same 3 fields (islands,
// totalPages, currentPage) and same nested IslandEntry (9 fields, past PacketCodec.tuple's
// 6-argument limit, hand-written with PacketCodec.of like IslandSnapshotS2C).
public record AdminIslandListS2C(List<IslandEntry> islands, int totalPages, int currentPage) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandListS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_list_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<IslandEntry>> ISLAND_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, IslandEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminIslandListS2C> CODEC = PacketCodec.tuple(
			ISLAND_LIST_CODEC, AdminIslandListS2C::islands,
			PacketCodecs.VAR_INT, AdminIslandListS2C::totalPages,
			PacketCodecs.VAR_INT, AdminIslandListS2C::currentPage,
			AdminIslandListS2C::new
	);

	@Override
	public CustomPayload.Id<AdminIslandListS2C> getId() {
		return ID;
	}

	// Wire field order: ownerUuid, ownerName, size, maxSize, type, currentBiomeId (a LIVE lookup of
	// the biome at the island's center, not any locally-tracked value), state, memberCount,
	// isSpawnIsland (NEW — true when ownerUuid is the server's synthetic Island.SERVER_OWNER_UUID;
	// see AdminIslandListScreen for how this replaces ownerName in the row label).
	public record IslandEntry(
			UUID ownerUuid,
			String ownerName,
			int size,
			int maxSize,
			String type,
			String currentBiomeId,
			String state,
			int memberCount,
			boolean isSpawnIsland
	) {
		public static final PacketCodec<RegistryByteBuf, IslandEntry> CODEC = PacketCodec.of(
				(value, buf) -> {
					Uuids.PACKET_CODEC.encode(buf, value.ownerUuid());
					PacketCodecs.STRING.encode(buf, value.ownerName());
					PacketCodecs.VAR_INT.encode(buf, value.size());
					PacketCodecs.VAR_INT.encode(buf, value.maxSize());
					PacketCodecs.STRING.encode(buf, value.type());
					PacketCodecs.STRING.encode(buf, value.currentBiomeId());
					PacketCodecs.STRING.encode(buf, value.state());
					PacketCodecs.VAR_INT.encode(buf, value.memberCount());
					PacketCodecs.BOOL.encode(buf, value.isSpawnIsland());
				},
				buf -> new IslandEntry(
						Uuids.PACKET_CODEC.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.BOOL.decode(buf)
				)
		);
	}
}
