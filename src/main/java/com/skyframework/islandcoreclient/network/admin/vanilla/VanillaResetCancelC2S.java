package com.skyframework.islandcoreclient.network.admin.vanilla;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.vanilla.VanillaResetCancelC2S exactly. Cancels a QUEUED
// (already-confirmed) reset only.
public record VanillaResetCancelC2S(String dimension) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetCancelC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "vanilla_reset_cancel_c2s"));

	public static final PacketCodec<RegistryByteBuf, VanillaResetCancelC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetCancelC2S::dimension,
			VanillaResetCancelC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetCancelC2S> getId() {
		return ID;
	}
}
