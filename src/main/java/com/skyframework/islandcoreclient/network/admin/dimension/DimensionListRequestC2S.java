package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionListRequestC2S exactly: empty, no fields, no
// pagination (matches "/dimension list" itself, which doesn't paginate either).
public record DimensionListRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<DimensionListRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_list_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, DimensionListRequestC2S> CODEC =
			PacketCodec.unit(new DimensionListRequestC2S());

	@Override
	public CustomPayload.Id<DimensionListRequestC2S> getId() {
		return ID;
	}
}
