package com.skyframework.islandcoreclient.network.handshake;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientHandshakeC2S(int protocolVersion) implements CustomPayload {
	// Bumped to 6 alongside the "teletransportes dinámicos" sprint (TeleportStatusS2C's farming
	// field became a dimensions list, TeleportRequestC2S's FARMING type was replaced by DIMENSION
	// with a new dimensionId field) — must match the server's NetworkChannels.PROTOCOL_VERSION.
	public static final int CURRENT_PROTOCOL_VERSION = 6;

	// Namespaced under "islandcore", not "islandcoreclient": the server mod owns this protocol.
	public static final CustomPayload.Id<ClientHandshakeC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "handshake_c2s"));

	public static final PacketCodec<RegistryByteBuf, ClientHandshakeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ClientHandshakeC2S::protocolVersion,
			ClientHandshakeC2S::new
	);

	@Override
	public CustomPayload.Id<ClientHandshakeC2S> getId() {
		return ID;
	}
}
