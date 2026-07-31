package com.skyframework.islandcoreclient.state;

import org.jetbrains.annotations.Nullable;

public final class ClientVanillaResetState {
	public enum SeedMode {
		RANDOM,
		KEEP,
		SPECIFIED
	}

	private boolean pending;
	@Nullable
	private SeedMode seedMode;
	@Nullable
	private Long seedValue;

	public ClientVanillaResetState(boolean pending, @Nullable SeedMode seedMode, @Nullable Long seedValue) {
		this.pending = pending;
		this.seedMode = seedMode;
		this.seedValue = seedValue;
	}

	public boolean isPending() {
		return pending;
	}

	@Nullable
	public SeedMode seedMode() {
		return seedMode;
	}

	@Nullable
	public Long seedValue() {
		return seedValue;
	}

	public void queue(SeedMode seedMode, @Nullable Long seedValue) {
		this.pending = true;
		this.seedMode = seedMode;
		this.seedValue = seedValue;
	}

	public void cancel() {
		this.pending = false;
		this.seedMode = null;
		this.seedValue = null;
	}
}
