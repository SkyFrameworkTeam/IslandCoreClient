package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record IslandDeleteConfirmC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_delete_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandDeleteConfirmC2S> CODEC =
			PacketCodec.unit(new IslandDeleteConfirmC2S());

	@Override
	public CustomPayload.Id<IslandDeleteConfirmC2S> getId() {
		return ID;
	}
}
