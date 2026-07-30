package com.skyframework.islandcoreclient.state;

public final class ClientPendingInviteView {
	private final String targetName;
	// Absolute deadline rather than a per-tick countdown: simpler and immune to missed/late ticks.
	private final long expiresAtMillis;

	public ClientPendingInviteView(String targetName, int expiresInSeconds) {
		this.targetName = targetName;
		this.expiresAtMillis = System.currentTimeMillis() + expiresInSeconds * 1000L;
	}

	public String targetName() {
		return targetName;
	}

	public long getRemainingSeconds() {
		return Math.max(0L, (expiresAtMillis - System.currentTimeMillis()) / 1000L);
	}

	public boolean isExpired() {
		return System.currentTimeMillis() >= expiresAtMillis;
	}
}
