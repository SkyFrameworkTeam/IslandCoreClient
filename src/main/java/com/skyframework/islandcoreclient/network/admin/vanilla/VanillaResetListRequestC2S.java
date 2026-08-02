package com.skyframework.islandcoreclient.network.admin.vanilla;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.vanilla.VanillaResetListRequestC2S exactly: empty, no fields.
public record VanillaResetListRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<VanillaResetListRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "vanilla_reset_list_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, VanillaResetListRequestC2S> CODEC =
			PacketCodec.unit(new VanillaResetListRequestC2S());

	@Override
	public CustomPayload.Id<VanillaResetListRequestC2S> getId() {
		return ID;
	}
}
