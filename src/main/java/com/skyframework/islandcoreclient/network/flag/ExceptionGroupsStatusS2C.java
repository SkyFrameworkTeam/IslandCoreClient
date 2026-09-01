package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server's net.flag.ExceptionGroupsStatusS2C exactly. Only sent when the requesting
// player has an island — if not, the server sends ActionResultS2C.fail("no_island") instead.
// Exception groups now resolve per role exactly like ROLE_BASED flags (see the server's
// ExceptionResolver), so GroupEntry mirrors FlagsStatusS2C.FlagEntry's shape instead of the old
// single "enabled" boolean.
public record ExceptionGroupsStatusS2C(List<GroupEntry> groups) implements CustomPayload {

	public static final CustomPayload.Id<ExceptionGroupsStatusS2C> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "exception_groups_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<GroupEntry>> GROUP_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, GroupEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupsStatusS2C> CODEC = PacketCodec.tuple(
			GROUP_LIST_CODEC, ExceptionGroupsStatusS2C::groups,
			ExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<ExceptionGroupsStatusS2C> getId() {
		return ID;
	}

	// category: "BLOCK" or "ENTITY". resolvedByRole: one entry per IslandRole, reusing
	// FlagsStatusS2C.RoleValueEntry exactly (same shape, no reason to duplicate it). currentPreset:
	// "nadie"/"miembros"/"aliados"/"todos" if the current VISITOR/ALLY/MEMBER/TRUSTED combination
	// exactly matches one of those presets, or "custom" if not.
	public record GroupEntry(
			String groupId, String category, List<FlagsStatusS2C.RoleValueEntry> resolvedByRole, String currentPreset, boolean ownerConfigurable
	) {
		private static final PacketCodec<RegistryByteBuf, List<FlagsStatusS2C.RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				PacketCodecs.collection(ArrayList::new, FlagsStatusS2C.RoleValueEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, GroupEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, GroupEntry::groupId,
				PacketCodecs.STRING, GroupEntry::category,
				ROLE_VALUE_LIST_CODEC, GroupEntry::resolvedByRole,
				PacketCodecs.STRING, GroupEntry::currentPreset,
				PacketCodecs.BOOL, GroupEntry::ownerConfigurable,
				GroupEntry::new
		);
	}
}
