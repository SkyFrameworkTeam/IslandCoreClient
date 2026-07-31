package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandDeleteConfirmC2S;
import com.skyframework.islandcoreclient.network.island.IslandDeleteRequestC2S;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Three-layer delete flow:
 * 1. "Solicitar eliminación" -&gt; vanilla {@link ConfirmScreen} (cancel = no-op) -&gt; real
 *    {@link IslandDeleteRequestC2S}.
 * 2. On success, opens a 30s LOCAL window mirroring IslandDeletionServiceImpl's own 30s server
 *    window (REQUEST_TIMEOUT) showing "Confirmar eliminación"; letting it expire silently falls
 *    back to the initial state (checked every frame in {@link #renderContent}). This countdown
 *    is client-tracked, not server-pushed: PendingConfirmationTickS2C exists server-side but
 *    isn't wired to broadcast yet, so there is no live tick to follow — if it drifts from the
 *    real server deadline, IslandDeleteConfirmC2S will simply come back with NO_PENDING_DELETION
 *    and the failure path below handles that like any other rejection.
 * 3. Confirming sends real {@link IslandDeleteConfirmC2S} and closes the screen on success.
 */
public class DeleteIslandScreen extends BaseMenuScreen {
	private static final long PENDING_DELETION_WINDOW_SECONDS = 30L;
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int BUTTON_Y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT * 2 + 12;

	// 0 = no pending deletion. Screen-local UI flow state, not island data, so it doesn't belong
	// in ClientIslandCache.
	private long pendingDeletionExpiresAtMillis = 0L;

	public DeleteIslandScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.delete.title"), parent);
	}

	@Override
	protected void initContent() {
		boolean pending = pendingDeletionExpiresAtMillis > 0;

		ButtonWidget requestButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.delete.request_button"),
						button -> onRequestClicked())
				.dimensions(CONTENT_X, BUTTON_Y, 200, 20)
				.build());
		requestButton.visible = !pending;
		requestButton.active = !pending;

		ButtonWidget confirmButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.delete.confirm_button"),
						button -> onConfirmClicked())
				.dimensions(CONTENT_X, BUTTON_Y, 200, 20)
				.build());
		confirmButton.visible = pending;
		confirmButton.active = pending;
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (pendingDeletionExpiresAtMillis > 0 && System.currentTimeMillis() >= pendingDeletionExpiresAtMillis) {
			pendingDeletionExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		int y = TOP_BAR_HEIGHT + 8;
		if (pendingDeletionExpiresAtMillis > 0) {
			long remaining = Math.max(0L, (pendingDeletionExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.delete.pending", remaining), CONTENT_X, y, 0xFFCC55);
		} else {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.delete.warning_line1").formatted(Formatting.RED), CONTENT_X, y, 0xFFFFFF);
			y += LINE_HEIGHT;
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.delete.warning_line2"), CONTENT_X, y, 0xAAAAAA);
		}
	}

	private void onRequestClicked() {
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						requestIslandDelete();
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.delete.confirm_title"),
				Text.translatable("islandcoreclient.delete.confirm_message")));
	}

	private void onConfirmClicked() {
		ClientPlayNetworking.send(new IslandDeleteConfirmC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			pendingDeletionExpiresAtMillis = 0L;
			if (!success) {
				ClientErrorToasts.showReason(reasonKey);
			}
			// ClientIslandCache has no "island deleted" state yet (nothing in Blocks A/B
			// introduced one) — the Dashboard picks up the real post-delete state (exists=false)
			// on its own next snapshot refresh when the player returns to it.
			this.close();
		});
	}

	private void requestIslandDelete() {
		ClientPlayNetworking.send(new IslandDeleteRequestC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				pendingDeletionExpiresAtMillis = System.currentTimeMillis() + PENDING_DELETION_WINDOW_SECONDS * 1000L;
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
