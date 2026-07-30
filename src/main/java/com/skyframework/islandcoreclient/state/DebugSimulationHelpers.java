package com.skyframework.islandcoreclient.state;

// DEBUG - quitar cuando haya snapshot real: fuerza estados difíciles de alcanzar sin red real
// (invitación entrante, cooldown de bioma) para poder probarlos desde el Dashboard.
public final class DebugSimulationHelpers {
	// El valor real de un cambio de bioma (7 días), para poder probar el formato días/horas.
	private static final long BIOME_COOLDOWN_DEBUG_SECONDS = 604800L;

	private DebugSimulationHelpers() {
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
}
