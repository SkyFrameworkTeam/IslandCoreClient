package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionDetailRequestC2S exactly. id is the
// dimension's PATH ONLY (e.g. "foo" for "islandcore:foo") — the server builds the full Identifier
// itself. Note this differs from DimensionListS2C.DimensionEntry#id, which carries the FULL
// identifier string; callers must strip the "islandcore:" namespace before sending this request.
public record DimensionDetailRequestC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDetailRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_detail_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, DimensionDetailRequestC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDetailRequestC2S::id,
			DimensionDetailRequestC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDetailRequestC2S> getId() {
		return ID;
	}
}
