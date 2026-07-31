package com.skyframework.islandcoreclient.state;

import java.util.Locale;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum ClientResetDimension {
	OVERWORLD(Formatting.GREEN),
	NETHER(Formatting.RED),
	END(Formatting.LIGHT_PURPLE);

	private final Formatting color;

	ClientResetDimension(Formatting color) {
		this.color = color;
	}

	// Centralized here so the list and every modal referencing a dimension use the same color.
	public Text label() {
		return Text.translatable("islandcoreclient.admin.reset_dimension." + name().toLowerCase(Locale.ROOT)).formatted(color);
	}
}
