package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ToggleRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandSettingsUpdateC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientIslandSettingView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SettingsScreen extends BaseMenuScreen {
	private static final int ROW_WIDTH = 220;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int FIRST_ROW_Y = TOP_BAR_HEIGHT + 20;

	public SettingsScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.settings.title"), parent);
	}

	@Override
	protected void initContent() {
		boolean owner = ClientIslandCache.isOwner();
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = FIRST_ROW_Y;

		for (ClientIslandSettingView setting : ClientIslandCache.getSettings()) {
			this.addDrawableChild(new ToggleRow(
					x, y, ROW_WIDTH, ROW_HEIGHT,
					setting.label(), setting.value(), owner,
					newValue -> onSettingToggled(setting.key(), newValue)));
			y += ROW_HEIGHT + ROW_SPACING;
		}
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ToggleRow already flipped itself optimistically before this runs. On failure, flip the
	// cached value back and rebuild the screen so the row reflects the real (unchanged) state.
	private void onSettingToggled(String key, boolean newValue) {
		ClientIslandCache.updateSetting(key, newValue);
		ClientPlayNetworking.send(new IslandSettingsUpdateC2S(key, newValue));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientIslandCache.updateSetting(key, !newValue);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}
}
