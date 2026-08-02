package com.skyframework.islandcoreclient.state;

// DEBUG - quitar cuando haya datos reales: fuerza estados que, a día de hoy, siguen sin tener
// ninguna fuente de red real (ver Sprint "Integración de red real" para el resto de estados, que
// ya se rellenan solos desde IslandSnapshotS2C/TeleportStatusS2C/BiomeTiersS2C/ServerHandshakeS2C
// y ya no necesitan un botón [DEBUG] que los fuerce).
public final class DebugSimulationHelpers {
	// El valor real de un cambio de bioma (7 días), para poder probar el formato días/horas.
	private static final long BIOME_COOLDOWN_DEBUG_SECONDS = 604800L;

	private DebugSimulationHelpers() {
	}

	// Ningún paquete actual informa de invitaciones ENTRANTES (donde el jugador local es el
	// invitado, no el dueño) — ni el snapshot ni ningún otro. Sigue siendo puramente local.
	public static void toggleIncomingInviteDebug() {
		if (ClientIslandCache.getIncomingInvite() == null) {
			ClientIslandCache.setIncomingInvite(new ClientIncomingInviteView("Peroten"));
		} else {
			ClientIslandCache.setIncomingInvite(null);
		}
	}

	// Ni IslandSnapshotS2C ni BiomeTiersS2C exponen el cooldown de cambio de bioma restante ni el
	// bioma actual de la isla — sigue siendo local/optimista (ver ClientIslandCache).
	public static void toggleBiomeCooldownDebug() {
		if (ClientIslandCache.getBiomeCooldownRemainingSeconds() > 0) {
			ClientIslandCache.clearBiomeCooldown();
		} else {
			ClientIslandCache.startBiomeCooldown(BIOME_COOLDOWN_DEBUG_SECONDS);
		}
	}
}
