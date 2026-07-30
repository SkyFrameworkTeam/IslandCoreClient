package com.skyframework.islandcoreclient.state;

public final class ClientIncomingInviteView {
	private final String fromName;

	public ClientIncomingInviteView(String fromName) {
		this.fromName = fromName;
	}

	public String fromName() {
		return fromName;
	}
}
