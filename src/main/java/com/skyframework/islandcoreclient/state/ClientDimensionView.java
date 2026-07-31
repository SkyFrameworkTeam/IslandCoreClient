package com.skyframework.islandcoreclient.state;

public final class ClientDimensionView {
	private final String id;
	private final String displayName;
	private final ClientDimensionStyle style;
	private long seed;
	private final String state;

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
}
