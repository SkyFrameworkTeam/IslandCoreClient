package com.skyframework.islandcoreclient.network.admin.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server's net.admin.island.AdminIslandDeleteC2S exactly.
public record AdminIslandDeleteC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDeleteC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_delete_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminIslandDeleteC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDeleteC2S::targetUuid,
			AdminIslandDeleteC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDeleteC2S> getId() {
		return ID;
	}
}
