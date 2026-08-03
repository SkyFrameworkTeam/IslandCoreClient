package com.skyframework.islandcoreclient.gui.common;

// Shared mm:ss formatting for short countdowns (pending/incoming invites) — extracted so
// MembersScreen's pending-invite list and DashboardScreen's incoming-invite banner render the
// exact same format instead of each keeping their own copy of the same String.format call.
public final class TimeFormat {
	private TimeFormat() {
	}

	public static String minutesSeconds(long remainingSeconds) {
		return String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
	}
}
