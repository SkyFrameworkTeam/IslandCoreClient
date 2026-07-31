package com.skyframework.islandcoreclient.state;

import java.util.List;
import java.util.UUID;

public final class ClientAdminIslandDetailView {
	public record EntityCounts(int players, int hostile, int passive, int cobblemon, int items, int other) {
	}

	private final String islandId;
	private final UUID ownerUuid;
	private final String ownerName;
	private final String dimension;
	private final int gridX;
	private final int gridZ;
	private final int size;
	private final int maxSize;
	private final int plotSize;
	private final List<ClientMemberView> members;
	private final String state;
	private final String createdAt;
	private final String updatedAt;
	private final EntityCounts entities;

	public ClientAdminIslandDetailView(String islandId, UUID ownerUuid, String ownerName, String dimension,
			int gridX, int gridZ, int size, int maxSize, int plotSize, List<ClientMemberView> members,
			String state, String createdAt, String updatedAt, EntityCounts entities) {
		this.islandId = islandId;
		this.ownerUuid = ownerUuid;
		this.ownerName = ownerName;
		this.dimension = dimension;
		this.gridX = gridX;
		this.gridZ = gridZ;
		this.size = size;
		this.maxSize = maxSize;
		this.plotSize = plotSize;
		this.members = members;
		this.state = state;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.entities = entities;
	}

	public String islandId() {
		return islandId;
	}

	public UUID ownerUuid() {
		return ownerUuid;
	}

	public String ownerName() {
		return ownerName;
	}

	public String dimension() {
		return dimension;
	}

	public int gridX() {
		return gridX;
	}

	public int gridZ() {
		return gridZ;
	}

	public int size() {
		return size;
	}

	public int maxSize() {
		return maxSize;
	}

	public int plotSize() {
		return plotSize;
	}

	public List<ClientMemberView> members() {
		return members;
	}

	public String state() {
		return state;
	}

	public String createdAt() {
		return createdAt;
	}

	public String updatedAt() {
		return updatedAt;
	}

	public EntityCounts entities() {
		return entities;
	}
}
