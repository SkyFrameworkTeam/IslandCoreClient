package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server's net.admin.dimension.DimensionListS2C exactly.
public record DimensionListS2C(List<DimensionEntry> dimensions) implements CustomPayload {

	public static final CustomPayload.Id<DimensionListS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_list_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<DimensionEntry>> DIMENSION_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, DimensionEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, DimensionListS2C> CODEC = PacketCodec.tuple(
			DIMENSION_LIST_CODEC, DimensionListS2C::dimensions,
			DimensionListS2C::new
	);

	@Override
	public CustomPayload.Id<DimensionListS2C> getId() {
		return ID;
	}

	// Wire field order: id (full Identifier as a String, e.g. "islandcore:foo"), displayName,
	// style (DimensionGeneratorStyle name), seed, state (DimensionState name).
	public record DimensionEntry(String id, String displayName, String style, long seed, String state) {
		public static final PacketCodec<RegistryByteBuf, DimensionEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, DimensionEntry::id,
				PacketCodecs.STRING, DimensionEntry::displayName,
				PacketCodecs.STRING, DimensionEntry::style,
				PacketCodecs.VAR_LONG, DimensionEntry::seed,
				PacketCodecs.STRING, DimensionEntry::state,
				DimensionEntry::new
		);
	}
}
