package com.skyframework.islandcoreclient.state;

// DEBUG - quitar cuando haya snapshot real: fuerza estados difíciles de alcanzar sin red real
// (invitación entrante, cooldown de bioma, handshake conectado, cooldowns de teletransporte)
// para poder probarlos desde el Dashboard sin depender de que el servidor implemente el
// protocolo todavía.
public final class DebugSimulationHelpers {
	// El valor real de un cambio de bioma (7 días), para poder probar el formato días/horas.
	private static final long BIOME_COOLDOWN_DEBUG_SECONDS = 604800L;
	private static final int FAKE_PROTOCOL_VERSION = 1;
	// Corto a propósito (5 min): suficiente para ver el formato mm:ss sin esperar los 600s reales.
	private static final long TELEPORT_COOLDOWN_DEBUG_SECONDS = 300L;

	private DebugSimulationHelpers() {
	}

	// The dev client has no IslandCore server to answer the real handshake, so
	// ClientConnectionState never reaches CONNECTED on its own: this fakes that response.
	public static void forceConnectedDebug() {
		ClientConnectionState.onServerHandshakeReceived(FAKE_PROTOCOL_VERSION, false);
	}

	public static void toggleHasIslandDebug() {
		ClientIslandCache.setHasIsland(!ClientIslandCache.hasIsland());
	}

	public static void toggleIncomingInviteDebug() {
		if (ClientIslandCache.getIncomingInvite() == null) {
			ClientIslandCache.setIncomingInvite(new ClientIncomingInviteView("Peroten"));
		} else {
			ClientIslandCache.setIncomingInvite(null);
		}
	}

	public static void toggleBiomeCooldownDebug() {
		if (ClientIslandCache.getBiomeCooldownRemainingSeconds() > 0) {
			ClientIslandCache.clearBiomeCooldown();
		} else {
			ClientIslandCache.startBiomeCooldown(BIOME_COOLDOWN_DEBUG_SECONDS);
		}
	}

	// RTP/Farming stay untouched: their block is a fixed reasonKey, not a cooldown.
	public static void toggleTeleportCooldownsDebug() {
		boolean anyOnCooldown = false;
		for (ClientTeleportType type : ClientTeleportType.values()) {
			ClientTeleportState state = ClientIslandCache.getTeleportState(type);
			if (state.isEnabled() && state.getCooldownRemainingSeconds() > 0) {
				anyOnCooldown = true;
				break;
			}
		}

		for (ClientTeleportType type : ClientTeleportType.values()) {
			ClientTeleportState state = ClientIslandCache.getTeleportState(type);
			if (!state.isEnabled()) {
				continue;
			}
			if (anyOnCooldown) {
				state.clearCooldown();
			} else {
				state.startCooldown(TELEPORT_COOLDOWN_DEBUG_SECONDS);
			}
		}
	}
}
