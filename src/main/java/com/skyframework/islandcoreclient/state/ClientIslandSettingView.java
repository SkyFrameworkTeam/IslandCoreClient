package com.skyframework.islandcoreclient.state;

import net.minecraft.text.Text;

/**
 * Client-side view of a single island setting (e.g. firespread, pvp). Named distinctly from
 * whatever the server mod may one day expose under "IslandSetting" to avoid a name clash.
 */
public final class ClientIslandSettingView {
	private final String key;
	private final Text label;
	private boolean value;

	public ClientIslandSettingView(String key, Text label, boolean value) {
		this.key = key;
		this.label = label;
		this.value = value;
	}

	public String key() {
		return key;
	}

	public Text label() {
		return label;
	}

	public boolean value() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}
}
