package com.skyframework.islandcoreclient.network.admin.vanilla;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Mirrors the server's net.admin.vanilla.VanillaResetListS2C exactly.
public record VanillaResetListS2C(List<QueueEntry> queue) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetListS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "vanilla_reset_list_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<QueueEntry>> QUEUE_CODEC =
			PacketCodecs.collection(ArrayList::new, QueueEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, VanillaResetListS2C> CODEC = PacketCodec.tuple(
			QUEUE_CODEC, VanillaResetListS2C::queue,
			VanillaResetListS2C::new
	);

	@Override
	public CustomPayload.Id<VanillaResetListS2C> getId() {
		return ID;
	}

	// Wire field order: dimensionKey ("overworld"/"nether"/"end"), seed (empty means "keeps the
	// dimension's current seed"), seedMode (PendingVanillaReset.SeedMode name —
	// "RANDOM"/"KEEP"/"CUSTOM", persisted verbatim server-side from how the seed was actually
	// decided at confirm time, since a resolved RANDOM seed and a CUSTOM one are otherwise
	// indistinguishable once resolved — do NOT re-derive this from whether seed is present),
	// requestedBy, status (PendingVanillaReset.Status name — always "IN_PROGRESS" today, since
	// that's the only status ever persisted to the queue file).
	public record QueueEntry(String dimensionKey, Optional<Long> seed, String seedMode, UUID requestedBy, String status) {
		private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

		public static final PacketCodec<RegistryByteBuf, QueueEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, QueueEntry::dimensionKey,
				SEED_CODEC, QueueEntry::seed,
				PacketCodecs.STRING, QueueEntry::seedMode,
				Uuids.PACKET_CODEC, QueueEntry::requestedBy,
				PacketCodecs.STRING, QueueEntry::status,
				QueueEntry::new
		);
	}
}
