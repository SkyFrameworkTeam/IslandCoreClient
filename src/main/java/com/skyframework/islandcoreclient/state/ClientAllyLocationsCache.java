package com.skyframework.islandcoreclient.state;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

// Populated from AllyLocationsS2C's periodic push (see ClientPacketHandlers) — read exclusively by
// AllyHudRenderer. Not tied to any Screen: the HUD indicator this feeds keeps updating even while
// no IslandCore menu is open, since the server keeps pushing regardless.
public final class ClientAllyLocationsCache {
	// Safety net, not the primary mechanism: applyLocations already REPLACES entries wholesale on
	// every AllyLocationsS2C (the server pushes one every 500ms, see AllyLocationBroadcaster, even
	// when empty), so a disconnected ally is normally gone from the very next packet. This only
	// matters if that periodic feed itself stops reaching this client for some reason (a dropped
	// packet, a reconnect racing the next broadcast, etc.) — without it, the last snapshot received
	// would otherwise keep rendering forever instead of clearing itself out.
	private static final long STALE_AFTER_MILLIS = 2_000L;

	private static volatile List<ClientAllyLocationView> entries = List.of();
	private static volatile long lastUpdatedAtMillis = 0L;

	// [DEBUG] see AllianceScreen's debug button: injects a fake ally at a fixed known position so
	// the HUD indicator's on-screen/off-screen projection can be exercised solo, without a second
	// connected player actually sharing their position. Kept separate from entries above so it
	// survives across real AllyLocationsS2C pushes instead of being overwritten by them, until
	// explicitly cleared (calling this again with null).
	@Nullable
	private static volatile ClientAllyLocationView debugEntry = null;

	private ClientAllyLocationsCache() {
	}

	public static void applyLocations(List<ClientAllyLocationView> newEntries) {
		entries = newEntries;
		lastUpdatedAtMillis = System.currentTimeMillis();
	}

	public static List<ClientAllyLocationView> getEntries() {
		List<ClientAllyLocationView> base = isStale() ? List.of() : entries;
		if (debugEntry == null) {
			return base;
		}
		List<ClientAllyLocationView> combined = new ArrayList<>(base);
		combined.add(debugEntry);
		return combined;
	}

	private static boolean isStale() {
		return lastUpdatedAtMillis != 0L && System.currentTimeMillis() - lastUpdatedAtMillis > STALE_AFTER_MILLIS;
	}

	public static void setDebugEntry(@Nullable ClientAllyLocationView entry) {
		debugEntry = entry;
	}

	public static boolean hasDebugEntry() {
		return debugEntry != null;
	}
}
