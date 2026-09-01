package com.skyframework.islandcoreclient.keybind;

import com.skyframework.islandcoreclient.gui.party.PartyScreen;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

// Mirrors OpenMenuKeybind exactly, but opens PartyScreen directly instead of the Dashboard — a
// party is independent of owning an island, so it gets its own shortcut rather than only being
// reachable through the island menu. Bound to P by default (unlike OpenMenuKeybind, which ships
// unbound): players can still rebind or unbind it in Controls like any other key.
public final class OpenPartyKeybind {
	private static final KeyBinding OPEN_PARTY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.islandcoreclient.open_party",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			"key.category.islandcoreclient"
	));

	private OpenPartyKeybind() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_PARTY_KEY.wasPressed()) {
				openParty(client);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("islandparty").executes(context -> {
					MinecraftClient client = context.getSource().getClient();
					// Deferred to the next client tick via MinecraftClient#execute: this command's
					// executes() callback runs SYNCHRONOUSLY inside ChatScreen's own Enter-key
					// handler, which — after dispatching the typed command/message — unconditionally
					// closes the chat screen with its own client.setScreen(null) call. Calling
					// setScreen(new PartyScreen(...)) directly here would open the screen for one
					// frame and then have it immediately wiped out by that follow-up call, which is
					// exactly what "/islandparty no hace nada" looks like from the player's side (see
					// the Bloque D / punto 13 investigation). Scheduling the real setScreen for the
					// next tick lets ChatScreen finish closing itself first, so PartyScreen is what's
					// actually on screen afterward. The P keybind path below isn't affected — it never
					// runs while a chat/text screen has input focus, so nothing schedules a
					// setScreen(null) after it.
					client.execute(() -> openParty(client));
					return 1;
				})));
	}

	private static void openParty(MinecraftClient client) {
		// Only meaningful once a screen can legitimately be opened at all — same guard implicit in
		// OpenMenuKeybind (wasPressed() only fires during normal gameplay, never while another
		// screen/GUI already has input focus, matching vanilla's own key-binding behavior).
		client.setScreen(new PartyScreen(client.currentScreen));
	}
}
