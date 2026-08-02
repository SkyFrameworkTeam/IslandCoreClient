package com.skyframework.islandcoreclient.network.admin.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server's net.admin.island.AdminIslandDeleteConfirmC2S exactly.
public record AdminIslandDeleteConfirmC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_delete_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminIslandDeleteConfirmC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDeleteConfirmC2S::targetUuid,
			AdminIslandDeleteConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDeleteConfirmC2S> getId() {
		return ID;
	}
}
