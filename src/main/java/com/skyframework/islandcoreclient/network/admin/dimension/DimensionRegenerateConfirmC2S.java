package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionRegenerateConfirmC2S exactly. id is the path only.
public record DimensionRegenerateConfirmC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionRegenerateConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_regenerate_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, DimensionRegenerateConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionRegenerateConfirmC2S::id,
			DimensionRegenerateConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionRegenerateConfirmC2S> getId() {
		return ID;
	}
}
