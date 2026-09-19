package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandBiomeChangeC2S;
import com.skyframework.islandcoreclient.state.ClientBiomeTierView;
import com.skyframework.islandcoreclient.state.ClientBiomeView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

// TODO (future sprint, not yet implemented): add a non-clickable (ⓘ) info icon next to each locked
// tier that explains how to unlock it on hover (e.g. "pídeselo a un administrador"), separate from
// the existing per-button Tooltip that just names the missing permission.
public class BiomeScreen extends BaseMenuScreen {
	// Neither IslandSnapshotS2C nor BiomeTiersS2C exposes the island's current biome-change
	// cooldown remaining, or which biome is currently applied — see ClientIslandCache's notes.
	// This local constant is only the fallback used to start an optimistic cooldown after a
	// change THIS client just made; it does not reflect server truth on relog (matches
	// IslandActionService's own default of 7 days, DEFAULT_BIOME_COOLDOWN_SECONDS server-side).
	private static final long BIOME_CHANGE_COOLDOWN_SECONDS = 604800L;

	private static final int CONTENT_X = 16;
	private static final int CONTENT_RIGHT_MARGIN = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int SECTION_GAP = 10;
	// Buttons are sized dynamically per tier between these two bounds (see buttonWidthForTier):
	// as wide as MAX when a row comfortably fits, shrinking toward MIN before ever wrapping.
	private static final int BIOME_BUTTON_MIN_WIDTH = 90;
	private static final int BIOME_BUTTON_MAX_WIDTH = 110;
	private static final int BIOME_BUTTON_HEIGHT = 20;
	private static final int BIOME_BUTTON_GAP = 6;

	public BiomeScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.biome.title"), parent);
		// Sent once here, NOT from initContent(): initContent() reruns on every clearAndInit(),
		// including the one refreshFromNetwork() below does when the reply to THIS exact request
		// lands — sending it from initContent() turned that into a self-perpetuating
		// request/rebuild loop (a fresh BiomeTiersS2C arriving, rebuilding all buttons — including
		// each locked tier's Tooltip, resetting its hover timer — which sent another request, ad
		// infinitum), which is what made the locked-tier tooltip flicker nonstop instead of holding
		// steady. Same one-shot-in-constructor pattern AllianceScreen already uses.
		ClientPlayNetworking.send(new BiomeTiersRequestC2S());
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
		boolean cooldownActive = ClientIslandCache.getBiomeCooldownRemainingSeconds() > 0;
		String currentBiomeId = ClientIslandCache.getCurrentBiomeId();

		int availableWidth = this.width - CONTENT_X - CONTENT_RIGHT_MARGIN;
		int maxColumns = computeMaxColumns(availableWidth);

