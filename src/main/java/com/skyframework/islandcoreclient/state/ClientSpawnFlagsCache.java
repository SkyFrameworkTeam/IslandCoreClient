package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.admin.spawn.SpawnExceptionGroupsStatusS2C;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnFlagsStatusS2C;
import com.skyframework.islandcoreclient.network.flag.ExceptionGroupsStatusS2C;
import com.skyframework.islandcoreclient.network.flag.FlagsStatusS2C;

import java.util.ArrayList;
import java.util.List;

// Spawn's own Permisos/General state — a SEPARATE cache from ClientIslandCache's flags/
// exceptionGroups (the ACTING player's own island), populated from the Spawn-specific
// SpawnFlagsStatusS2C/SpawnExceptionGroupsStatusS2C rather than colliding with SettingsScreen's.
// Reuses ClientFlagView/ClientExceptionGroupView directly (same shape as a normal island's) — same
// mapping ClientIslandCache#applyFlagsStatus/applyExceptionGroupsStatus already does, just off the
// Spawn-specific payload's nested entries instead.
public final class ClientSpawnFlagsCache {
	private static volatile List<ClientFlagView> flags = List.of();
	private static volatile List<ClientExceptionGroupView> exceptionGroups = List.of();

	private ClientSpawnFlagsCache() {
	}

	public static void applyFlagsStatus(SpawnFlagsStatusS2C status) {
		List<ClientFlagView> mapped = new ArrayList<>();
		for (FlagsStatusS2C.FlagEntry entry : status.flags()) {
			ClientFlagView.Category category = ClientFlagView.Category.valueOf(entry.category());
			List<ClientFlagView.RoleValue> resolvedByRole = new ArrayList<>();
			for (FlagsStatusS2C.RoleValueEntry roleEntry : entry.resolvedByRole()) {
				resolvedByRole.add(new ClientFlagView.RoleValue(roleEntry.role(), "ALLOW".equals(roleEntry.value())));
			}
			mapped.add(new ClientFlagView(entry.flagId(), category, entry.resolvedValue(), resolvedByRole,
					ClientTriState.fromWire(entry.islandOverride()), entry.currentPreset(), entry.missingRequiredPermission()));
		}
		flags = List.copyOf(mapped);
	}

	public static List<ClientFlagView> getFlags() {
		return flags;
	}

	public static void updateFlagOverride(String flagId, ClientTriState newOverride) {
		for (ClientFlagView flag : flags) {
			if (flag.flagId().equals(flagId)) {
				flag.setIslandOverride(newOverride);
				return;
			}
		}
	}

	public static void updateFlagPreset(String flagId, String preset) {
		for (ClientFlagView flag : flags) {
			if (flag.flagId().equals(flagId)) {
				flag.setCurrentPreset(preset);
				return;
			}
		}
	}

	public static void applyExceptionGroupsStatus(SpawnExceptionGroupsStatusS2C status) {
		List<ClientExceptionGroupView> mapped = new ArrayList<>();
		for (ExceptionGroupsStatusS2C.GroupEntry entry : status.groups()) {
			List<ClientFlagView.RoleValue> resolvedByRole = new ArrayList<>();
			for (FlagsStatusS2C.RoleValueEntry roleEntry : entry.resolvedByRole()) {
				resolvedByRole.add(new ClientFlagView.RoleValue(roleEntry.role(), "ALLOW".equals(roleEntry.value())));
			}
			mapped.add(new ClientExceptionGroupView(entry.groupId(), entry.category(), resolvedByRole,
					entry.currentPreset(), entry.ownerConfigurable()));
		}
		exceptionGroups = List.copyOf(mapped);
	}

	public static List<ClientExceptionGroupView> getExceptionGroups() {
		return exceptionGroups;
	}

	public static void updateExceptionGroupPreset(String groupId, String preset) {
		for (ClientExceptionGroupView group : exceptionGroups) {
			if (group.groupId().equals(groupId)) {
				group.setCurrentPreset(preset);
				return;
			}
		}
	}
}
