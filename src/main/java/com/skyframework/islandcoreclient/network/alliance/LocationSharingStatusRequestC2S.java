package com.skyframework.islandcoreclient.network.alliance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Empty on purpose, mirrors the server record exactly.
public record LocationSharingStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<LocationSharingStatusRequestC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "location_sharing_status_request_c2s"));

	public static final PacketCodec<RegistryByteBuf, LocationSharingStatusRequestC2S> CODEC =
			PacketCodec.unit(new LocationSharingStatusRequestC2S());

	@Override
	public CustomPayload.Id<LocationSharingStatusRequestC2S> getId() {
		return ID;
	}
}
