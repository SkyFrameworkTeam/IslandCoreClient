package com.skyframework.islandcoreclient.gui.admin;

import java.util.Locale;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientResetDimension;
import com.skyframework.islandcoreclient.state.ClientVanillaResetState;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * The 3 dimension rows are fixed ({@link ClientResetDimension}), not data-driven. "Encolar
 * reseteo" opens an options step (this same screen, {@link Mode#OPTIONS}) instead of a vanilla
 * {@link net.minecraft.client.gui.screen.ConfirmScreen}, since it needs a seed-mode form rather
 * than a yes/no prompt; from there it follows the same pending-countdown-then-confirm shape as
 * every other 3-layer flow in this project.
 */
public class VanillaResetScreen extends BaseMenuScreen {
	private enum Mode {
		LIST,
		OPTIONS,
		PENDING
	}

	private static final long PENDING_WINDOW_SECONDS = 30L;
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 8;
	private static final int LIST_START_Y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT * 2 + 12;
	private static final int SUB_BACK_Y = TOP_BAR_HEIGHT + 6;
	private static final int OPTIONS_WARNING_Y = SUB_BACK_Y + 22;
	private static final int OPTIONS_FORM_Y = OPTIONS_WARNING_Y + LINE_HEIGHT * 3 + 8;

	private Mode mode = Mode.LIST;
	@Nullable
	private ClientResetDimension selectedDimension;
	private ClientVanillaResetState.SeedMode selectedSeedMode = ClientVanillaResetState.SeedMode.RANDOM;
	private TextFieldWidget seedField;

	// 0 = not counting down. Screen-local: nothing is committed to ClientIslandCache until the
	// Capa 3 confirm, exactly like DeleteIslandScreen.
	private long pendingExpiresAtMillis = 0L;

