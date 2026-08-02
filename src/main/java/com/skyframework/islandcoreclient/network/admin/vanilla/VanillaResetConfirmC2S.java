package com.skyframework.islandcoreclient.network.admin.vanilla;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.vanilla.VanillaResetConfirmC2S exactly.
public record VanillaResetConfirmC2S(String dimension) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetConfirmC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "vanilla_reset_confirm_c2s"));

	public static final PacketCodec<RegistryByteBuf, VanillaResetConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetConfirmC2S::dimension,
			VanillaResetConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetConfirmC2S> getId() {
		return ID;
	}
}
