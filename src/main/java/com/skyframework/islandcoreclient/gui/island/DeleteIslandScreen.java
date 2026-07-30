package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Three-layer delete flow, all client-local for now (see the TODOs below):
 * 1. "Solicitar eliminación" -&gt; vanilla {@link ConfirmScreen} (cancel = no-op).
 * 2. Confirming opens a 30s local window showing "Confirmar eliminación"; letting it expire
 *    silently falls back to the initial state (checked every frame in {@link #renderContent}).
 * 3. Confirming that closes the screen. There is no "island deleted" state anywhere yet in
 *    ClientIslandCache, so this block stops at closing the screen — see the TODO on
 *    {@link #simulateIslandDeleteConfirm()}.
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
						simulateIslandDeleteRequest();
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.delete.confirm_title"),
				Text.translatable("islandcoreclient.delete.confirm_message")));
	}

	private void onConfirmClicked() {
		simulateIslandDeleteConfirm();
		this.close();
	}

	// TODO: replace with sending IslandDeleteRequestC2S once IslandCore implements the island
	// deletion protocol; the server should own the 30s confirmation window instead of the
	// client, since a client-only timer can't be trusted (crash/relog would silently reset it).
	private void simulateIslandDeleteRequest() {
		pendingDeletionExpiresAtMillis = System.currentTimeMillis() + PENDING_DELETION_WINDOW_SECONDS * 1000L;
	}

	// TODO: replace with sending IslandDeleteConfirmC2S and awaiting ActionResultS2C once
	// IslandCore implements the island deletion protocol. ClientIslandCache has no "island
	// deleted" state yet (nothing in Blocks A/B introduced one), so this only resets the pending
	// window and closes the screen rather than faking a deleted-island Dashboard.
	private void simulateIslandDeleteConfirm() {
		pendingDeletionExpiresAtMillis = 0L;
	}
}
