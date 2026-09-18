package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.admin.defaults.AdminDefaultsStatusS2C;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Server-wide default configuration for ROLE_BASED flags and exception groups — not tied to any
// specific island, same independence reasoning as ClientPartyCache. Populated exclusively from
// AdminDefaultsStatusS2C. Immutable view records (rather than ClientFlagView's mutable-field
// style) since the only mutation needed is "swap this one entry's preset", cheaply done by
// rebuilding the list.
public final class ClientAdminDefaultsCache {
	public record FlagDefaultView(String flagId, String currentPreset) {
	}

	public record ExceptionDefaultView(String groupId, String currentPreset) {
	}

	// currentValue is a ClientTriState (ALLOW/DENY/DEFAULT), not a preset — same distinction the
	// server's GlobalDefaultEntry makes, see its class doc.
	public record GlobalDefaultView(String flagId, ClientTriState currentValue) {
	}

	private static volatile List<FlagDefaultView> flagDefaults = List.of();
	private static volatile List<ExceptionDefaultView> exceptionDefaults = List.of();
	private static volatile List<GlobalDefaultView> globalDefaults = List.of();

	private ClientAdminDefaultsCache() {
	}

	public static void applyStatus(AdminDefaultsStatusS2C status) {
		List<FlagDefaultView> mappedFlags = new ArrayList<>();
		for (AdminDefaultsStatusS2C.FlagDefaultEntry entry : status.flagDefaults()) {
			mappedFlags.add(new FlagDefaultView(entry.flagId(), entry.currentPreset()));
		}
		flagDefaults = List.copyOf(mappedFlags);

		List<ExceptionDefaultView> mappedExceptions = new ArrayList<>();
		for (AdminDefaultsStatusS2C.ExceptionDefaultEntry entry : status.exceptionDefaults()) {
			mappedExceptions.add(new ExceptionDefaultView(entry.groupId(), entry.currentPreset()));
		}
		exceptionDefaults = List.copyOf(mappedExceptions);

		List<GlobalDefaultView> mappedGlobals = new ArrayList<>();
		for (AdminDefaultsStatusS2C.GlobalDefaultEntry entry : status.globalDefaults()) {
			mappedGlobals.add(new GlobalDefaultView(entry.flagId(), ClientTriState.fromWire(entry.currentValue().toUpperCase(Locale.ROOT))));
		}
		globalDefaults = List.copyOf(mappedGlobals);
	}

	public static List<FlagDefaultView> getFlagDefaults() {
		return flagDefaults;
	}

	public static List<ExceptionDefaultView> getExceptionDefaults() {
		return exceptionDefaults;
	}

	public static List<GlobalDefaultView> getGlobalDefaults() {
		return globalDefaults;
	}

	// Optimistic update for FlagPresetRow's immediate highlight — same "don't guess, always refetch
	// on success" reasoning as ClientIslandCache#updateFlagPreset.
	public static void updateFlagDefaultPreset(String flagId, String preset) {
		List<FlagDefaultView> current = flagDefaults;
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i).flagId().equals(flagId)) {
				List<FlagDefaultView> updated = new ArrayList<>(current);
				updated.set(i, new FlagDefaultView(flagId, preset));
				flagDefaults = List.copyOf(updated);
				return;
			}
		}
	}

	public static void updateExceptionDefaultPreset(String groupId, String preset) {
		List<ExceptionDefaultView> current = exceptionDefaults;
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i).groupId().equals(groupId)) {
				List<ExceptionDefaultView> updated = new ArrayList<>(current);
				updated.set(i, new ExceptionDefaultView(groupId, preset));
				exceptionDefaults = List.copyOf(updated);
				return;
			}
		}
	}

	public static void updateGlobalDefaultValue(String flagId, ClientTriState value) {
		List<GlobalDefaultView> current = globalDefaults;
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i).flagId().equals(flagId)) {
				List<GlobalDefaultView> updated = new ArrayList<>(current);
				updated.set(i, new GlobalDefaultView(flagId, value));
				globalDefaults = List.copyOf(updated);
				return;
			}
		}
	}
}
