package com.skyframework.islandcoreclient.network.admin.spawn;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

// Mirrors the server's net.admin.spawn.SpawnStatusS2C exactly: exists, size (0 if !exists),
// homeLocation (empty if !exists). exists = false is a normal state (Spawn island simply doesn't
// exist yet), never wrapped in an ActionResultS2C failure.
public record SpawnStatusS2C(boolean exists, int size, Optional<BlockPos> homeLocation) implements CustomPayload {

	public static final CustomPayload.Id<SpawnStatusS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "spawn_status_s2c"));

	// See IslandSnapshotS2C's HOME_CODEC comment: PacketCodecs.optional needs an exact ByteBuf
	// match, so this stays typed over plain ByteBuf rather than RegistryByteBuf.
	private static final PacketCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = PacketCodecs.optional(BlockPos.PACKET_CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnStatusS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnStatusS2C::exists,
			PacketCodecs.VAR_INT, SpawnStatusS2C::size,
			HOME_CODEC, SpawnStatusS2C::homeLocation,
			SpawnStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnStatusS2C> getId() {
		return ID;
	}
}
