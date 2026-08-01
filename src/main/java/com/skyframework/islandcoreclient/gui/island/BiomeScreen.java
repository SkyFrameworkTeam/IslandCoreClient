package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandBiomeChangeC2S;
import com.skyframework.islandcoreclient.state.ClientBiomeTierView;
import com.skyframework.islandcoreclient.state.ClientBiomeView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BiomeScreen extends BaseMenuScreen {
	// Neither IslandSnapshotS2C nor BiomeTiersS2C exposes the island's current biome-change
	// cooldown remaining, or which biome is currently applied — see ClientIslandCache's notes.
	// This local constant is only the fallback used to start an optimistic cooldown after a
	// change THIS client just made; it does not reflect server truth on relog (matches
	// IslandActionService's own default of 7 days, DEFAULT_BIOME_COOLDOWN_SECONDS server-side).
	private static final long BIOME_CHANGE_COOLDOWN_SECONDS = 604800L;

	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int SECTION_GAP = 10;
	private static final int BIOME_BUTTON_WIDTH = 110;
	private static final int BIOME_BUTTON_HEIGHT = 20;
	private static final int BIOME_BUTTON_GAP = 6;

	public BiomeScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.biome.title"), parent);
	}

	// Called by ClientPacketHandlers when a fresh BiomeTiersS2C lands while this screen is open —
	// Screen#clearAndInit() itself is protected, so this is the public door into it. Same fix as
	// TeleportsScreen#refreshFromNetwork: initContent() builds its buttons synchronously from
	// whatever was already cached, which on the very first visit this session is the empty
	// placeholder (this reply hasn't landed yet).
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		ClientPlayNetworking.send(new BiomeTiersRequestC2S());

		boolean cooldownActive = ClientIslandCache.getBiomeCooldownRemainingSeconds() > 0;
		String currentBiomeId = ClientIslandCache.getCurrentBiomeId();

		int y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT + SECTION_GAP;
		for (ClientBiomeTierView tier : ClientIslandCache.getBiomeTiers()) {
			y += LINE_HEIGHT + 4;
			int x = CONTENT_X;
			for (ClientBiomeView biome : tier.biomes()) {
				boolean isCurrent = biome.biomeId().equals(currentBiomeId);
				Text label = isCurrent ? Text.literal("✓ ").append(biome.label()) : biome.label();

				ButtonWidget.Builder builder = ButtonWidget.builder(label, button -> onBiomeClicked(biome))
						.dimensions(x, y, BIOME_BUTTON_WIDTH, BIOME_BUTTON_HEIGHT);
				if (tier.permissionLabel() != null) {
					builder = builder.tooltip(Tooltip.of(tier.permissionLabel()));
				}
				ButtonWidget button = this.addDrawableChild(builder.build());
				button.active = tier.unlocked() && !cooldownActive;

				x += BIOME_BUTTON_WIDTH + BIOME_BUTTON_GAP;
			}
			y += BIOME_BUTTON_HEIGHT + SECTION_GAP;
		}
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		long remaining = ClientIslandCache.getBiomeCooldownRemainingSeconds();
		Text cooldownText = remaining > 0
				? Text.translatable("islandcoreclient.biome.cooldown_active", formatCooldown(remaining))
				: Text.translatable("islandcoreclient.biome.available_now");

		int y = TOP_BAR_HEIGHT + 8;
		context.drawTextWithShadow(this.textRenderer, cooldownText, CONTENT_X, y, 0xFFFFFF);
		y += LINE_HEIGHT + SECTION_GAP;

		for (ClientBiomeTierView tier : ClientIslandCache.getBiomeTiers()) {
			Text title = Text.translatable("islandcoreclient.biome.tier." + tier.tierId());
			if (!tier.unlocked()) {
				title = title.copy().append(" ").append(Text.translatable("islandcoreclient.biome.locked_suffix").formatted(Formatting.RED));
			}
			context.drawTextWithShadow(this.textRenderer, title, CONTENT_X, y, 0xFFFFFF);
			y += LINE_HEIGHT + 4 + BIOME_BUTTON_HEIGHT + SECTION_GAP;
		}
	}

	private void onBiomeClicked(ClientBiomeView biome) {
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						requestBiomeChange(biome.biomeId());
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.biome.confirm_title"),
				Text.translatable("islandcoreclient.biome.confirm_message", biome.label())));
	}

	private void requestBiomeChange(String biomeId) {
		ClientPlayNetworking.send(new IslandBiomeChangeC2S(biomeId));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// Optimistic only — see the class-level note on BIOME_CHANGE_COOLDOWN_SECONDS,
				// neither field has a real server source yet.
				ClientIslandCache.setCurrentBiomeId(biomeId);
				ClientIslandCache.startBiomeCooldown(BIOME_CHANGE_COOLDOWN_SECONDS);
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private static String formatCooldown(long totalSeconds) {
		long days = totalSeconds / 86400L;
		long hours = (totalSeconds % 86400L) / 3600L;
		if (days > 0) {
			return days + "d " + hours + "h";
		}
		long minutes = (totalSeconds % 3600L) / 60L;
		if (hours > 0) {
			return hours + "h " + minutes + "m";
		}
		return minutes + "m";
	}
}
