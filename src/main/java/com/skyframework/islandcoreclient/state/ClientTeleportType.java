package com.skyframework.islandcoreclient.state;

import java.util.Locale;

import net.minecraft.text.Text;

// FARMING removed (Sprint "teletransportes dinámicos"): it's now just another entry in
// ClientIslandCache's dimensionTeleports list, same as every other DIMENSION_REGISTRY dimension —
// see ClientDimensionTeleportView.
public enum ClientTeleportType {
	HOME,
	SPAWN,
	RTP;

	public Text label() {
		return Text.translatable("islandcoreclient.teleports.type." + name().toLowerCase(Locale.ROOT));
	}
}
