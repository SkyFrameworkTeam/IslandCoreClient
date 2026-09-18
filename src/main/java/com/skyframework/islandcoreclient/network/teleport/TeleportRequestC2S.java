package com.skyframework.islandcoreclient.network.teleport;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server record exactly, including the nested Type enum (HOME/SPAWN/RTP/DIMENSION,
// same names as ClientTeleportType so the two can convert 1:1) and the STRING-based enum codec
// (Type::valueOf / Enum::name).
//
// dimensionId is only ever present (and only ever read server-side) when type == DIMENSION: the
// fixed FARMING type from the old single hardcoded farming button was removed (Sprint
// "teletransportes dinámicos") in favor of this generic one — send back the same Identifier string
// TeleportStatusS2C.DimensionTeleportEntry#id gave for whichever dynamic-section button was
// clicked, farming's own entry included.
public record TeleportRequestC2S(Type type, Optional<String> dimensionId) implements CustomPayload {

	public enum Type {
		HOME, SPAWN, RTP, DIMENSION
	}

	public static final CustomPayload.Id<TeleportRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "teleport_request_c2s"));

	private static final PacketCodec<ByteBuf, Type> TYPE_CODEC = PacketCodecs.STRING.xmap(Type::valueOf, Enum::name);
	private static final PacketCodec<ByteBuf, Optional<String>> DIMENSION_ID_CODEC = PacketCodecs.optional(PacketCodecs.STRING);

	public static final PacketCodec<RegistryByteBuf, TeleportRequestC2S> CODEC = PacketCodec.tuple(
			TYPE_CODEC, TeleportRequestC2S::type,
			DIMENSION_ID_CODEC, TeleportRequestC2S::dimensionId,
			TeleportRequestC2S::new
	);

	@Override
	public CustomPayload.Id<TeleportRequestC2S> getId() {
		return ID;
	}

	// Convenience factories, so call sites don't spell out Optional.empty()/Optional.of() by hand.
	public static TeleportRequestC2S fixed(Type type) {
		return new TeleportRequestC2S(type, Optional.empty());
	}

	public static TeleportRequestC2S dimension(String dimensionId) {
		return new TeleportRequestC2S(Type.DIMENSION, Optional.of(dimensionId));
	}
}
