package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionDeleteConfirmC2S exactly. id is the path only.
public record DimensionDeleteConfirmC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_delete_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, DimensionDeleteConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDeleteConfirmC2S::id,
			DimensionDeleteConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDeleteConfirmC2S> getId() {
		return ID;
	}
}
