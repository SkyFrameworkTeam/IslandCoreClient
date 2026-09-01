package com.skyframework.islandcoreclient.state;

import java.util.UUID;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClientMemberView {
	public enum Role {
		OWNER(Formatting.GOLD),
		TRUSTED(Formatting.AQUA),
		MEMBER(Formatting.GREEN),
		ALLY(Formatting.YELLOW);

		private final Formatting color;

		Role(Formatting color) {
			this.color = color;
		}

		// Centralized here so every screen that lists members shows the same role colors.
		public Text label() {
			return Text.literal(name()).formatted(color);
		}
	}

	private final UUID uuid;
	private final String name;
	private Role role;

	public ClientMemberView(UUID uuid, String name, Role role) {
		this.uuid = uuid;
		this.name = name;
		this.role = role;
	}

	public UUID uuid() {
		return uuid;
	}

	public String name() {
		return name;
	}

	public Role role() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
