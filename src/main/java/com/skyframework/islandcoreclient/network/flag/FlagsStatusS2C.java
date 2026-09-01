package com.skyframework.islandcoreclient.network.flag;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server's net.flag.FlagsStatusS2C exactly. Only sent when the requesting player has
// an island — if not, the server sends ActionResultS2C.fail("no_island") instead. flags is in
// FlagRegistry.all()'s registration order (build, break, interact, containers, entities, redstone,
// fire_spread, pvp_damage, mob_damage).
public record FlagsStatusS2C(List<FlagEntry> flags) implements CustomPayload {

	public static final CustomPayload.Id<FlagsStatusS2C> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "flags_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<FlagEntry>> FLAG_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, FlagsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_LIST_CODEC, FlagsStatusS2C::flags,
			FlagsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<FlagsStatusS2C> getId() {
		return ID;
	}

	// category: "ROLE_BASED" or "ISLAND_GLOBAL". resolvedValue: ISLAND_GLOBAL only, meaningless
	// (false) for a ROLE_BASED entry — read resolvedByRole instead. resolvedByRole: ROLE_BASED only,
	// one entry per IslandRole, empty for an ISLAND_GLOBAL entry. islandOverride: this island's own
	// override ("ALLOW"/"DENY"/"DEFAULT") — a single value even for ROLE_BASED, since "/island flags
	// set" (unlike the preset) always overrides every role uniformly. currentPreset: ROLE_BASED only
	// (added after islandOverride) — "nadie"/"miembros"/"aliados"/"todos" if the current
	// VISITOR/ALLY/MEMBER/TRUSTED combination exactly matches one of those presets, or "custom" if
	// not; always "" for an ISLAND_GLOBAL entry.
	public record FlagEntry(
			String flagId, String category, boolean resolvedValue, List<RoleValueEntry> resolvedByRole, String islandOverride, String currentPreset
	) {
		private static final PacketCodec<RegistryByteBuf, List<RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				PacketCodecs.collection(ArrayList::new, RoleValueEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, FlagEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, FlagEntry::flagId,
				PacketCodecs.STRING, FlagEntry::category,
				PacketCodecs.BOOL, FlagEntry::resolvedValue,
				ROLE_VALUE_LIST_CODEC, FlagEntry::resolvedByRole,
				PacketCodecs.STRING, FlagEntry::islandOverride,
				PacketCodecs.STRING, FlagEntry::currentPreset,
				FlagEntry::new
		);
	}

	// value is always "ALLOW" or "DENY" — never "DEFAULT".
	public record RoleValueEntry(String role, String value) {
		public static final PacketCodec<RegistryByteBuf, RoleValueEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, RoleValueEntry::role,
				PacketCodecs.STRING, RoleValueEntry::value,
				RoleValueEntry::new
		);
	}
}
