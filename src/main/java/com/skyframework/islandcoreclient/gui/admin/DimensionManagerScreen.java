package com.skyframework.islandcoreclient.gui.admin;

import java.util.Random;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ToggleRow;
import com.skyframework.islandcoreclient.state.ClientDimensionStyle;
import com.skyframework.islandcoreclient.state.ClientDimensionView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * List / detail / create-form all live in this one screen, switched by {@link #mode}, same
 * "reuse the frame, swap the widgets" idea as the Dashboard's Admin tab.
 */
public class DimensionManagerScreen extends BaseMenuScreen {
	private enum Mode {
		LIST,
		DETAIL,
		CREATE
	}

	private enum PendingAction {
		DELETE,
		REGENERATE
	}

	private static final long PENDING_ACTION_WINDOW_SECONDS = 30L;
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int LIST_START_Y = TOP_BAR_HEIGHT + 8;
	private static final int SUB_BACK_Y = TOP_BAR_HEIGHT + 6;

	private Mode mode = Mode.LIST;
	@Nullable
	private String selectedDimensionId;

	@Nullable
	private PendingAction pendingAction;
	private long pendingActionExpiresAtMillis = 0L;

	private TextFieldWidget idField;
	private TextFieldWidget nameField;
	private TextFieldWidget seedField;
	private ClientDimensionStyle selectedStyle = ClientDimensionStyle.OVERWORLD_LIKE;
	private boolean randomSeed = true;
	@Nullable
	private Text createError;

	public DimensionManagerScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.dimension_manager.title"), parent);
	}

	@Override
	protected void initContent() {
		switch (this.mode) {
			case LIST -> initListContent();
			case DETAIL -> initDetailContent();
			case CREATE -> initCreateContent();
		}
	}

	private void initListContent() {
		int y = LIST_START_Y;
		for (ClientDimensionView dimension : ClientIslandCache.getDimensions()) {
			this.addDrawableChild(ButtonWidget.builder(
							Text.literal(dimension.displayName() + " (" + dimension.id() + ")"),
							button -> {
								this.selectedDimensionId = dimension.id();
								this.mode = Mode.DETAIL;
								this.clearAndInit();
							})
					.dimensions(CONTENT_X, y, 360, ROW_HEIGHT)
					.build());
			y += ROW_HEIGHT + ROW_GAP;
		}

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.create_button"),
						button -> {
							this.createError = null;
							this.mode = Mode.CREATE;
							this.clearAndInit();
						})
				.dimensions(CONTENT_X, y + 8, 220, ROW_HEIGHT)
				.build());
	}

	private void addBackToListButton() {
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.back_to_list"),
						button -> {
							this.mode = Mode.LIST;
							this.clearAndInit();
						})
				.dimensions(CONTENT_X, SUB_BACK_Y, 120, 16)
				.build());
	}

	private void initDetailContent() {
		addBackToListButton();

		ClientDimensionView dimension = getSelectedDimension();
		if (dimension == null) {
			return;
		}

		boolean pending = this.pendingAction != null;
		int buttonY = SUB_BACK_Y + 20 + LINE_HEIGHT * 4 + 16;

		ButtonWidget regenerateButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.regenerate_button"),
						button -> onRegenerateClicked(dimension))
				.dimensions(CONTENT_X, buttonY, 150, ROW_HEIGHT)
				.build());
		ButtonWidget deleteButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.delete_button").formatted(Formatting.RED),
						button -> onDeleteClicked(dimension))
				.dimensions(CONTENT_X + 154, buttonY, 150, ROW_HEIGHT)
				.build());
		regenerateButton.visible = !pending;
		regenerateButton.active = !pending;
		deleteButton.visible = !pending;
		deleteButton.active = !pending;

		ButtonWidget confirmButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.confirm_action_button"),
						button -> onConfirmActionClicked())
				.dimensions(CONTENT_X, buttonY, 200, ROW_HEIGHT)
				.build());
		confirmButton.visible = pending;
		confirmButton.active = pending;
	}

	private void initCreateContent() {
		addBackToListButton();

		int y = SUB_BACK_Y + 24;
		this.idField = new TextFieldWidget(this.textRenderer, CONTENT_X, y, 200, 20,
				Text.translatable("islandcoreclient.admin.dimension_manager.id_field"));
		this.idField.setPlaceholder(Text.translatable("islandcoreclient.admin.dimension_manager.id_field"));
		this.idField.setMaxLength(48);
		this.addDrawableChild(this.idField);

		y += 28;
		this.nameField = new TextFieldWidget(this.textRenderer, CONTENT_X, y, 200, 20,
				Text.translatable("islandcoreclient.admin.dimension_manager.name_field"));
		this.nameField.setPlaceholder(Text.translatable("islandcoreclient.admin.dimension_manager.name_field"));
		this.nameField.setMaxLength(48);
		this.addDrawableChild(this.nameField);

		y += 32;
		for (ClientDimensionStyle style : ClientDimensionStyle.values()) {
			Text label = style == this.selectedStyle ? Text.literal("✓ ").append(style.label()) : style.label();
			this.addDrawableChild(ButtonWidget.builder(label, button -> {
						this.selectedStyle = style;
						this.clearAndInit();
					})
					.dimensions(CONTENT_X, y, 150, 20)
					.build());
			y += 24;
		}

		y += 4;
		this.addDrawableChild(new ToggleRow(CONTENT_X, y, 150, 20,
				Text.translatable("islandcoreclient.admin.dimension_manager.random_seed"), this.randomSeed, true,
				newValue -> {
					this.randomSeed = newValue;
					this.seedField.setEditable(!newValue);
					this.seedField.active = !newValue;
				}));

		this.seedField = new TextFieldWidget(this.textRenderer, CONTENT_X + 154, y, 150, 20,
				Text.translatable("islandcoreclient.admin.dimension_manager.seed_field"));
		this.seedField.setTextPredicate(text -> text.isEmpty() || text.matches("-?[0-9]{1,19}"));
		this.seedField.setEditable(!this.randomSeed);
		this.seedField.active = !this.randomSeed;
		this.addDrawableChild(this.seedField);

		y += 28;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dimension_manager.confirm_create_button"),
						button -> onCreateClicked())
				.dimensions(CONTENT_X, y, 150, 20)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		switch (this.mode) {
			case LIST -> renderListContent(context);
			case DETAIL -> renderDetailContent(context);
			case CREATE -> renderCreateContent(context);
		}
	}

	private void renderListContent(DrawContext context) {
		if (ClientIslandCache.getDimensions().isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.dimension_manager.empty"), CONTENT_X, LIST_START_Y, 0xAAAAAA);
		}
	}

	private void renderDetailContent(DrawContext context) {
		if (this.pendingActionExpiresAtMillis > 0 && System.currentTimeMillis() >= this.pendingActionExpiresAtMillis) {
			this.pendingAction = null;
			this.pendingActionExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		ClientDimensionView dimension = getSelectedDimension();
		if (dimension == null) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.dimension_manager.not_found"), CONTENT_X, SUB_BACK_Y + 24, 0xAAAAAA);
			return;
		}

		int x = CONTENT_X;
		int y = SUB_BACK_Y + 24;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.dimension_manager.detail_id", dimension.id()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.dimension_manager.detail_name", dimension.displayName()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.dimension_manager.detail_style", dimension.style().label()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.dimension_manager.detail_seed", dimension.seed()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT + 16;

		if (this.pendingAction != null) {
			long remaining = Math.max(0L, (this.pendingActionExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			String actionKey = this.pendingAction == PendingAction.DELETE
					? "islandcoreclient.admin.dimension_manager.pending_delete"
					: "islandcoreclient.admin.dimension_manager.pending_regenerate";
			context.drawTextWithShadow(this.textRenderer, Text.translatable(actionKey, remaining), x, y, 0xFFCC55);
		}
	}

	private void renderCreateContent(DrawContext context) {
		if (this.createError != null) {
			context.drawTextWithShadow(this.textRenderer, this.createError.copy().formatted(Formatting.RED),
					CONTENT_X, this.height - 28, 0xFFFFFF);
		}
	}

	@Nullable
	private ClientDimensionView getSelectedDimension() {
		for (ClientDimensionView dimension : ClientIslandCache.getDimensions()) {
			if (dimension.id().equals(this.selectedDimensionId)) {
				return dimension;
			}
		}
		return null;
	}

	private void onRegenerateClicked(ClientDimensionView dimension) {
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						simulateDimensionActionRequest(PendingAction.REGENERATE);
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.admin.dimension_manager.confirm_title"),
				Text.translatable("islandcoreclient.admin.dimension_manager.confirm_regenerate_message",
						Text.literal(dimension.displayName()).formatted(Formatting.BOLD))));
	}

	private void onDeleteClicked(ClientDimensionView dimension) {
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						simulateDimensionActionRequest(PendingAction.DELETE);
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.admin.dimension_manager.confirm_title"),
				Text.translatable("islandcoreclient.admin.dimension_manager.confirm_delete_message",
						Text.literal(dimension.displayName()).formatted(Formatting.BOLD))));
	}

	private void onConfirmActionClicked() {
		ClientDimensionView dimension = getSelectedDimension();
		if (dimension != null && this.pendingAction == PendingAction.DELETE) {
			simulateDimensionDeleteConfirm(dimension.id());
			this.mode = Mode.LIST;
		} else if (dimension != null && this.pendingAction == PendingAction.REGENERATE) {
			simulateDimensionRegenerateConfirm(dimension);
		}
		this.pendingAction = null;
		this.pendingActionExpiresAtMillis = 0L;
		this.clearAndInit();
	}

	private void onCreateClicked() {
		String id = this.idField.getText().trim();
		String name = this.nameField.getText().trim();

		if (id.isEmpty() || !id.matches("[a-z0-9_]+")) {
			this.createError = Text.translatable("islandcoreclient.admin.dimension_manager.error_invalid_id");
			return;
		}
		if (name.isEmpty()) {
			this.createError = Text.translatable("islandcoreclient.admin.dimension_manager.error_invalid_name");
			return;
		}

		Long seed = this.randomSeed ? null : parseSeed(this.seedField.getText());
		simulateDimensionCreate(id, name, this.selectedStyle, seed);
		this.mode = Mode.LIST;
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

	// TODO: replace with sending DimensionRegenerateC2S / DimensionDeleteC2S (request phase) once
	// IslandCore implements the admin protocol; the server should own the 30s confirmation window,
	// same reasoning as the other 3-layer flows in this project.
	private void simulateDimensionActionRequest(PendingAction action) {
		this.pendingAction = action;
		this.pendingActionExpiresAtMillis = System.currentTimeMillis() + PENDING_ACTION_WINDOW_SECONDS * 1000L;
	}

	// TODO: replace with sending DimensionDeleteC2S (confirm phase) and awaiting ActionResultS2C
	// once IslandCore implements the admin protocol.
	private static void simulateDimensionDeleteConfirm(String id) {
		ClientIslandCache.removeDimension(id);
	}

	// TODO: replace with sending DimensionRegenerateC2S (confirm phase) and awaiting
	// ActionResultS2C once IslandCore implements the admin protocol.
	private static void simulateDimensionRegenerateConfirm(ClientDimensionView dimension) {
		dimension.setSeed(new Random().nextLong());
	}

	// TODO: replace with sending DimensionCreateC2S and awaiting ActionResultS2C once IslandCore
	// implements the admin protocol.
	private static void simulateDimensionCreate(String id, String name, ClientDimensionStyle style, @Nullable Long seed) {
		long resolvedSeed = seed != null ? seed : new Random().nextLong();
		ClientIslandCache.addDimension(new ClientDimensionView("islandcore:" + id, name, style, resolvedSeed, "ACTIVE"));
	}
}
