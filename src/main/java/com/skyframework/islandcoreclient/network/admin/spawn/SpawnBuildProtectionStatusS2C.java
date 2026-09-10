package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Mirrors the server's net.admin.spawn.SpawnBuildProtectionStatusS2C exactly: enabled (the Spawn
// island's current BUILD_PROTECTION value, false if Spawn doesn't exist), authorizedPlayers (list
// of AuthorizedPlayerEntry: the Spawn island's MEMBER/CO_OWNER members, empty if Spawn doesn't
// exist).
public record SpawnBuildProtectionStatusS2C(boolean enabled, List<AuthorizedPlayerEntry> authorizedPlayers) implements CustomPayload {

	public static final CustomPayload.Id<SpawnBuildProtectionStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_build_protection_status_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<AuthorizedPlayerEntry>> AUTHORIZED_PLAYERS_CODEC =
			PacketCodecs.collection(ArrayList::new, AuthorizedPlayerEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionStatusS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnBuildProtectionStatusS2C::enabled,
			AUTHORIZED_PLAYERS_CODEC, SpawnBuildProtectionStatusS2C::authorizedPlayers,
			SpawnBuildProtectionStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnBuildProtectionStatusS2C> getId() {
		return ID;
	}

	public record AuthorizedPlayerEntry(UUID uuid, String name, String role) {
		public static final PacketCodec<RegistryByteBuf, AuthorizedPlayerEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, AuthorizedPlayerEntry::uuid,
				PacketCodecs.STRING, AuthorizedPlayerEntry::name,
				PacketCodecs.STRING, AuthorizedPlayerEntry::role,
				AuthorizedPlayerEntry::new
		);
	}
}
