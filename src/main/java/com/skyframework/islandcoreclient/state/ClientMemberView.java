package com.skyframework.islandcoreclient.state;

import java.util.UUID;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClientMemberView {
	public enum Role {
		OWNER(Formatting.GOLD),
		// Formerly TRUSTED — renamed server-side to CO_OWNER (hard-coded always-ALLOW, exactly as
		// strong as OWNER). Wire value must match the server's IslandRole#name() exactly.
		CO_OWNER(Formatting.AQUA),
		MEMBER(Formatting.GREEN),
		ALLY(Formatting.YELLOW);

		private final Formatting color;

		Role(Formatting color) {
			this.color = color;
		}

		// Centralized here so every screen that lists members shows the same role colors. CO_OWNER
		// gets a translated display name ("Copropietario") instead of the raw enum name; the other
		// three keep printing their raw name (pre-existing behavior, unchanged).
		public Text label() {
			MutableText text = this == CO_OWNER ? Text.translatable("islandcoreclient.members.role_co_owner") : Text.literal(name());
			return text.formatted(color);
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
