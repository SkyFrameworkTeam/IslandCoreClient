package com.skyframework.islandcoreclient.state;

import java.util.Locale;

import net.minecraft.text.Text;

public enum ClientDimensionStyle {
	OVERWORLD_LIKE,
	NETHER_LIKE,
	END_LIKE,
	VOID_FLAT;

	public Text label() {
		return Text.translatable("islandcoreclient.admin.dimension_style." + name().toLowerCase(Locale.ROOT));
	}
}
