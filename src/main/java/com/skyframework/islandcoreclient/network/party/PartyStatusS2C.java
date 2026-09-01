package com.skyframework.islandcoreclient.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Mirrors the server's net.party.PartyStatusS2C exactly: 8 fields, hand-written PacketCodec.of
// (past PacketCodec.tuple's 6-argument limit). hasParty = false is a normal, valid state (not an
// error), same as IslandSnapshotS2C#exists — every other field is a default/empty placeholder in
// that case, EXCEPT incomingInvite, which is the one field that can still be populated then.
//
// incomingInvite mirrors IslandSnapshotS2C#incomingInvite: an invite where the receiving player is
// the INVITEE. In practice only ever non-empty when hasParty is false — the server refuses to
// invite a player who's already in a party, so a player who has one can never also have a pending
// invite.
public record PartyStatusS2C(
		boolean hasParty,
		UUID partyId,
		String name,
		UUID leaderUuid,
		String leaderName,
		List<MemberEntry> members,
		List<AlliedPartyEntry> alliedParties,
		Optional<IncomingPartyInviteEntry> incomingInvite
) implements CustomPayload {

	public static final CustomPayload.Id<PartyStatusS2C> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "party_status_s2c"));

	// Sentinel for "no party" — mirrors Island.SERVER_OWNER_UUID's own zero-UUID convention server-side.
	public static final UUID NO_PARTY_UUID = new UUID(0, 0);

	private static final PacketCodec<RegistryByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<AlliedPartyEntry>> ALLIED_PARTY_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, AlliedPartyEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, Optional<IncomingPartyInviteEntry>> INCOMING_INVITE_CODEC =
			PacketCodecs.optional(IncomingPartyInviteEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, PartyStatusS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.BOOL.encode(buf, value.hasParty());
				Uuids.PACKET_CODEC.encode(buf, value.partyId());
				PacketCodecs.STRING.encode(buf, value.name());
				Uuids.PACKET_CODEC.encode(buf, value.leaderUuid());
				PacketCodecs.STRING.encode(buf, value.leaderName());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				ALLIED_PARTY_LIST_CODEC.encode(buf, value.alliedParties());
				INCOMING_INVITE_CODEC.encode(buf, value.incomingInvite());
			},
			buf -> new PartyStatusS2C(
					PacketCodecs.BOOL.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					ALLIED_PARTY_LIST_CODEC.decode(buf),
					INCOMING_INVITE_CODEC.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<PartyStatusS2C> getId() {
		return ID;
	}

	public record MemberEntry(UUID uuid, String name) {
		public static final PacketCodec<RegistryByteBuf, MemberEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, MemberEntry::uuid,
				PacketCodecs.STRING, MemberEntry::name,
				MemberEntry::new
		);
	}

	public record AlliedPartyEntry(UUID partyId, String name) {
		public static final PacketCodec<RegistryByteBuf, AlliedPartyEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, AlliedPartyEntry::partyId,
				PacketCodecs.STRING, AlliedPartyEntry::name,
				AlliedPartyEntry::new
		);
	}

	public record IncomingPartyInviteEntry(String inviterName, String partyName, int expiresInSeconds) {
		public static final PacketCodec<RegistryByteBuf, IncomingPartyInviteEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, IncomingPartyInviteEntry::inviterName,
				PacketCodecs.STRING, IncomingPartyInviteEntry::partyName,
				PacketCodecs.VAR_INT, IncomingPartyInviteEntry::expiresInSeconds,
				IncomingPartyInviteEntry::new
		);
	}
}
