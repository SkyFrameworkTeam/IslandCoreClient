package com.skyframework.islandcoreclient.state;

import org.jetbrains.annotations.Nullable;

public final class ClientTeleportState {
	private final boolean enabled;
	@Nullable
	private final String reasonKey;
	// Absolute deadline rather than a per-tick countdown, same reasoning as pending invites.
	private long cooldownEndMillis;

	public ClientTeleportState(boolean enabled, long cooldownRemainingSeconds, @Nullable String reasonKey) {
		this.enabled = enabled;
		this.reasonKey = reasonKey;
		this.cooldownEndMillis = System.currentTimeMillis() + cooldownRemainingSeconds * 1000L;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Nullable
	public String reasonKey() {
		return reasonKey;
	}

	public long getCooldownRemainingSeconds() {
		return Math.max(0L, (cooldownEndMillis - System.currentTimeMillis()) / 1000L);
	}

	public void startCooldown(long durationSeconds) {
		cooldownEndMillis = System.currentTimeMillis() + durationSeconds * 1000L;
	}

	public void clearCooldown() {
		cooldownEndMillis = 0L;
	}
}
