package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. value: "allow"/"deny"/"default".
public record FlagSetC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<FlagSetC2S> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "flag_set_c2s"));

	public static final PacketCodec<RegistryByteBuf, FlagSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, FlagSetC2S::flagId,
			PacketCodecs.STRING, FlagSetC2S::value,
			FlagSetC2S::new
	);

	@Override
	public CustomPayload.Id<FlagSetC2S> getId() {
		return ID;
	}
}
