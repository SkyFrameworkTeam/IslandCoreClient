package com.skyframework.islandcoreclient.gui.admin;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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
		simulateSpawnCreate(size);
		this.clearAndInit();
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
			// unreachable, but resizeIsland must never shrink, so never simulate it regardless.
			return;
		}
		simulateSpawnResize(value);
		this.clearAndInit();
	}

	private void onSetHomeClicked() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			simulateSpawnSetHome(player.getBlockPos());
		}
	}

	private static int parseSize(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	// TODO: replace with sending SpawnIslandCreateC2S and awaiting ActionResultS2C once IslandCore
	// implements the admin protocol.
	private static void simulateSpawnCreate(int size) {
		ClientIslandCache.setSpawnExists(true);
		ClientIslandCache.setSpawnSize(size);
	}

	// TODO: replace with sending SpawnIslandResizeC2S and awaiting ActionResultS2C once IslandCore
	// implements the admin protocol. Client-side validation above (strictly greater than the
	// current size) mirrors the server's real constraint: resizeIsland only ever grows.
	private static void simulateSpawnResize(int newSize) {
		ClientIslandCache.setSpawnSize(newSize);
	}

	// TODO: replace with sending SpawnIslandSetHomeC2S once IslandCore implements the admin
	// protocol. The real packet must NOT carry coordinates as an argument — the server reads the
	// admin's actual position itself. This client-side BlockPos read only exists to preview the
	// behavior locally while there is no server to talk to.
	private static void simulateSpawnSetHome(BlockPos pos) {
		ClientIslandCache.setSpawnHomeLocation(pos);
	}
}
