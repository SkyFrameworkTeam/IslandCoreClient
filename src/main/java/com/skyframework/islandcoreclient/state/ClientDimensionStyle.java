package com.skyframework.islandcoreclient.state;

import java.util.Locale;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum ClientDimensionStyle {
	OVERWORLD_LIKE(Formatting.GREEN),
	NETHER_LIKE(Formatting.RED),
	END_LIKE(Formatting.LIGHT_PURPLE),
	// Same 4-color convention ClientResetDimension already uses for Overworld/Nether/End (LIGHT_PURPLE
	// there too) — VOID_FLAT has no vanilla-dimension counterpart there, so BLACK is this enum's own
	// addition, not reused from anywhere else.
	VOID_FLAT(Formatting.BLACK);

	private final Formatting color;

	ClientDimensionStyle(Formatting color) {
		this.color = color;
	}

	public Text label() {
		return Text.translatable("islandcoreclient.admin.dimension_style." + name().toLowerCase(Locale.ROOT));
	}

	// Used by DimensionManagerScreen's own dimension list to color each row's displayName per its
	// style — kept as a plain accessor rather than a "colored label" helper since label() here is
	// this STYLE's own translated name ("Overworld-like", etc.), not a dimension's displayName.
	public Formatting color() {
		return color;
	}
}
