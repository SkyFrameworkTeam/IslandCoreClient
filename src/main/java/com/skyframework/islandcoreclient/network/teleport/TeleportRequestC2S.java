package com.skyframework.islandcoreclient.network.teleport;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly, including the nested Type enum (HOME/SPAWN/RTP/FARMING,
// same names as ClientTeleportType so the two can convert 1:1) and the STRING-based enum codec
// (Type::valueOf / Enum::name).
public record TeleportRequestC2S(Type type) implements CustomPayload {

	public enum Type {
		HOME, SPAWN, RTP, FARMING
	}

	public static final CustomPayload.Id<TeleportRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "teleport_request_c2s"));

	private static final PacketCodec<ByteBuf, Type> TYPE_CODEC = PacketCodecs.STRING.xmap(Type::valueOf, Enum::name);

	public static final PacketCodec<RegistryByteBuf, TeleportRequestC2S> CODEC = PacketCodec.tuple(
			TYPE_CODEC, TeleportRequestC2S::type,
			TeleportRequestC2S::new
	);

	@Override
	public CustomPayload.Id<TeleportRequestC2S> getId() {
		return ID;
	}
}
