package com.skyframework.islandcoreclient.state;

import java.util.Locale;

import net.minecraft.text.Text;

public enum ClientTeleportType {
	HOME,
	SPAWN,
	RTP,
	FARMING;

	public Text label() {
		return Text.translatable("islandcoreclient.teleports.type." + name().toLowerCase(Locale.ROOT));
	}
}
