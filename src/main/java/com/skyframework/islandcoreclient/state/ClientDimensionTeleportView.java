package com.skyframework.islandcoreclient.state;

// One row of TeleportsScreen's paginated dynamic-dimensions section (Sprint "teletransportes
// dinámicos") — id is the raw Identifier string (e.g. "islandcore:farming"), sent back verbatim in
// TeleportRequestC2S.dimension(id) when the button is clicked. state reuses ClientTeleportState so
// the same "MM:SS" cooldown-countdown rendering TeleportsScreen already has for HOME/SPAWN/RTP
// works here unchanged; unlike those three, a dimension entry never carries a reasonKey (the
// server's DimensionTeleportEntry has no such field — see its class doc).
public final class ClientDimensionTeleportView {
	private final String id;
	private final String displayName;
	private final ClientTeleportState state;

	public ClientDimensionTeleportView(String id, String displayName, ClientTeleportState state) {
		this.id = id;
		this.displayName = displayName;
		this.state = state;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public ClientTeleportState state() {
		return state;
	}
}
