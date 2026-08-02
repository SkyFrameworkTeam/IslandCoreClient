package com.skyframework.islandcoreclient.network.admin.vanilla;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server's net.admin.vanilla.VanillaResetQueueC2S exactly: dimension
// ("overworld"/"nether"/"end"), seedMode (the server ONLY understands "CUSTOM" or "DEFAULT" here —
// NOT the 3-way RANDOM/KEEP/SPECIFIED distinction ClientVanillaResetState.SeedMode has locally;
// "CUSTOM" maps to explicitSeed = seedValue, anything else (RANDOM or KEEP) maps to
// explicitSeed = null, deferring to the server's own VanillaResetConfig seed-mode default), and
// seedValue (only meaningful when seedMode == "CUSTOM").
public record VanillaResetQueueC2S(String dimension, String seedMode, Optional<Long> seedValue) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetQueueC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "vanilla_reset_queue_c2s"));

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, VanillaResetQueueC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetQueueC2S::dimension,
			PacketCodecs.STRING, VanillaResetQueueC2S::seedMode,
			SEED_CODEC, VanillaResetQueueC2S::seedValue,
			VanillaResetQueueC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetQueueC2S> getId() {
		return ID;
	}
}
