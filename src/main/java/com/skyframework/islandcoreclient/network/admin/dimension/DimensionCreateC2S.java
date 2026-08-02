package com.skyframework.islandcoreclient.network.admin.dimension;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server's net.admin.dimension.DimensionCreateC2S exactly. id is the path only (see
// DimensionDetailRequestC2S); style is DimensionGeneratorStyle's name; seed absent means "random".
public record DimensionCreateC2S(String id, String displayName, String style, Optional<Long> seed) implements CustomPayload {

	public static final CustomPayload.Id<DimensionCreateC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_create_c2s"));

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, DimensionCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionCreateC2S::id,
			PacketCodecs.STRING, DimensionCreateC2S::displayName,
			PacketCodecs.STRING, DimensionCreateC2S::style,
			SEED_CODEC, DimensionCreateC2S::seed,
			DimensionCreateC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionCreateC2S> getId() {
		return ID;
	}
}
