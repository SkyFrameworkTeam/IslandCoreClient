package com.skyframework.islandcoreclient.network.admin.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server's net.admin.island.AdminIslandDetailRequestC2S exactly. targetUuid is the
// island OWNER's uuid, not the islandId.
public record AdminIslandDetailRequestC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDetailRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_detail_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminIslandDetailRequestC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDetailRequestC2S::targetUuid,
			AdminIslandDetailRequestC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDetailRequestC2S> getId() {
		return ID;
	}
}
