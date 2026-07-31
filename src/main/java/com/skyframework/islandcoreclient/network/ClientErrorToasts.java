package com.skyframework.islandcoreclient.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

/**
 * Transient error feedback for failed actions, using vanilla's toast system rather than adding
 * a bespoke error-banner widget to every single screen. reasonKey is one of IslandCore's
 * ActionReason constants (translated via "islandcoreclient.reason.&lt;key&gt;") or null for a
 * local timeout (no reply from the server at all).
 */
public final class ClientErrorToasts {
	private ClientErrorToasts() {
	}

	public static void showReason(@Nullable String reasonKey) {
		Text description = reasonKey != null
				? Text.translatable("islandcoreclient.reason." + reasonKey)
				: Text.translatable("islandcoreclient.reason.timeout");
		show(description);
	}

	private static void show(Text description) {
		MinecraftClient client = MinecraftClient.getInstance();
		SystemToast.show(client.getToastManager(), SystemToast.Type.PACK_LOAD_FAILURE,
				Text.translatable("islandcoreclient.error.toast_title"), description);
	}
}
