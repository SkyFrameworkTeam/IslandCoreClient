package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Empty on purpose — requests the current server-wide defaults
// (not any specific island's), see AdminDefaultsStatusS2C. Operator-only.
public record AdminDefaultsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<AdminDefaultsStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_defaults_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminDefaultsStatusRequestC2S> CODEC =
			PacketCodec.unit(new AdminDefaultsStatusRequestC2S());

	@Override
	public CustomPayload.Id<AdminDefaultsStatusRequestC2S> getId() {
		return ID;
	}
}
