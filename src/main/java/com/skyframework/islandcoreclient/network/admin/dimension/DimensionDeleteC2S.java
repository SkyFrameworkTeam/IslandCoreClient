package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionDeleteC2S exactly. id is the path only.
public record DimensionDeleteC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDeleteC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_delete_c2s"));

	public static final PacketCodec<RegistryByteBuf, DimensionDeleteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDeleteC2S::id,
			DimensionDeleteC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDeleteC2S> getId() {
		return ID;
	}
}
