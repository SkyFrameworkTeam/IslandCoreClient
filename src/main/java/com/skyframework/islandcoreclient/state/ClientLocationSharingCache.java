package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.alliance.LocationSharingStatusS2C;

// The local player's own four location-sharing toggles (see PlayerLocationSharingConfig
// server-side): an independent party pair and allies pair. Populated exclusively from
// LocationSharingStatusS2C; PartyScreen updates these fields optimistically itself right before
// sending LocationSharingSetC2S (same pattern ToggleRow-based rows use elsewhere), reverting on
// failure.
public final class ClientLocationSharingCache {
	private static volatile boolean sendPositionToParty = false;
	private static volatile boolean receivePositionsFromParty = false;
	private static volatile boolean sendPositionToAllies = false;
	private static volatile boolean receivePositionsFromAllies = false;

	private ClientLocationSharingCache() {
	}

	public static void applyStatus(LocationSharingStatusS2C status) {
		sendPositionToParty = status.sendPositionToParty();
		receivePositionsFromParty = status.receivePositionsFromParty();
		sendPositionToAllies = status.sendPositionToAllies();
		receivePositionsFromAllies = status.receivePositionsFromAllies();
	}

	public static boolean isSendPositionToPartyEnabled() {
		return sendPositionToParty;
	}

	public static boolean isReceivePositionsFromPartyEnabled() {
		return receivePositionsFromParty;
	}

	public static boolean isSendPositionToAlliesEnabled() {
		return sendPositionToAllies;
	}

	public static boolean isReceivePositionsFromAlliesEnabled() {
		return receivePositionsFromAllies;
	}

	public static void setSendPositionToParty(boolean value) {
		sendPositionToParty = value;
	}

	public static void setReceivePositionsFromParty(boolean value) {
		receivePositionsFromParty = value;
	}

	public static void setSendPositionToAllies(boolean value) {
		sendPositionToAllies = value;
	}

	public static void setReceivePositionsFromAllies(boolean value) {
		receivePositionsFromAllies = value;
	}
}
