package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Mirrors the server's net.admin.spawn.SpawnAuthorizedPlayerRemoveC2S exactly: targetUuid (UUID) —
// always targets an existing entry from the authorizedPlayers list the client already fetched via
// SpawnBuildProtectionStatusS2C.
public record SpawnAuthorizedPlayerRemoveC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<SpawnAuthorizedPlayerRemoveC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_authorized_player_remove_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnAuthorizedPlayerRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, SpawnAuthorizedPlayerRemoveC2S::targetUuid,
			SpawnAuthorizedPlayerRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnAuthorizedPlayerRemoveC2S> getId() {
		return ID;
	}
}
