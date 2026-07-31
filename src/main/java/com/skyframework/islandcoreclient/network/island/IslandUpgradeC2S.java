package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record IslandUpgradeC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandUpgradeC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_upgrade_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandUpgradeC2S> CODEC = PacketCodec.unit(new IslandUpgradeC2S());

	@Override
	public CustomPayload.Id<IslandUpgradeC2S> getId() {
		return ID;
	}
}
