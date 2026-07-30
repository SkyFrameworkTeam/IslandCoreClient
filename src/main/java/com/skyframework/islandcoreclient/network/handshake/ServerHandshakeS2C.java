package com.skyframework.islandcoreclient.network.handshake;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ServerHandshakeS2C(int protocolVersion, boolean isOperator) implements CustomPayload {
	public static final CustomPayload.Id<ServerHandshakeS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "handshake_s2c"));

	public static final PacketCodec<RegistryByteBuf, ServerHandshakeS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ServerHandshakeS2C::protocolVersion,
			PacketCodecs.BOOL, ServerHandshakeS2C::isOperator,
			ServerHandshakeS2C::new
	);

	@Override
	public CustomPayload.Id<ServerHandshakeS2C> getId() {
		return ID;
	}
}
