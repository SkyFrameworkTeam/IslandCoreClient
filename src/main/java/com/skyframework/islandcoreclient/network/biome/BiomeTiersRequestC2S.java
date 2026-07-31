package com.skyframework.islandcoreclient.network.biome;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record BiomeTiersRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<BiomeTiersRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "biome_tiers_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, BiomeTiersRequestC2S> CODEC =
			PacketCodec.unit(new BiomeTiersRequestC2S());

	@Override
	public CustomPayload.Id<BiomeTiersRequestC2S> getId() {
		return ID;
	}
}
