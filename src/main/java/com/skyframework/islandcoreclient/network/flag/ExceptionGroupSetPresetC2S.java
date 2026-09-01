package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Replaces the old boolean-shaped ExceptionGroupSetC2S —
// exception groups now resolve per role, exact mirror of FlagSetPresetC2S. preset:
// "nadie"/"miembros"/"aliados"/"todos". Only valid for owner-configurable groups.
public record ExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<ExceptionGroupSetPresetC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "exception_group_set_preset_c2s"));

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, ExceptionGroupSetPresetC2S::groupId,
			PacketCodecs.STRING, ExceptionGroupSetPresetC2S::preset,
			ExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<ExceptionGroupSetPresetC2S> getId() {
		return ID;
	}
}
