package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record ExceptionGroupsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<ExceptionGroupsStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "exception_groups_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupsStatusRequestC2S> CODEC =
			PacketCodec.unit(new ExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPayload.Id<ExceptionGroupsStatusRequestC2S> getId() {
		return ID;
	}
}
