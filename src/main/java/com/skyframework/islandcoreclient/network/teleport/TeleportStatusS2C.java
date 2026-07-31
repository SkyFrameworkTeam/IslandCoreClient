package com.skyframework.islandcoreclient.network.teleport;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server record exactly. reasonKey is only ever present when enabled == false:
// SPAWN_DISABLED/FARMING_DISABLED for the two config-gated destinations, RTP_DISABLED/
// RTP_DIMENSION_NOT_ALLOWED for rtp. home never sets it — there's no server-wide toggle for
// /island home.
public record TeleportStatusS2C(StatusEntry home, StatusEntry spawn, StatusEntry rtp, StatusEntry farming) implements CustomPayload {

	public static final CustomPayload.Id<TeleportStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "teleport_status_s2c"));

	public static final PacketCodec<RegistryByteBuf, TeleportStatusS2C> CODEC = PacketCodec.tuple(
			StatusEntry.CODEC, TeleportStatusS2C::home,
			StatusEntry.CODEC, TeleportStatusS2C::spawn,
			StatusEntry.CODEC, TeleportStatusS2C::rtp,
			StatusEntry.CODEC, TeleportStatusS2C::farming,
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
}
