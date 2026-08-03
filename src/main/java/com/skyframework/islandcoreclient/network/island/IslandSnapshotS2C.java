package com.skyframework.islandcoreclient.network.island;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Mirrors the server's net.island.IslandSnapshotS2C exactly: same 13 fields in the same order,
// same hand-written PacketCodec.of (past PacketCodec.tuple's 6-argument limit), same nested
// per-entry records/codecs. type is the island's IslandType id (server-side always "plains" for
// now — a distinct, mostly-unused concept from the current biome, NOT what BiomeScreen changes).
// currentBiomeId is the real current biome (e.g. "minecraft:jungle", or IslandData.DEFAULT_BIOME_ID
// = "minecraft:the_void" for an island that's never had /island biome used on it) — this is what
// BiomeScreen's "current" marker and Dashboard's summary line should read, not type.
//
// Wire format changed: biomeCooldownRemainingSeconds was inserted after currentBiomeId (grouped
// with the other biome field), and incomingInvite was inserted after pendingInvites (grouped with
// the other invite field). Both are now the real, server-computed values — see ClientIslandCache
// #applySnapshot, which no longer needs to simulate either locally.
public record IslandSnapshotS2C(
		boolean exists,
		int size,
		int maxSize,
		String type,
		String currentBiomeId,
		int biomeCooldownRemainingSeconds,
		Optional<BlockPos> home,
		String state,
		List<MemberEntry> members,
		List<PendingInviteEntry> pendingInvites,
		Optional<IncomingInviteEntry> incomingInvite,
		List<SettingEntry> settings,
		EntityCounts entities
) implements CustomPayload {

	public static final CustomPayload.Id<IslandSnapshotS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_snapshot_s2c"));

	private static final PacketCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = PacketCodecs.optional(BlockPos.PACKET_CODEC);
	// IncomingInviteEntry.CODEC is already typed over RegistryByteBuf (like every other nested
	// entry here), so unlike HOME_CODEC above this needs no ByteBuf/RegistryByteBuf split.
	private static final PacketCodec<RegistryByteBuf, Optional<IncomingInviteEntry>> INCOMING_INVITE_CODEC =
			PacketCodecs.optional(IncomingInviteEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<PendingInviteEntry>> PENDING_INVITE_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, PendingInviteEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<SettingEntry>> SETTING_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, SettingEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, IslandSnapshotS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.BOOL.encode(buf, value.exists());
				PacketCodecs.VAR_INT.encode(buf, value.size());
				PacketCodecs.VAR_INT.encode(buf, value.maxSize());
				PacketCodecs.STRING.encode(buf, value.type());
				PacketCodecs.STRING.encode(buf, value.currentBiomeId());
				PacketCodecs.VAR_INT.encode(buf, value.biomeCooldownRemainingSeconds());
				HOME_CODEC.encode(buf, value.home());
				PacketCodecs.STRING.encode(buf, value.state());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				PENDING_INVITE_LIST_CODEC.encode(buf, value.pendingInvites());
				INCOMING_INVITE_CODEC.encode(buf, value.incomingInvite());
				SETTING_LIST_CODEC.encode(buf, value.settings());
				EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new IslandSnapshotS2C(
					PacketCodecs.BOOL.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					HOME_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					PENDING_INVITE_LIST_CODEC.decode(buf),
					INCOMING_INVITE_CODEC.decode(buf),
					SETTING_LIST_CODEC.decode(buf),
					EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<IslandSnapshotS2C> getId() {
		return ID;
	}

	public record MemberEntry(UUID uuid, String name, String role) {
		public static final PacketCodec<RegistryByteBuf, MemberEntry> CODEC = PacketCodec.tuple(
				net.minecraft.util.Uuids.PACKET_CODEC, MemberEntry::uuid,
				PacketCodecs.STRING, MemberEntry::name,
				PacketCodecs.STRING, MemberEntry::role,
				MemberEntry::new
		);
	}

	public record PendingInviteEntry(String targetName, int expiresInSeconds) {
		public static final PacketCodec<RegistryByteBuf, PendingInviteEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, PendingInviteEntry::targetName,
				PacketCodecs.VAR_INT, PendingInviteEntry::expiresInSeconds,
				PendingInviteEntry::new
		);
	}

	// An invite where the receiving player is the INVITEE, not the island's owner (contrast
	// PendingInviteEntry above, which lists invites the player's own island sent out). Empty
	// (IslandSnapshotS2C#incomingInvite) means no pending incoming invite, or it already expired.
	public record IncomingInviteEntry(String inviterName, int expiresInSeconds) {
		public static final PacketCodec<RegistryByteBuf, IncomingInviteEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, IncomingInviteEntry::inviterName,
				PacketCodecs.VAR_INT, IncomingInviteEntry::expiresInSeconds,
				IncomingInviteEntry::new
		);
	}

	// key is the server's IslandSetting enum CONSTANT NAME (e.g. "FIRE_SPREAD"), not its id
	// ("firespread") — see ClientIslandCache's mapping when consuming this.
	public record SettingEntry(String key, boolean value) {
		public static final PacketCodec<RegistryByteBuf, SettingEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, SettingEntry::key,
				PacketCodecs.BOOL, SettingEntry::value,
				SettingEntry::new
		);
	}

	public record EntityCounts(int players, int hostile, int passive, int cobblemon, int items, int other) {
		public static final PacketCodec<RegistryByteBuf, EntityCounts> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, EntityCounts::players,
				PacketCodecs.VAR_INT, EntityCounts::hostile,
				PacketCodecs.VAR_INT, EntityCounts::passive,
				PacketCodecs.VAR_INT, EntityCounts::cobblemon,
				PacketCodecs.VAR_INT, EntityCounts::items,
				PacketCodecs.VAR_INT, EntityCounts::other,
				EntityCounts::new
		);

		public static final EntityCounts EMPTY = new EntityCounts(0, 0, 0, 0, 0, 0);
	}
}
