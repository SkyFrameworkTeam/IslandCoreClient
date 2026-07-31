package com.skyframework.islandcoreclient.network;

import org.jetbrains.annotations.Nullable;

/**
 * Bridges a fire-and-forget C2S action packet to its {@link ActionResultS2C} reply. Only one
 * action is ever in flight at a time in practice (the player just clicked one button on a modal
 * screen), so a single global slot is enough — the packets themselves carry no request id to
 * correlate against, so a full request/response registry would be solving a problem that doesn't
 * exist here.
 */
public final class PendingActionTracker {
	@FunctionalInterface
	public interface ResultHandler {
		// reasonKey is null both on success and on a local timeout (no reply from the server at
		// all) — callers only need to distinguish those from a real server-sent failure reason,
		// which is always non-null.
		void onResult(boolean success, @Nullable String reasonKey);
	}

	// Same window ClientConnectionState uses for the handshake timeout: long enough to rule out
	// normal network jitter, short enough a screen doesn't feel stuck if the server never replies.
	private static final long TIMEOUT_MILLIS = 3000L;

	@Nullable
	private static volatile ResultHandler pendingHandler;
	private static volatile long deadlineMillis = -1L;

	private PendingActionTracker() {
	}

	public static void await(ResultHandler handler) {
		pendingHandler = handler;
		deadlineMillis = System.currentTimeMillis() + TIMEOUT_MILLIS;
	}

	public static void onActionResult(ActionResultS2C result) {
		ResultHandler handler = pendingHandler;
		if (handler == null) {
			return;
		}
		pendingHandler = null;
		deadlineMillis = -1L;
		handler.onResult(result.success(), result.reasonKey().orElse(null));
	}

	// Called every client tick from ClientPacketHandlers.
	public static void tick() {
		if (pendingHandler != null && deadlineMillis > 0 && System.currentTimeMillis() >= deadlineMillis) {
			ResultHandler handler = pendingHandler;
			pendingHandler = null;
			deadlineMillis = -1L;
			handler.onResult(false, null);
		}
	}

	public static void reset() {
		pendingHandler = null;
		deadlineMillis = -1L;
	}
}
