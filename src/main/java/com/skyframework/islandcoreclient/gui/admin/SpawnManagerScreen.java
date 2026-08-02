package com.skyframework.islandcoreclient.gui.admin;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandCreateC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandResizeC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandSetHomeC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Requests the real status once from the constructor; {@link #refreshFromNetwork()} only rebuilds
 * from {@link ClientIslandCache}. {@link SpawnIslandSetHomeC2S} carries NO coordinates — the
 * server reads the sender's actual position and validates it itself (bounds + dimension, same as
 * "/island admin spawn sethome"); this screen must never read/send a local BlockPos for it.
 */
public class SpawnManagerScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int FIELD_WIDTH = 80;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 170;

	private static final int FORM_Y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT + 8;
	private static final int HOME_WARNING_Y = FORM_Y + FIELD_HEIGHT + 16;
	private static final int HOME_BUTTON_Y = HOME_WARNING_Y + LINE_HEIGHT * 2 + 8;

	private TextFieldWidget sizeField;
	private ButtonWidget resizeButton;

	public SpawnManagerScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.spawn.title"), parent);
		ClientPlayNetworking.send(new SpawnStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh SpawnStatusS2C lands while this screen is open —
	// same pattern as TeleportsScreen/BiomeScreen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		if (!ClientIslandCache.spawnExists()) {
			initCreateForm();
		} else {
			initResizeForm();
		}
	}

	private void initCreateForm() {
		this.sizeField = new TextFieldWidget(this.textRenderer, CONTENT_X, FORM_Y, FIELD_WIDTH, FIELD_HEIGHT,
				Text.translatable("islandcoreclient.admin.spawn.size_field"));
		this.sizeField.setText("25");
		this.sizeField.setTextPredicate(text -> text.isEmpty() || text.matches("[0-9]{1,4}"));
		this.addDrawableChild(this.sizeField);

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.create_button"),
						button -> onCreateClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, FORM_Y, BUTTON_WIDTH, FIELD_HEIGHT)
				.build());
	}

	private void initResizeForm() {
		this.sizeField = new TextFieldWidget(this.textRenderer, CONTENT_X, FORM_Y, FIELD_WIDTH, FIELD_HEIGHT,
				Text.translatable("islandcoreclient.admin.spawn.size_field"));
		this.sizeField.setTextPredicate(text -> text.isEmpty() || text.matches("[0-9]{1,4}"));
		this.sizeField.setChangedListener(this::onResizeFieldChanged);
		this.addDrawableChild(this.sizeField);

		this.resizeButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.resize_button"),
						button -> onResizeClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, FORM_Y, BUTTON_WIDTH, FIELD_HEIGHT)
				.build());
		this.resizeButton.active = false;

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.set_home_button"),
						button -> onSetHomeClicked())
				.dimensions(CONTENT_X, HOME_BUTTON_Y, 200, FIELD_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!ClientIslandCache.spawnExists()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.spawn.not_exists"), CONTENT_X, TOP_BAR_HEIGHT + 8, 0xAAAAAA);
			return;
		}

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.spawn.current_size", ClientIslandCache.getSpawnSize()),
				CONTENT_X, TOP_BAR_HEIGHT + 8, 0xFFFFFF);

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.spawn.home_warning"), CONTENT_X, HOME_WARNING_Y, 0xFFCC55);

		BlockPos home = ClientIslandCache.getSpawnHomeLocation();
		Text homeText = home != null
				? Text.translatable("islandcoreclient.admin.spawn.home_current", home.getX(), home.getY(), home.getZ())
				: Text.translatable("islandcoreclient.admin.spawn.home_not_set");
		context.drawTextWithShadow(this.textRenderer, homeText, CONTENT_X, HOME_WARNING_Y + LINE_HEIGHT, 0xDDDDDD);
	}

	private void onCreateClicked() {
		int size = parseSize(this.sizeField.getText(), 25);
		ClientPlayNetworking.send(new SpawnIslandCreateC2S(size));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
	}

	private void onResizeFieldChanged(String text) {
		if (this.resizeButton == null) {
			return;
		}
		try {
			int value = Integer.parseInt(text.trim());
			this.resizeButton.active = value > ClientIslandCache.getSpawnSize();
		} catch (NumberFormatException e) {
			this.resizeButton.active = false;
		}
	}

	private void onResizeClicked() {
		int value = parseSize(this.sizeField.getText(), -1);
		if (value <= ClientIslandCache.getSpawnSize()) {
			// Defense in depth: the button is already inactive in this case, this should be
			// unreachable, but resizeIsland must never shrink, so never send it regardless.
			return;
		}
		ClientPlayNetworking.send(new SpawnIslandResizeC2S(value));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
	}

	private void onSetHomeClicked() {
		ClientPlayNetworking.send(new SpawnIslandSetHomeC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
	}

	private static int parseSize(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
