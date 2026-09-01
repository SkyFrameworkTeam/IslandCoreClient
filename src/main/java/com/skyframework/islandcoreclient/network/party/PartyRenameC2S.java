package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly.
public record PartyRenameC2S(String newName) implements CustomPayload {
	public static final CustomPayload.Id<PartyRenameC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_rename_c2s"));

	public static final PacketCodec<RegistryByteBuf, PartyRenameC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyRenameC2S::newName,
			PartyRenameC2S::new
	);

	@Override
	public CustomPayload.Id<PartyRenameC2S> getId() {
		return ID;
	}
}
