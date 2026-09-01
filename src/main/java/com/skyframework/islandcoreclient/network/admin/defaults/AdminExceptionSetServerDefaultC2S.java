package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Sets the server-wide default preset for one exception group.
// Operator-only.
public record AdminExceptionSetServerDefaultC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<AdminExceptionSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_exception_set_server_default_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminExceptionSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminExceptionSetServerDefaultC2S::groupId,
			PacketCodecs.STRING, AdminExceptionSetServerDefaultC2S::preset,
			AdminExceptionSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminExceptionSetServerDefaultC2S> getId() {
		return ID;
	}
}
