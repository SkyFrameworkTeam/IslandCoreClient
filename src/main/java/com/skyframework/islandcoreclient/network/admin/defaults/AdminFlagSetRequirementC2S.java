package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. Sets (or, with an empty permissionNode, clears) the LuckPerms
// node required to change one flag. Operator-only; reaches both ROLE_BASED and ISLAND_GLOBAL flags.
public record AdminFlagSetRequirementC2S(String flagId, String permissionNode) implements CustomPayload {
	public static final CustomPayload.Id<AdminFlagSetRequirementC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_flag_set_requirement_c2s"));

	public static final PacketCodec<RegistryByteBuf, AdminFlagSetRequirementC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminFlagSetRequirementC2S::flagId,
			PacketCodecs.STRING, AdminFlagSetRequirementC2S::permissionNode,
			AdminFlagSetRequirementC2S::new
	);

	@Override
	public CustomPayload.Id<AdminFlagSetRequirementC2S> getId() {
		return ID;
	}
}
