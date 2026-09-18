package com.skyframework.islandcoreclient.network.alliance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. The receiving player's own four location-sharing toggles: two
// independent pairs, party and allies, each with its own send/receive half.
//
// Wire field order: sendPositionToParty, receivePositionsFromParty, sendPositionToAllies,
// receivePositionsFromAllies.
public record LocationSharingStatusS2C(
		boolean sendPositionToParty,
		boolean receivePositionsFromParty,
		boolean sendPositionToAllies,
		boolean receivePositionsFromAllies
) implements CustomPayload {
	public static final CustomPayload.Id<LocationSharingStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "location_sharing_status_s2c"));

	public static final PacketCodec<RegistryByteBuf, LocationSharingStatusS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, LocationSharingStatusS2C::sendPositionToParty,
			PacketCodecs.BOOL, LocationSharingStatusS2C::receivePositionsFromParty,
			PacketCodecs.BOOL, LocationSharingStatusS2C::sendPositionToAllies,
			PacketCodecs.BOOL, LocationSharingStatusS2C::receivePositionsFromAllies,
			LocationSharingStatusS2C::new
	);

	@Override
	public CustomPayload.Id<LocationSharingStatusS2C> getId() {
		return ID;
	}
}
