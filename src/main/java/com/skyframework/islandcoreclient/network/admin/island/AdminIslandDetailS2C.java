package com.skyframework.islandcoreclient.network.admin.island;

import com.skyframework.islandcoreclient.network.island.IslandSnapshotS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Mirrors the server's net.admin.island.AdminIslandDetailS2C exactly: same 20 fields in the same
// order (islandId, ownerUuid, ownerName, dimension, gridX, gridZ, center, boundsMin, boundsMax,
// plotBoundsMin, plotBoundsMax, islandSize, maxSize, plotSize, islandType, homeLocation, members,
// state, createdAt, updatedAt, entities), past PacketCodec.tuple's 6-argument limit so
// hand-written with PacketCodec.of like IslandSnapshotS2C. members/entities reuse
// IslandSnapshotS2C's own nested MemberEntry/EntityCounts types as-is (confirmed identical field
// order to the server's copy), exactly like the server reuses its own IslandSnapshotS2C for the
// same reason. createdAt/updatedAt arrive already formatted (dd/MM/yyyy HH:mm) — no client-side
// date formatting needed. maxSize was added after islandSize (mirroring AdminIslandListS2C
// .IslandEntry's adjacent size/maxSize pairing) so the detail screen never needs to fall back to
// the list's cached row for it.
public record AdminIslandDetailS2C(
		UUID islandId,
		UUID ownerUuid,
		String ownerName,
		String dimension,
		int gridX,
		int gridZ,
		BlockPos center,
		BlockPos boundsMin,
		BlockPos boundsMax,
		BlockPos plotBoundsMin,
		BlockPos plotBoundsMax,
		int islandSize,
		int maxSize,
		int plotSize,
		String islandType,
		BlockPos homeLocation,
		List<IslandSnapshotS2C.MemberEntry> members,
		String state,
		String createdAt,
		String updatedAt,
		IslandSnapshotS2C.EntityCounts entities
) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDetailS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "admin_island_detail_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<IslandSnapshotS2C.MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, IslandSnapshotS2C.MemberEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminIslandDetailS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				Uuids.PACKET_CODEC.encode(buf, value.islandId());
				Uuids.PACKET_CODEC.encode(buf, value.ownerUuid());
				PacketCodecs.STRING.encode(buf, value.ownerName());
				PacketCodecs.STRING.encode(buf, value.dimension());
				PacketCodecs.VAR_INT.encode(buf, value.gridX());
				PacketCodecs.VAR_INT.encode(buf, value.gridZ());
				BlockPos.PACKET_CODEC.encode(buf, value.center());
				BlockPos.PACKET_CODEC.encode(buf, value.boundsMin());
				BlockPos.PACKET_CODEC.encode(buf, value.boundsMax());
				BlockPos.PACKET_CODEC.encode(buf, value.plotBoundsMin());
				BlockPos.PACKET_CODEC.encode(buf, value.plotBoundsMax());
				PacketCodecs.VAR_INT.encode(buf, value.islandSize());
				PacketCodecs.VAR_INT.encode(buf, value.maxSize());
				PacketCodecs.VAR_INT.encode(buf, value.plotSize());
				PacketCodecs.STRING.encode(buf, value.islandType());
				BlockPos.PACKET_CODEC.encode(buf, value.homeLocation());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				PacketCodecs.STRING.encode(buf, value.state());
				PacketCodecs.STRING.encode(buf, value.createdAt());
				PacketCodecs.STRING.encode(buf, value.updatedAt());
				IslandSnapshotS2C.EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new AdminIslandDetailS2C(
					Uuids.PACKET_CODEC.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.STRING.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					IslandSnapshotS2C.EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<AdminIslandDetailS2C> getId() {
		return ID;
	}
}
