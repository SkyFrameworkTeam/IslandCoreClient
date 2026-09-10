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
// FlagRegistry.all()'s registration order (construccion, interact, entities, redstone, fire_spread,
// pvp_damage, mob_damage, crop_trample, natural_mob_spawning, raids).
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
	// VISITOR/ALLY/MEMBER combination exactly matches one of those presets, or "custom" if
	// not; always "" for an ISLAND_GLOBAL entry. missingRequiredPermission: true only if the server's
	// FlagPermissionRequirements has a node set for this flag and the requesting player (the island
	// owner) lacks it — presentation only, SettingsScreen dims the row and explains why; the real
	// gate is server-side.
	public record FlagEntry(
			String flagId, String category, boolean resolvedValue, List<RoleValueEntry> resolvedByRole, String islandOverride, String currentPreset,
			boolean missingRequiredPermission
	) {
		private static final PacketCodec<RegistryByteBuf, List<RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				PacketCodecs.collection(ArrayList::new, RoleValueEntry.CODEC);

		// 7 fields is past PacketCodec#tuple's 6-argument limit, so this is hand-written with
		// PacketCodec#of instead — mirrors the server's net.flag.FlagsStatusS2C.FlagEntry#CODEC.
		public static final PacketCodec<RegistryByteBuf, FlagEntry> CODEC = PacketCodec.of(
				(value, buf) -> {
					PacketCodecs.STRING.encode(buf, value.flagId());
					PacketCodecs.STRING.encode(buf, value.category());
					PacketCodecs.BOOL.encode(buf, value.resolvedValue());
					ROLE_VALUE_LIST_CODEC.encode(buf, value.resolvedByRole());
					PacketCodecs.STRING.encode(buf, value.islandOverride());
					PacketCodecs.STRING.encode(buf, value.currentPreset());
					PacketCodecs.BOOL.encode(buf, value.missingRequiredPermission());
				},
				buf -> new FlagEntry(
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.BOOL.decode(buf),
						ROLE_VALUE_LIST_CODEC.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.BOOL.decode(buf)
				)
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
