package com.skyframework.islandcoreclient.network.admin.defaults;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

// Mirrors the server record exactly. The server-wide default configuration for every ROLE_BASED
// flag and every exception group — not any specific island. Scoped to preset-shaped entries only:
// the 3 ISLAND_GLOBAL flags have no preset concept and stay command-only server-side (see the
// server's AdminDefaultsStatusS2C class javadoc), so DefaultConfigScreen never shows them.
public record AdminDefaultsStatusS2C(List<FlagDefaultEntry> flagDefaults, List<ExceptionDefaultEntry> exceptionDefaults) implements CustomPayload {

	public static final CustomPayload.Id<AdminDefaultsStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_defaults_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<FlagDefaultEntry>> FLAG_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagDefaultEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<ExceptionDefaultEntry>> EXCEPTION_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, ExceptionDefaultEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminDefaultsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::flagDefaults,
			EXCEPTION_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::exceptionDefaults,
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
}
