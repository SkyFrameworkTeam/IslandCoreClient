package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server record exactly. The server-wide default configuration for every ROLE_BASED
// flag, every exception group, and (Sprint "teletransportes dinámicos" Admin Permisos/General work)
// every ISLAND_GLOBAL flag — not any specific island.
//
// Wire format changed: globalDefaults (list of GlobalDefaultEntry) was added as a 3rd field — a
// client/server protocol break, see the server's AdminDefaultsStatusS2C class javadoc.
public record AdminDefaultsStatusS2C(
		List<FlagDefaultEntry> flagDefaults, List<ExceptionDefaultEntry> exceptionDefaults, List<GlobalDefaultEntry> globalDefaults
) implements CustomPayload {

	public static final CustomPayload.Id<AdminDefaultsStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_defaults_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<FlagDefaultEntry>> FLAG_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagDefaultEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<ExceptionDefaultEntry>> EXCEPTION_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, ExceptionDefaultEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<GlobalDefaultEntry>> GLOBAL_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, GlobalDefaultEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminDefaultsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::flagDefaults,
			EXCEPTION_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::exceptionDefaults,
			GLOBAL_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::globalDefaults,
			AdminDefaultsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<AdminDefaultsStatusS2C> getId() {
		return ID;
	}

	public record FlagDefaultEntry(String flagId, String currentPreset) {
		public static final PacketCodec<RegistryByteBuf, FlagDefaultEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, FlagDefaultEntry::flagId,
				PacketCodecs.STRING, FlagDefaultEntry::currentPreset,
				FlagDefaultEntry::new
		);
	}

	public record ExceptionDefaultEntry(String groupId, String currentPreset) {
		public static final PacketCodec<RegistryByteBuf, ExceptionDefaultEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, ExceptionDefaultEntry::groupId,
				PacketCodecs.STRING, ExceptionDefaultEntry::currentPreset,
				ExceptionDefaultEntry::new
		);
	}

	// currentValue: "allow"/"deny"/"default" — same ClientTriState vocabulary TriStateRow already
	// cycles through for an island's own ISLAND_GLOBAL override.
	public record GlobalDefaultEntry(String flagId, String currentValue) {
		public static final PacketCodec<RegistryByteBuf, GlobalDefaultEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, GlobalDefaultEntry::flagId,
				PacketCodecs.STRING, GlobalDefaultEntry::currentValue,
				GlobalDefaultEntry::new
		);
	}
}
