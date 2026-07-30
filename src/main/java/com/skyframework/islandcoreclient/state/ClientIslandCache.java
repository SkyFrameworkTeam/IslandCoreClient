package com.skyframework.islandcoreclient.state;

import java.util.List;

import net.minecraft.text.Text;

public final class ClientIslandCache {
	// Simulated Sprint 2 data: the server doesn't implement an island snapshot packet yet.
	// Replace with the real IslandSnapshotS2C contents once that lands.
	private static final List<ClientIslandSettingView> SETTINGS = List.of(
			new ClientIslandSettingView("firespread", Text.translatable("islandcoreclient.settings.firespread"), false),
			new ClientIslandSettingView("pvp", Text.translatable("islandcoreclient.settings.pvp"), false),
			new ClientIslandSettingView("mobdamage", Text.translatable("islandcoreclient.settings.mobdamage"), false)
	);

	private static volatile boolean owner = true;

	private ClientIslandCache() {
	}

	public static List<ClientIslandSettingView> getSettings() {
		return SETTINGS;
	}

	public static boolean isOwner() {
		return owner;
	}

	public static void updateSetting(String key, boolean value) {
		for (ClientIslandSettingView setting : SETTINGS) {
			if (setting.key().equals(key)) {
				setting.setValue(value);
				return;
			}
		}
	}
}
