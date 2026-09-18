package com.skyframework.islandcoreclient.network.alliance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Sets all four toggles at once — the full desired state, not a
// single flip.
//
// Wire field order: sendPositionToParty, receivePositionsFromParty, sendPositionToAllies,
// receivePositionsFromAllies.
public record LocationSharingSetC2S(
		boolean sendPositionToParty,
		boolean receivePositionsFromParty,
		boolean sendPositionToAllies,
		boolean receivePositionsFromAllies
) implements CustomPayload {
	public static final CustomPayload.Id<LocationSharingSetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "location_sharing_set_c2s"));

	public static final PacketCodec<RegistryByteBuf, LocationSharingSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, LocationSharingSetC2S::sendPositionToParty,
			PacketCodecs.BOOL, LocationSharingSetC2S::receivePositionsFromParty,
			PacketCodecs.BOOL, LocationSharingSetC2S::sendPositionToAllies,
			PacketCodecs.BOOL, LocationSharingSetC2S::receivePositionsFromAllies,
			LocationSharingSetC2S::new
	);

	@Override
	public CustomPayload.Id<LocationSharingSetC2S> getId() {
		return ID;
	}
}
