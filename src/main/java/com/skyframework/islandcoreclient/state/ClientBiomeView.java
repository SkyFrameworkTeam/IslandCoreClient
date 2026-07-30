package com.skyframework.islandcoreclient.state;

import net.minecraft.text.Text;

public final class ClientBiomeView {
	private final String biomeId;
	private final Text label;

	public ClientBiomeView(String biomeId, Text label) {
		this.biomeId = biomeId;
		this.label = label;
	}

	public String biomeId() {
		return biomeId;
	}

	public Text label() {
		return label;
	}
}
