package com.skyframework.islandcoreclient.state;

public final class ClientIncomingInviteView {
	private final String fromName;
	private final int expiresInSeconds;

	public ClientIncomingInviteView(String fromName, int expiresInSeconds) {
		this.fromName = fromName;
		this.expiresInSeconds = expiresInSeconds;
	}

	public String fromName() {
		return fromName;
	}

	public int expiresInSeconds() {
		return expiresInSeconds;
	}
}
