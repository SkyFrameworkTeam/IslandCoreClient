package com.skyframework.islandcoreclient.keybind;

import com.skyframework.islandcoreclient.gui.island.DashboardScreen;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

public final class OpenMenuKeybind {
	// Unbound by default (GLFW_KEY_UNKNOWN): players opt in via Controls settings.
	private static final KeyBinding OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.islandcoreclient.open_menu",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"key.category.islandcoreclient"
	));

	private OpenMenuKeybind() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.wasPressed()) {
				openMenu(client);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("islandmenu").executes(context -> {
					// Deferred to the next tick: this callback runs inside ChatScreen's own Enter-key
					// handler, which closes the chat screen (client.setScreen(null)) right after
					// dispatching, which would otherwise wipe out a setScreen call made synchronously
					// here.
					MinecraftClient client = context.getSource().getClient();
					client.execute(() -> openMenu(client));
					return 1;
				})));
	}

	private static void openMenu(MinecraftClient client) {
		client.setScreen(new DashboardScreen());
	}
}
