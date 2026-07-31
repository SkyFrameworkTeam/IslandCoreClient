package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. biomeId is the raw Identifier string (e.g.
// "minecraft:jungle"), same as ClientBiomeView#biomeId().
public record IslandBiomeChangeC2S(String biomeId) implements CustomPayload {
	public static final CustomPayload.Id<IslandBiomeChangeC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_biome_change_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandBiomeChangeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, IslandBiomeChangeC2S::biomeId,
			IslandBiomeChangeC2S::new
	);

	@Override
	public CustomPayload.Id<IslandBiomeChangeC2S> getId() {
		return ID;
	}
}
