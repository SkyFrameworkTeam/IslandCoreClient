package com.skyframework.islandcoreclient.network.admin.dimension;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server's net.admin.dimension.DimensionRegenerateC2S exactly. id is the path only;
// seed absent means "random", same as DimensionCreateC2S.
public record DimensionRegenerateC2S(String id, Optional<Long> seed) implements CustomPayload {

	public static final CustomPayload.Id<DimensionRegenerateC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_regenerate_c2s"));

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, DimensionRegenerateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionRegenerateC2S::id,
			SEED_CODEC, DimensionRegenerateC2S::seed,
			DimensionRegenerateC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionRegenerateC2S> getId() {
		return ID;
	}
}
