package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandUpgradeC2S;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientTeleportType;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class LimitsScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int BAR_WIDTH = 220;
	private static final int BAR_HEIGHT = 14;
	private static final int BUTTON_HEIGHT = 20;

	private static final int BAR_Y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT;
	private static final int BUTTON_Y = BAR_Y + BAR_HEIGHT + 12;
	private static final int COOLDOWN_TEXT_Y = BUTTON_Y + BUTTON_HEIGHT + 12;

	public LimitsScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.limits.title"), parent);
	}

	@Override
	protected void initContent() {
		// TeleportStatusS2C is the only real source for the home cooldown line below — reuses the
		// same status TeleportsScreen fetches, since IslandSnapshotS2C carries no cooldown field.
		ClientPlayNetworking.send(new TeleportStatusRequestC2S());

		boolean atMax = ClientIslandCache.getSize() >= ClientIslandCache.getMaxSize();
		Text buttonLabel = atMax
				? Text.translatable("islandcoreclient.limits.max_reached")
				: Text.translatable("islandcoreclient.limits.upgrade_button");

		ButtonWidget upgradeButton = this.addDrawableChild(ButtonWidget.builder(buttonLabel, button -> onUpgradeClicked())
				.dimensions(CONTENT_X, BUTTON_Y, BAR_WIDTH, BUTTON_HEIGHT)
				.build());
		upgradeButton.active = !atMax;
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		int size = ClientIslandCache.getSize();
		int maxSize = ClientIslandCache.getMaxSize();

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.limits.size_label", size, maxSize), CONTENT_X, TOP_BAR_HEIGHT + 8, 0xFFFFFF);

		float ratio = maxSize > 0 ? Math.min(1f, (float) size / maxSize) : 0f;
		int filledWidth = Math.round(BAR_WIDTH * ratio);
		context.fill(CONTENT_X, BAR_Y, CONTENT_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, 0xFF404040);
		context.fill(CONTENT_X, BAR_Y, CONTENT_X + filledWidth, BAR_Y + BAR_HEIGHT, 0xFF55AA55);
		context.drawBorder(CONTENT_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, 0xFF000000);

		long homeCooldownRemaining = ClientIslandCache.getTeleportState(ClientTeleportType.HOME).getCooldownRemainingSeconds();
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.limits.home_cooldown", homeCooldownRemaining),
				CONTENT_X, COOLDOWN_TEXT_Y, 0xAAAAAA);
	}

	private void onUpgradeClicked() {
		ClientPlayNetworking.send(new IslandUpgradeC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
