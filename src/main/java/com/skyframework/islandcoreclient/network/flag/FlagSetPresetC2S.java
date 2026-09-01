package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. preset: "nadie"/"miembros"/"aliados"/"todos". Only valid for
// ROLE_BASED flags.
public record FlagSetPresetC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<FlagSetPresetC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "flag_set_preset_c2s"));

	public static final PacketCodec<RegistryByteBuf, FlagSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, FlagSetPresetC2S::flagId,
			PacketCodecs.STRING, FlagSetPresetC2S::preset,
			FlagSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<FlagSetPresetC2S> getId() {
		return ID;
	}
}
