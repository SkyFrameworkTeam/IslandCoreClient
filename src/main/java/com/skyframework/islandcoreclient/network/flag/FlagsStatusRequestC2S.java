package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record FlagsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<FlagsStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "flags_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, FlagsStatusRequestC2S> CODEC =
			PacketCodec.unit(new FlagsStatusRequestC2S());

	@Override
	public CustomPayload.Id<FlagsStatusRequestC2S> getId() {
		return ID;
	}
}
