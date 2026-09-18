package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.party.PartyStatusS2C;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

// Deliberately independent of ClientIslandCache: a party is not tied to any one island, same as
// the server keeps party/ independent of island/. Populated exclusively from PartyStatusS2C.
public final class ClientPartyCache {
	private static volatile boolean hasParty = false;
	private static volatile UUID partyId = PartyStatusS2C.NO_PARTY_UUID;
	private static volatile String name = "";
	private static volatile UUID leaderUuid = PartyStatusS2C.NO_PARTY_UUID;
	private static volatile String leaderName = "";

	private static final List<ClientPartyMemberView> MEMBERS = new ArrayList<>();

	@Nullable
	private static volatile ClientIncomingPartyInviteView incomingInvite = null;

	private ClientPartyCache() {
	}

	public static void applyStatus(PartyStatusS2C status) {
		hasParty = status.hasParty();
		partyId = status.partyId();
		name = status.name();
		leaderUuid = status.leaderUuid();
		leaderName = status.leaderName();

		MEMBERS.clear();
		for (PartyStatusS2C.MemberEntry entry : status.members()) {
			MEMBERS.add(new ClientPartyMemberView(entry.uuid(), entry.name()));
		}

		incomingInvite = status.incomingInvite()
				.map(entry -> new ClientIncomingPartyInviteView(entry.inviterName(), entry.partyName(), entry.expiresInSeconds()))
				.orElse(null);
	}

	public static boolean hasParty() {
		return hasParty;
	}

	public static UUID getPartyId() {
		return partyId;
	}

	public static String getName() {
		return name;
	}

	public static UUID getLeaderUuid() {
		return leaderUuid;
	}

	public static String getLeaderName() {
		return leaderName;
	}

	public static boolean isLeader(UUID localPlayerUuid) {
		return hasParty && leaderUuid.equals(localPlayerUuid);
	}

	public static List<ClientPartyMemberView> getMembers() {
		return MEMBERS;
	}

	@Nullable
	public static ClientIncomingPartyInviteView getIncomingInvite() {
		return incomingInvite;
	}

	public static void setIncomingInvite(@Nullable ClientIncomingPartyInviteView invite) {
		incomingInvite = invite;
	}
}
