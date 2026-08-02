package com.skyframework.islandcoreclient.state;

import java.util.UUID;

public final class ClientAdminIslandSummaryView {
	private final UUID ownerUuid;
	private final String ownerName;
	private final int size;
	private final int maxSize;
	private final String type;
	private final String currentBiomeId;
	private final String state;
	private final int memberCount;

	public ClientAdminIslandSummaryView(UUID ownerUuid, String ownerName, int size, int maxSize,
			String type, String currentBiomeId, String state, int memberCount) {
		this.ownerUuid = ownerUuid;
		this.ownerName = ownerName;
		this.size = size;
		this.maxSize = maxSize;
		this.type = type;
		this.currentBiomeId = currentBiomeId;
		this.state = state;
		this.memberCount = memberCount;
	}

	public UUID ownerUuid() {
		return ownerUuid;
	}

	public String ownerName() {
		return ownerName;
	}

	public int size() {
		return size;
	}

	public int maxSize() {
		return maxSize;
	}

	public String type() {
		return type;
	}

	// LIVE lookup of the biome at the island's center block (see AdminIslandBuilder server-side),
	// not any locally-tracked value.
	public String currentBiomeId() {
		return currentBiomeId;
	}

	public String state() {
		return state;
	}

	public int memberCount() {
		return memberCount;
	}
}
