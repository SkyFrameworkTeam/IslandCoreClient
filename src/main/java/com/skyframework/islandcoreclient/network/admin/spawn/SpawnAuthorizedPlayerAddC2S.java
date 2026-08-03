package com.skyframework.islandcoreclient.network.admin.spawn;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server's net.admin.spawn.SpawnAuthorizedPlayerAddC2S exactly: targetName (String) —
// the client has no reliable way to know an offline player's UUID up front, resolved server-side.
public record SpawnAuthorizedPlayerAddC2S(String targetName) implements CustomPayload {

	public static final CustomPayload.Id<SpawnAuthorizedPlayerAddC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_authorized_player_add_c2s"));

	public static final PacketCodec<RegistryByteBuf, SpawnAuthorizedPlayerAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnAuthorizedPlayerAddC2S::targetName,
			SpawnAuthorizedPlayerAddC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnAuthorizedPlayerAddC2S> getId() {
		return ID;
	}
}
