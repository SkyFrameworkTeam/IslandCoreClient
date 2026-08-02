package com.skyframework.islandcoreclient.state;

import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;

public final class ClientDimensionView {
	private final String id;
	private final String displayName;
	private final ClientDimensionStyle style;
	private long seed;
	private final String state;

	// Only DimensionDetailS2C carries these (DimensionListS2C's DimensionEntry doesn't) — null
	// until the detail screen actually fetches this dimension at least once this session.
	@Nullable
	private String createdAt;
	@Nullable
	private String updatedAt;

	public ClientDimensionView(String id, String displayName, ClientDimensionStyle style, long seed, String state) {
		this.id = id;
		this.displayName = displayName;
		this.style = style;
		this.seed = seed;
		this.state = state;
	}

	public String id() {
		return id;
	}

	// DimensionDetailRequestC2S/DimensionDeleteC2S/DimensionRegenerateC2S all take the dimension's
	// PATH ONLY (e.g. "foo" for "islandcore:foo") — the server builds the full Identifier itself —
	// while id() above carries the full identifier string DimensionListS2C sends. This is the one
	// place that conversion happens, so screens never re-derive it ad hoc.
	public String path() {
		return Identifier.of(id).getPath();
	}

	public String displayName() {
		return displayName;
	}

	public ClientDimensionStyle style() {
		return style;
	}

	public long seed() {
		return seed;
	}

	public String state() {
		return state;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Nullable
	public String createdAt() {
		return createdAt;
	}

	@Nullable
	public String updatedAt() {
		return updatedAt;
	}

	public void applyDetail(String createdAt, String updatedAt) {
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}
