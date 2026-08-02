package com.skyframework.islandcoreclient.state;

import java.util.List;
import java.util.UUID;

import net.minecraft.util.math.BlockPos;

public final class ClientAdminIslandDetailView {
	public record EntityCounts(int players, int hostile, int passive, int cobblemon, int items, int other) {
	}

	private final String islandId;
	private final UUID ownerUuid;
	private final String ownerName;
	private final String dimension;
	private final int gridX;
	private final int gridZ;
	private final BlockPos center;
	private final BlockPos boundsMin;
	private final BlockPos boundsMax;
	private final BlockPos plotBoundsMin;
	private final BlockPos plotBoundsMax;
	private final int size;
	private final int maxSize;
	private final int plotSize;
	private final String islandType;
	private final BlockPos homeLocation;
	private final List<ClientMemberView> members;
	private final String state;
	private final String createdAt;
	private final String updatedAt;
	private final EntityCounts entities;

	public ClientAdminIslandDetailView(String islandId, UUID ownerUuid, String ownerName, String dimension,
			int gridX, int gridZ, BlockPos center, BlockPos boundsMin, BlockPos boundsMax,
			BlockPos plotBoundsMin, BlockPos plotBoundsMax, int size, int maxSize, int plotSize,
			String islandType, BlockPos homeLocation, List<ClientMemberView> members,
			String state, String createdAt, String updatedAt, EntityCounts entities) {
		this.islandId = islandId;
		this.ownerUuid = ownerUuid;
		this.ownerName = ownerName;
		this.dimension = dimension;
		this.gridX = gridX;
		this.gridZ = gridZ;
		this.center = center;
		this.boundsMin = boundsMin;
		this.boundsMax = boundsMax;
		this.plotBoundsMin = plotBoundsMin;
		this.plotBoundsMax = plotBoundsMax;
		this.size = size;
		this.maxSize = maxSize;
		this.plotSize = plotSize;
		this.islandType = islandType;
		this.homeLocation = homeLocation;
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

	public BlockPos center() {
		return center;
	}

	public BlockPos boundsMin() {
		return boundsMin;
	}

	public BlockPos boundsMax() {
		return boundsMax;
	}

	public BlockPos plotBoundsMin() {
		return plotBoundsMin;
	}

	public BlockPos plotBoundsMax() {
		return plotBoundsMax;
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

	public String islandType() {
		return islandType;
	}

	public BlockPos homeLocation() {
		return homeLocation;
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