	public VanillaResetScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.vanilla_reset.title"), parent);
	}

	@Override
	protected void initContent() {
		switch (this.mode) {
			case LIST -> initListContent();
			case OPTIONS -> initOptionsContent();
			case PENDING -> initPendingContent();
		}
	}

	private void initListContent() {
		int y = LIST_START_Y;
		for (ClientResetDimension dimension : ClientResetDimension.values()) {
			ClientVanillaResetState state = ClientIslandCache.getVanillaResetState(dimension);
			if (state.isPending()) {
				this.addDrawableChild(ButtonWidget.builder(
								Text.translatable("islandcoreclient.admin.vanilla_reset.cancel_button"),
								button -> onCancelClicked(dimension))
						.dimensions(CONTENT_X + 200, y, 150, ROW_HEIGHT)
						.build());
			} else {
				this.addDrawableChild(ButtonWidget.builder(
								Text.translatable("islandcoreclient.admin.vanilla_reset.queue_button"),
								button -> onQueueClicked(dimension))
						.dimensions(CONTENT_X + 200, y, 150, ROW_HEIGHT)
						.build());
			}
			y += rowHeight(state) + ROW_GAP;
		}
	}

	private void addBackToListButton() {
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.vanilla_reset.back_to_list"),
						button -> {
							this.mode = Mode.LIST;
							this.pendingExpiresAtMillis = 0L;
							this.clearAndInit();
						})
				.dimensions(CONTENT_X, SUB_BACK_Y, 120, 16)
				.build());
	}

	private void initOptionsContent() {
		addBackToListButton();

		int y = OPTIONS_FORM_Y;
		for (ClientVanillaResetState.SeedMode seedMode : ClientVanillaResetState.SeedMode.values()) {
			Text label = seedMode == this.selectedSeedMode ? Text.literal("✓ ").append(seedModeLabel(seedMode)) : seedModeLabel(seedMode);
			this.addDrawableChild(ButtonWidget.builder(label, button -> {
						this.selectedSeedMode = seedMode;
						this.clearAndInit();
					})
					.dimensions(CONTENT_X, y, 200, 20)
					.build());
			y += 24;
		}

		// Aligned with the SPECIFIED row above (the last one drawn in the loop).
		this.seedField = new TextFieldWidget(this.textRenderer, CONTENT_X + 210, y - 24, 120, 20,
				Text.translatable("islandcoreclient.admin.vanilla_reset.seed_field"));
		this.seedField.setTextPredicate(text -> text.isEmpty() || text.matches("-?[0-9]{1,19}"));
		boolean specified = this.selectedSeedMode == ClientVanillaResetState.SeedMode.SPECIFIED;
		this.seedField.setEditable(specified);
		this.seedField.active = specified;
		this.addDrawableChild(this.seedField);

		y += 16;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.vanilla_reset.continue_button"),
						button -> onContinueClicked())
				.dimensions(CONTENT_X, y, 150, 20)
				.build());
	}

	private void initPendingContent() {
		addBackToListButton();

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.vanilla_reset.confirm_button"),
						button -> onConfirmQueueClicked())
				.dimensions(CONTENT_X, SUB_BACK_Y + 44, 200, 20)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		switch (this.mode) {
			case LIST -> renderListContent(context);
			case OPTIONS -> renderOptionsContent(context);
			case PENDING -> renderPendingContent(context);
		}
	}

	private void renderListContent(DrawContext context) {
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.warning_line1").formatted(Formatting.GOLD),
				CONTENT_X, TOP_BAR_HEIGHT + 8, 0xFFFFFF);
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.warning_line2"),
				CONTENT_X, TOP_BAR_HEIGHT + 8 + LINE_HEIGHT, 0xAAAAAA);

		int y = LIST_START_Y;
		for (ClientResetDimension dimension : ClientResetDimension.values()) {
			ClientVanillaResetState state = ClientIslandCache.getVanillaResetState(dimension);
			context.drawTextWithShadow(this.textRenderer, dimension.label(),
					CONTENT_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			if (state.isPending()) {
				context.drawTextWithShadow(this.textRenderer, seedModeLabel(state.seedMode()), CONTENT_X, y + ROW_HEIGHT + 1, 0xAAAAAA);
			}
			y += rowHeight(state) + ROW_GAP;
		}
	}

	private void renderOptionsContent(DrawContext context) {
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.warning_line1").formatted(Formatting.GOLD),
				CONTENT_X, OPTIONS_WARNING_Y, 0xFFFFFF);
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.warning_line2"),
				CONTENT_X, OPTIONS_WARNING_Y + LINE_HEIGHT, 0xAAAAAA);

		boolean anotherSpecified = false;
		for (ClientResetDimension dimension : ClientResetDimension.values()) {
			if (dimension == this.selectedDimension) {
				continue;
			}
			ClientVanillaResetState state = ClientIslandCache.getVanillaResetState(dimension);
			if (state.isPending() && state.seedMode() == ClientVanillaResetState.SeedMode.SPECIFIED) {
				anotherSpecified = true;
				break;
			}
		}
		if (anotherSpecified) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.vanilla_reset.warning_specified_conflict"),
					CONTENT_X, OPTIONS_WARNING_Y + LINE_HEIGHT * 2, 0xFF5555);
		}
	}

	private void renderPendingContent(DrawContext context) {
		if (this.pendingExpiresAtMillis > 0 && System.currentTimeMillis() >= this.pendingExpiresAtMillis) {
			this.mode = Mode.LIST;
			this.pendingExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		long remaining = Math.max(0L, (this.pendingExpiresAtMillis - System.currentTimeMillis()) / 1000L);
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.evacuate_warning").formatted(Formatting.RED),
				CONTENT_X, SUB_BACK_Y + 22, 0xFFFFFF);
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.vanilla_reset.countdown", remaining),
				CONTENT_X, SUB_BACK_Y + 22 + LINE_HEIGHT + 4, 0xFFCC55);
	}

	private static int rowHeight(ClientVanillaResetState state) {
		return state.isPending() ? ROW_HEIGHT + LINE_HEIGHT + 2 : ROW_HEIGHT;
	}

	private static Text seedModeLabel(ClientVanillaResetState.SeedMode seedMode) {
		return Text.translatable("islandcoreclient.admin.vanilla_reset.seed_mode_" + seedMode.name().toLowerCase(Locale.ROOT));
	}

	private void onQueueClicked(ClientResetDimension dimension) {
		this.selectedDimension = dimension;
		this.selectedSeedMode = ClientVanillaResetState.SeedMode.RANDOM;
		this.mode = Mode.OPTIONS;
		this.clearAndInit();
	}

	private void onContinueClicked() {
		if (this.selectedSeedMode == ClientVanillaResetState.SeedMode.SPECIFIED && parseSeed(this.seedField.getText()) == null) {
			return;
		}
		this.mode = Mode.PENDING;
		this.pendingExpiresAtMillis = System.currentTimeMillis() + PENDING_WINDOW_SECONDS * 1000L;
		this.clearAndInit();
	}

	private void onConfirmQueueClicked() {
		if (this.selectedDimension != null) {
			Long seedValue = this.selectedSeedMode == ClientVanillaResetState.SeedMode.SPECIFIED
					? parseSeed(this.seedField.getText())
					: null;
			simulateVanillaResetConfirm(this.selectedDimension, this.selectedSeedMode, seedValue);
		}
		this.mode = Mode.LIST;
		this.pendingExpiresAtMillis = 0L;
		this.clearAndInit();
	}

	private void onCancelClicked(ClientResetDimension dimension) {
		simulateVanillaResetCancel(dimension);
		this.clearAndInit();
	}

	@Nullable
	private static Long parseSeed(String text) {
		try {
			return Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// TODO: replace with sending VanillaResetQueueC2S (request phase, opening the options form)
	// and VanillaResetConfirmC2S (this method, on the final "Confirmar" click) once IslandCore
	// implements the admin protocol; the server should own the 30s window, same reasoning as
	// every other 3-layer flow in this project.
	private static void simulateVanillaResetConfirm(ClientResetDimension dimension,
			ClientVanillaResetState.SeedMode seedMode, @Nullable Long seedValue) {
		ClientIslandCache.getVanillaResetState(dimension).queue(seedMode, seedValue);
	}

	// TODO: replace with sending VanillaResetCancelC2S and awaiting ActionResultS2C once
	// IslandCore implements the admin protocol.
	private static void simulateVanillaResetCancel(ClientResetDimension dimension) {
		ClientIslandCache.getVanillaResetState(dimension).cancel();
	}
}
