package com.skyframework.islandcoreclient.state;

// Mirrors ClientIncomingInviteView (the island-invite equivalent), plus partyName.
public final class ClientIncomingPartyInviteView {
	private final String inviterName;
	private final String partyName;
	private final int expiresInSeconds;

	public ClientIncomingPartyInviteView(String inviterName, String partyName, int expiresInSeconds) {
		this.inviterName = inviterName;
		this.partyName = partyName;
		this.expiresInSeconds = expiresInSeconds;
	}

	public String inviterName() {
		return inviterName;
	}

	public String partyName() {
		return partyName;
	}

	public int expiresInSeconds() {
		return expiresInSeconds;
	}
}
