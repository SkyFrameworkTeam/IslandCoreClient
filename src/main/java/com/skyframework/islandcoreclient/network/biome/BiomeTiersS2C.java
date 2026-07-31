package com.skyframework.islandcoreclient.network.biome;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Mirrors the server record exactly. permissionRequired is empty for a base tier. label
// (BiomeEntry) is a plain display string computed server-side from the biome's Identifier path
// (e.g. "minecraft:dark_forest" -> "Dark Forest") — NOT a translation key.
public record BiomeTiersS2C(List<TierEntry> tiers) implements CustomPayload {

	public static final CustomPayload.Id<BiomeTiersS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "biome_tiers_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<TierEntry>> TIERS_CODEC =
			PacketCodecs.collection(ArrayList::new, TierEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, BiomeTiersS2C> CODEC = PacketCodec.tuple(
			TIERS_CODEC, BiomeTiersS2C::tiers,
			BiomeTiersS2C::new
	);

	@Override
	public CustomPayload.Id<BiomeTiersS2C> getId() {
		return ID;
	}

	public record TierEntry(String tierId, Optional<String> permissionRequired, boolean unlocked, List<BiomeEntry> biomes) {

		private static final PacketCodec<ByteBuf, Optional<String>> PERMISSION_CODEC = PacketCodecs.optional(PacketCodecs.STRING);
		private static final PacketCodec<RegistryByteBuf, List<BiomeEntry>> BIOMES_CODEC =
				PacketCodecs.collection(ArrayList::new, BiomeEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, TierEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, TierEntry::tierId,
				PERMISSION_CODEC, TierEntry::permissionRequired,
				PacketCodecs.BOOL, TierEntry::unlocked,
				BIOMES_CODEC, TierEntry::biomes,
				TierEntry::new
		);
	}

	public record BiomeEntry(String biomeId, String label) {
		public static final PacketCodec<RegistryByteBuf, BiomeEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, BiomeEntry::biomeId,
				PacketCodecs.STRING, BiomeEntry::label,
				BiomeEntry::new
		);
	}
}
