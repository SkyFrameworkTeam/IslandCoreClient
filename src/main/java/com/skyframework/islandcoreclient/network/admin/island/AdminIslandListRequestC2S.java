package com.skyframework.islandcoreclient.network.admin.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.island.AdminIslandListRequestC2S exactly: same 3 fields in the
// same order. page is 0-indexed (page 0 is the first page); pageSize is clamped to at least 1
// server-side; searchQuery empty means "no filter" (matched against the resolved owner name).
public record AdminIslandListRequestC2S(int page, int pageSize, String searchQuery) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandListRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_list_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminIslandListRequestC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, AdminIslandListRequestC2S::page,
			PacketCodecs.VAR_INT, AdminIslandListRequestC2S::pageSize,
			PacketCodecs.STRING, AdminIslandListRequestC2S::searchQuery,
			AdminIslandListRequestC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandListRequestC2S> getId() {
		return ID;
	}
}
