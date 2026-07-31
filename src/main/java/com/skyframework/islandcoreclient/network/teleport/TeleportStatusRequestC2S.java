package com.skyframework.islandcoreclient.network.teleport;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record TeleportStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<TeleportStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "teleport_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, TeleportStatusRequestC2S> CODEC =
			PacketCodec.unit(new TeleportStatusRequestC2S());

	@Override
	public CustomPayload.Id<TeleportStatusRequestC2S> getId() {
		return ID;
	}
}