		int y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT + SECTION_GAP;
		for (ClientBiomeTierView tier : ClientIslandCache.getBiomeTiers()) {
			y += LINE_HEIGHT + 4;

			List<ClientBiomeView> biomes = tier.biomes();
			int columns = columnsForTier(biomes, maxColumns);
			int buttonWidth = buttonWidthForTier(availableWidth, columns);

			for (int i = 0; i < biomes.size(); i++) {
				ClientBiomeView biome = biomes.get(i);
				boolean isCurrent = biome.biomeId().equals(currentBiomeId);
				Text label = isCurrent ? Text.literal("✓ ").append(biome.label()) : biome.label();

				int col = i % columns;
				int row = i / columns;
				int x = CONTENT_X + col * (buttonWidth + BIOME_BUTTON_GAP);
				int buttonY = y + row * (BIOME_BUTTON_HEIGHT + BIOME_BUTTON_GAP);

				ButtonWidget button;
				if (isCurrent) {
					// Bold + underlined, on top of the permanently-"pressed" look below — the check
					// prefix alone read as too subtle to spot at a glance among a full grid of buttons.
					Text selectedLabel = label.copy().formatted(Formatting.BOLD, Formatting.UNDERLINE);
					button = new SelectedBiomeButton(x, buttonY, buttonWidth, BIOME_BUTTON_HEIGHT, selectedLabel,
							b -> onBiomeClicked(biome));
					if (tier.permissionLabel() != null) {
						button.setTooltip(Tooltip.of(tier.permissionLabel()));
					}
					this.addDrawableChild(button);
				} else {
					ButtonWidget.Builder builder = ButtonWidget.builder(label, b -> onBiomeClicked(biome))
							.dimensions(x, buttonY, buttonWidth, BIOME_BUTTON_HEIGHT);
					if (tier.permissionLabel() != null) {
						builder = builder.tooltip(Tooltip.of(tier.permissionLabel()));
					}
					button = this.addDrawableChild(builder.build());
				}
				button.active = tier.unlocked() && !cooldownActive;
			}

			int rows = rowsForTier(biomes, columns);
			y += rows * BIOME_BUTTON_HEIGHT + (rows - 1) * BIOME_BUTTON_GAP + SECTION_GAP;
		}
	}

	// Max columns a row can hold at all, using the narrowest reasonable button width — this is
	// what decides whether a tier needs to wrap into multiple rows instead of overflowing the
	// screen. Individual tiers with fewer biomes than this use fewer, wider columns instead (see
	// columnsForTier/buttonWidthForTier), so a 3-biome tier isn't stretched into 6 slots.
	private static int computeMaxColumns(int availableWidth) {
		return Math.max(1, (availableWidth + BIOME_BUTTON_GAP) / (BIOME_BUTTON_MIN_WIDTH + BIOME_BUTTON_GAP));
	}

	private static int columnsForTier(List<ClientBiomeView> biomes, int maxColumns) {
		return Math.max(1, Math.min(biomes.size(), maxColumns));
	}

	private static int rowsForTier(List<ClientBiomeView> biomes, int columns) {
		return biomes.isEmpty() ? 1 : (biomes.size() + columns - 1) / columns;
	}

	private static int buttonWidthForTier(int availableWidth, int columns) {
		int width = (availableWidth - (columns - 1) * BIOME_BUTTON_GAP) / columns;
		return Math.min(BIOME_BUTTON_MAX_WIDTH, width);
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		long remaining = ClientIslandCache.getBiomeCooldownRemainingSeconds();
		Text cooldownText = remaining > 0
				? Text.translatable("islandcoreclient.biome.cooldown_active", formatCooldown(remaining))
				: Text.translatable("islandcoreclient.biome.available_now");

		int availableWidth = this.width - CONTENT_X - CONTENT_RIGHT_MARGIN;
		int maxColumns = computeMaxColumns(availableWidth);

		int y = TOP_BAR_HEIGHT + 8;
		context.drawTextWithShadow(this.textRenderer, cooldownText, CONTENT_X, y, 0xFFFFFF);
		y += LINE_HEIGHT + SECTION_GAP;

		for (ClientBiomeTierView tier : ClientIslandCache.getBiomeTiers()) {
			Text title = Text.translatable("islandcoreclient.biome.tier." + tier.tierId());
			if (!tier.unlocked()) {
				title = title.copy().append(" ").append(Text.translatable("islandcoreclient.biome.locked_suffix").formatted(Formatting.RED));
			}
			context.drawTextWithShadow(this.textRenderer, title, CONTENT_X, y, 0xFFFFFF);

			int columns = columnsForTier(tier.biomes(), maxColumns);
			int rows = rowsForTier(tier.biomes(), columns);
			y += LINE_HEIGHT + 4 + rows * BIOME_BUTTON_HEIGHT + (rows - 1) * BIOME_BUTTON_GAP + SECTION_GAP;
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

	// Renders permanently in vanilla's own "highlighted/pressed" button texture — the same
	// widget_highlighted look ButtonWidget already uses on hover (PressableWidget#renderWidget
	// keys it off isSelected()) — repurposed here as a standing "this one is active" marker for
	// the current biome, instead of inventing a new visual not already used elsewhere.
	private static final class SelectedBiomeButton extends ButtonWidget {
		private SelectedBiomeButton(int x, int y, int width, int height, Text message, PressAction onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
		}

		@Override
		public boolean isSelected() {
			return true;
		}
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
