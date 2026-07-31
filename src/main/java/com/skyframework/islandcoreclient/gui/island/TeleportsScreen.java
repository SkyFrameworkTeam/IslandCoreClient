package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.teleport.TeleportRequestC2S;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientTeleportState;
import com.skyframework.islandcoreclient.state.ClientTeleportType;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TeleportsScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int ROW_HEIGHT = 20;
	private static final int REASON_LINE_HEIGHT = 11;
	private static final int ROW_GAP = 6;
	private static final int BUTTON_WIDTH = 130;

	public TeleportsScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.teleports.title"), parent);
	}

	@Override
	protected void initContent() {
		ClientPlayNetworking.send(new TeleportStatusRequestC2S());

		int y = TOP_BAR_HEIGHT + 8;
		int buttonX = this.width - 16 - BUTTON_WIDTH;

		for (ClientTeleportType type : ClientTeleportType.values()) {
			ClientTeleportState state = ClientIslandCache.getTeleportState(type);

			Text label;
			boolean active;
			if (!state.isEnabled()) {
				label = Text.literal("-");
				active = false;
			} else if (state.getCooldownRemainingSeconds() > 0) {
				long remaining = state.getCooldownRemainingSeconds();
				label = Text.literal(String.format("%02d:%02d", remaining / 60, remaining % 60));
				active = false;
			} else {
				label = Text.translatable("islandcoreclient.teleports.teleport_button");
				active = true;
			}

			ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(label, b -> onTeleportClicked(type))
					.dimensions(buttonX, y, BUTTON_WIDTH, ROW_HEIGHT)
					.build());
			button.active = active;

			y += ROW_HEIGHT;
			if (!state.isEnabled()) {
				y += REASON_LINE_HEIGHT;
			}
			y += ROW_GAP;
		}
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		int y = TOP_BAR_HEIGHT + 8;

		for (ClientTeleportType type : ClientTeleportType.values()) {
			ClientTeleportState state = ClientIslandCache.getTeleportState(type);

			context.drawTextWithShadow(this.textRenderer, type.label(),
					CONTENT_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			y += ROW_HEIGHT;

			if (!state.isEnabled() && state.reasonKey() != null) {
				context.drawTextWithShadow(this.textRenderer, Text.translatable(state.reasonKey()), CONTENT_X + 8, y, 0xAAAAAA);
			}
			if (!state.isEnabled()) {
				y += REASON_LINE_HEIGHT;
			}
			y += ROW_GAP;
		}
	}

	// Closes immediately per design: the screen must not block player movement while the
	// teleport resolves server-side. HOME/SPAWN/FARMING's warmup progress and completion are
	// communicated via chat messages from TeleportManagerImpl, not this screen — only an
	// immediate rejection (wrong dimension, disabled, cooldown, etc.) surfaces here, as a toast,
	// since by the time it arrives the screen is usually already closed.
	private void onTeleportClicked(ClientTeleportType type) {
		ClientPlayNetworking.send(new TeleportRequestC2S(TeleportRequestC2S.Type.valueOf(type.name())));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
		this.close();
	}
}
