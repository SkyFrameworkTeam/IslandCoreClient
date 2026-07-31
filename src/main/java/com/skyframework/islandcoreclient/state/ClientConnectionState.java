package com.skyframework.islandcoreclient.state;

public final class ClientConnectionState {
	// Long enough to rule out normal network jitter, short enough that a real server without
	// IslandCore doesn't leave the player staring at "Conectando..." for long.
	private static final long HANDSHAKE_TIMEOUT_MILLIS = 3000L;

	public enum Status {
		UNKNOWN,
		UNSUPPORTED,
		CONNECTED
	}

	private static volatile Status status = Status.UNKNOWN;
	private static volatile int protocolVersion = -1;
	private static volatile boolean operator = false;
	private static volatile long handshakeSentAtMillis = -1L;

	private ClientConnectionState() {
	}

	public static Status getStatus() {
		return status;
	}

	public static int getProtocolVersion() {
		return protocolVersion;
	}

	public static boolean isOperator() {
		return operator;
	}

	public static void onHandshakeSent() {
		status = Status.UNKNOWN;
		handshakeSentAtMillis = System.currentTimeMillis();
	}

	public static void onServerHandshakeReceived(int protocolVersion, boolean isOperator) {
		ClientConnectionState.protocolVersion = protocolVersion;
		ClientConnectionState.operator = isOperator;
		status = Status.CONNECTED;
	}

	public static void reset() {
		status = Status.UNKNOWN;
		protocolVersion = -1;
		operator = false;
		handshakeSentAtMillis = -1L;
	}

	public static void tickTimeout() {
		if (status == Status.UNKNOWN && handshakeSentAtMillis > 0
				&& System.currentTimeMillis() - handshakeSentAtMillis >= HANDSHAKE_TIMEOUT_MILLIS) {
			status = Status.UNSUPPORTED;
		}
	}
}
