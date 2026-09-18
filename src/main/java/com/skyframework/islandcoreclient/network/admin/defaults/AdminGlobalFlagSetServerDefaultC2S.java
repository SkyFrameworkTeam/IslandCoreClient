package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Sets the server-wide default TriState override for one
// ISLAND_GLOBAL flag. Operator-only; ROLE_BASED flags aren't reachable through this payload — see
// AdminFlagSetServerDefaultC2S for those.
public record AdminGlobalFlagSetServerDefaultC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<AdminGlobalFlagSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_global_flag_set_server_default_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminGlobalFlagSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminGlobalFlagSetServerDefaultC2S::flagId,
			PacketCodecs.STRING, AdminGlobalFlagSetServerDefaultC2S::value,
			AdminGlobalFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminGlobalFlagSetServerDefaultC2S> getId() {
		return ID;
	}
}
