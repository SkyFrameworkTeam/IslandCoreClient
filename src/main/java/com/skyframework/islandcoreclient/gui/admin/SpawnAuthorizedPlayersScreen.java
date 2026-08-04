package com.skyframework.islandcoreclient.gui.admin;

import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnAuthorizedPlayerRemoveC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnBuildProtectionStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Full-page list of the Spawn island's authorized (always-can-build) players, split out of
 * {@link SpawnManagerScreen} for the same reason {@link AdminIslandMembersScreen} was split out of
 * {@link AdminIslandDetailScreen}: a real full page has all the room a list could ever need, no
 * embedded scrollable/boxed panel required. Reads straight from
 * {@link ClientIslandCache#getSpawnAuthorizedPlayers()} — no separate network request, same data
 * {@link SpawnManagerScreen} already has cached. Adding a new authorized player stays on
 * {@link SpawnManagerScreen} itself (a quick action that doesn't need a screen switch); only
 * viewing/removing from the full list lives here.
 */
public class SpawnAuthorizedPlayersScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int LIST_START_Y = TOP_BAR_HEIGHT + 8;

	public SpawnAuthorizedPlayersScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.spawn_authorized.title"), parent);
	}

	// Called by ClientPacketHandlers when a fresh SpawnBuildProtectionStatusS2C lands while this
	// screen is open — same pattern as every other Admin screen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		int rowY = LIST_START_Y;
		int actionsX = this.width - 16 - ACTION_BUTTON_WIDTH;
		for (ClientMemberView authorized : ClientIslandCache.getSpawnAuthorizedPlayers()) {
			int buttonY = rowY + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
			this.addDrawableChild(ButtonWidget.builder(
							Text.translatable("islandcoreclient.admin.spawn.authorized_remove"),
							button -> onRemoveClicked(authorized.uuid()))
					.dimensions(actionsX, buttonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
					.build());
			rowY += ROW_HEIGHT + ROW_GAP;
		}
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		List<ClientMemberView> authorizedPlayers = ClientIslandCache.getSpawnAuthorizedPlayers();
		if (authorizedPlayers.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.spawn_authorized.empty"), CONTENT_X, LIST_START_Y, 0xAAAAAA);
			return;
		}

		int y = LIST_START_Y;
		for (ClientMemberView authorized : authorizedPlayers) {
			Text line = Text.literal(authorized.name() + " ").append(authorized.role().label());
			context.drawTextWithShadow(this.textRenderer, line, CONTENT_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			y += ROW_HEIGHT + ROW_GAP;
		}
	}

	private void onRemoveClicked(UUID targetUuid) {
		ClientPlayNetworking.send(new SpawnAuthorizedPlayerRemoveC2S(targetUuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnBuildProtectionStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
