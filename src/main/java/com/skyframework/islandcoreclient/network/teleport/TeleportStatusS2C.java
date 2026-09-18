package com.skyframework.islandcoreclient.network.teleport;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Mirrors the server record exactly. reasonKey is only ever present when enabled == false:
// SPAWN_DISABLED for the one remaining config-gated fixed destination, RTP_DISABLED/
// RTP_DIMENSION_NOT_ALLOWED for rtp. home never sets it — there's no server-wide toggle for
// /island home.
//
// Wire format changed (Sprint "teletransportes dinámicos"): the fixed 4th field `farming`
// (StatusEntry) was replaced by `dimensions` (List<DimensionTeleportEntry>) — one entry per
// DIMENSION_REGISTRY dimension instead of a single hardcoded farming slot. Final field order:
// home, spawn, rtp, dimensions.
public record TeleportStatusS2C(StatusEntry home, StatusEntry spawn, StatusEntry rtp, List<DimensionTeleportEntry> dimensions) implements CustomPayload {

	public static final CustomPayload.Id<TeleportStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "teleport_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<DimensionTeleportEntry>> DIMENSION_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, DimensionTeleportEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, TeleportStatusS2C> CODEC = PacketCodec.tuple(
			StatusEntry.CODEC, TeleportStatusS2C::home,
			StatusEntry.CODEC, TeleportStatusS2C::spawn,
			StatusEntry.CODEC, TeleportStatusS2C::rtp,
			DIMENSION_LIST_CODEC, TeleportStatusS2C::dimensions,
			TeleportStatusS2C::new
	);

	@Override
	public CustomPayload.Id<TeleportStatusS2C> getId() {
		return ID;
	}

	public record StatusEntry(boolean enabled, long cooldownRemainingSeconds, Optional<String> reasonKey) {

		private static final PacketCodec<ByteBuf, Optional<String>> REASON_CODEC = PacketCodecs.optional(PacketCodecs.STRING);

		public static final PacketCodec<RegistryByteBuf, StatusEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.BOOL, StatusEntry::enabled,
				PacketCodecs.VAR_LONG, StatusEntry::cooldownRemainingSeconds,
				REASON_CODEC, StatusEntry::reasonKey,
				StatusEntry::new
		);
	}

	// id is the dimension's Identifier.toString() (e.g. "islandcore:farming") — sent back verbatim
	// in TeleportRequestC2S's dimensionId field, so there's no id<->name resolution to keep in sync.
	public record DimensionTeleportEntry(String id, String displayName, boolean enabled, long cooldownRemainingSeconds) {

		public static final PacketCodec<RegistryByteBuf, DimensionTeleportEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, DimensionTeleportEntry::id,
				PacketCodecs.STRING, DimensionTeleportEntry::displayName,
				PacketCodecs.BOOL, DimensionTeleportEntry::enabled,
				PacketCodecs.VAR_LONG, DimensionTeleportEntry::cooldownRemainingSeconds,
				DimensionTeleportEntry::new
		);
	}
}
