package com.skyframework.islandcoreclient.network.admin.dimension;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.dimension.DimensionDetailS2C exactly: 7 fields (id, displayName,
// style, seed, state, createdAt, updatedAt), past PacketCodec.tuple's 6-argument limit so
// hand-written with PacketCodec.of. Sent only on success — not found replies with
// ActionResultS2C.fail(DIMENSION_NOT_FOUND) instead.
public record DimensionDetailS2C(
		String id,
		String displayName,
		String style,
		long seed,
		String state,
		String createdAt,
		String updatedAt
) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDetailS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "dimension_detail_s2c"));

	public static final PacketCodec<RegistryByteBuf, DimensionDetailS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.STRING.encode(buf, value.id());
				PacketCodecs.STRING.encode(buf, value.displayName());
				PacketCodecs.STRING.encode(buf, value.style());
				PacketCodecs.VAR_LONG.encode(buf, value.seed());
				PacketCodecs.STRING.encode(buf, value.state());
				PacketCodecs.STRING.encode(buf, value.createdAt());
				PacketCodecs.STRING.encode(buf, value.updatedAt());
			},
			buf -> new DimensionDetailS2C(
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.VAR_LONG.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<DimensionDetailS2C> getId() {
		return ID;
	}
}
