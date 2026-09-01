package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Sets the server-wide default preset for one ROLE_BASED flag.
// Operator-only; ISLAND_GLOBAL flags aren't reachable through this payload.
public record AdminFlagSetServerDefaultC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<AdminFlagSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_flag_set_server_default_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminFlagSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminFlagSetServerDefaultC2S::flagId,
			PacketCodecs.STRING, AdminFlagSetServerDefaultC2S::preset,
			AdminFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminFlagSetServerDefaultC2S> getId() {
		return ID;
	}
}
