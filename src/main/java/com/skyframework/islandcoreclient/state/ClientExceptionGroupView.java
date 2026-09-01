package com.skyframework.islandcoreclient.state;

import net.minecraft.text.Text;

import java.util.List;

// Exception groups now resolve per role exactly like ROLE_BASED flags (see the server's
// ExceptionResolver) — mirrors ClientFlagView's shape instead of the old single "enabled" boolean.
public final class ClientExceptionGroupView {
	private final String groupId;
	private final String category;
	private final List<ClientFlagView.RoleValue> resolvedByRole;
	// "nadie"/"miembros"/"aliados"/"todos" if the current role combination exactly matches one of
	// FlagSetPresetC2S's 4 presets, or "custom" otherwise.
	private String currentPreset;
	private final boolean ownerConfigurable;

	public ClientExceptionGroupView(String groupId, String category, List<ClientFlagView.RoleValue> resolvedByRole,
			String currentPreset, boolean ownerConfigurable) {
		this.groupId = groupId;
		this.category = category;
		this.resolvedByRole = resolvedByRole;
		this.currentPreset = currentPreset;
		this.ownerConfigurable = ownerConfigurable;
	}

	public String groupId() {
		return groupId;
	}

	public String category() {
		return category;
	}

	public List<ClientFlagView.RoleValue> resolvedByRole() {
		return resolvedByRole;
	}

	public String currentPreset() {
		return currentPreset;
	}

	public void setCurrentPreset(String currentPreset) {
		this.currentPreset = currentPreset;
	}

	public boolean ownerConfigurable() {
		return ownerConfigurable;
	}

	// Presentation only. Server-defined groups (doors/chests/redstone/animals) get a translated
	// label; anything else (a custom server config) falls back to the raw id.
	public Text label() {
		return labelFor(groupId);
	}

	// Static so DefaultConfigScreen (which only has a groupId + currentPreset from
	// AdminDefaultsStatusS2C, not a full ClientExceptionGroupView) can reuse the exact same labels
	// instead of duplicating this switch.
	public static Text labelFor(String groupId) {
		return switch (groupId) {
			case "doors" -> Text.translatable("islandcoreclient.exceptions.group.doors");
			case "chests" -> Text.translatable("islandcoreclient.exceptions.group.chests");
			case "redstone" -> Text.translatable("islandcoreclient.exceptions.group.redstone");
			case "animals" -> Text.translatable("islandcoreclient.exceptions.group.animals");
			default -> Text.literal(groupId);
		};
	}

	// A one-sentence explanation of what this group actually does — from the lang file, not
	// hardcoded.
	public Text description() {
		return Text.translatable("islandcoreclient.exceptions.group." + groupId + ".description");
	}

}
