package com.skyframework.islandcoreclient.state;

import net.minecraft.text.Text;

import java.util.List;

public final class ClientFlagView {
	public enum Category {
		ROLE_BASED,
		ISLAND_GLOBAL
	}

	// role: the server's IslandRole#name() (OWNER/MEMBER/TRUSTED/ALLY/VISITOR/DENIED). allow:
	// true/false, never "default" — FlagResolver#resolveForRole never returns DEFAULT.
	public record RoleValue(String role, boolean allow) {
	}

	private final String flagId;
	private final Category category;
	private boolean resolvedValue;
	private final List<RoleValue> resolvedByRole;
	private ClientTriState islandOverride;
	// ROLE_BASED only: "nadie"/"miembros"/"aliados"/"todos" if the current role combination exactly
	// matches one of FlagSetPresetC2S's 4 presets, or "custom" otherwise. Empty for ISLAND_GLOBAL.
	private String currentPreset;

	public ClientFlagView(String flagId, Category category, boolean resolvedValue, List<RoleValue> resolvedByRole,
			ClientTriState islandOverride, String currentPreset) {
		this.flagId = flagId;
		this.category = category;
		this.resolvedValue = resolvedValue;
		this.resolvedByRole = resolvedByRole;
		this.islandOverride = islandOverride;
		this.currentPreset = currentPreset;
	}

	public String flagId() {
		return flagId;
	}

	public Category category() {
		return category;
	}

	public boolean resolvedValue() {
		return resolvedValue;
	}

	public List<RoleValue> resolvedByRole() {
		return resolvedByRole;
	}

	public ClientTriState islandOverride() {
		return islandOverride;
	}

	public void setIslandOverride(ClientTriState islandOverride) {
		this.islandOverride = islandOverride;
	}

	public String currentPreset() {
		return currentPreset;
	}

	public void setCurrentPreset(String currentPreset) {
		this.currentPreset = currentPreset;
	}

	// Presentation only — flag ids themselves (used by FlagSetC2S/FlagSetPresetC2S) are unchanged.
	// Falls back to the raw id for anything not in this fixed list (a custom server can't add new
	// flags, so this should always match in practice).
	public Text label() {
		return labelFor(flagId);
	}

	// Static so DefaultConfigScreen (which only has a flagId + currentPreset from
	// AdminDefaultsStatusS2C, not a full ClientFlagView) can reuse the exact same labels instead of
	// duplicating this switch.
	public static Text labelFor(String flagId) {
		return switch (flagId) {
			case "construccion" -> Text.translatable("islandcoreclient.flags.flag.construccion");
			case "interact" -> Text.translatable("islandcoreclient.flags.flag.interact");
			case "containers" -> Text.translatable("islandcoreclient.flags.flag.containers");
			case "entities" -> Text.translatable("islandcoreclient.flags.flag.entities");
			case "redstone" -> Text.translatable("islandcoreclient.flags.flag.redstone");
			case "fire_spread" -> Text.translatable("islandcoreclient.flags.flag.fire_spread");
			case "pvp_damage" -> Text.translatable("islandcoreclient.flags.flag.pvp_damage");
			case "mob_damage" -> Text.translatable("islandcoreclient.flags.flag.mob_damage");
			default -> Text.literal(flagId);
		};
	}

	// A one-sentence explanation of what this flag actually controls — from the lang file, not
	// hardcoded, so translators can localize it like everything else.
	public Text description() {
		return Text.translatable("islandcoreclient.flags.flag." + flagId + ".description");
	}

}
