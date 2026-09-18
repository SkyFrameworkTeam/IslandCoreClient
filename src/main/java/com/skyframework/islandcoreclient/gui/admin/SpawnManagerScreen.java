package com.skyframework.islandcoreclient.gui.admin;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ToggleRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnAuthorizedPlayerAddC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnBuildProtectionSetC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnBuildProtectionStatusRequestC2S;
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
 *
 * <p>The build-protection section only appears once the Spawn island exists — same condition
 * {@link #initResizeForm()} already uses instead of {@link #initCreateForm()} — since there is
 * nothing to protect/authorize before that. The authorized-players LIST itself lives on its own
 * dedicated page ({@link SpawnAuthorizedPlayersScreen}, same fix already applied to
 * {@link AdminIslandDetailScreen}'s member list), reached via a "Ver jugadores autorizados (N)"
 * button that lives in the TOP BAR's free right-hand slot — same slot/height DashboardScreen's
 * admin toggle and DimensionManagerScreen's create button already use — instead of anywhere in the
 * scrollable content area, so it never competes with size/sethome/toggle/add-field for room. Only
 * the toggle and the "add authorized player" field/button — a quick action that doesn't warrant a
 * screen switch — stay in the content area, in the left column with this screen's other controls.
 */
public class SpawnManagerScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int FIELD_WIDTH = 80;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 170;
	// Sits to the right of the 200px-wide sethome button on the same row — verified against this
	// project's 427px virtual-width reference (854 real / Auto GUI Scale): row starts at
	// CONTENT_X(16) + 200 + 8 = 224, this button ends at 224 + 150 = 374, well inside the
	// width - 8 = 419 right margin at the minimum reference width.
	private static final int PERMISSIONS_BUTTON_WIDTH = 150;

	private static final int FORM_Y = TOP_BAR_HEIGHT + 8 + LINE_HEIGHT + 8;
	private static final int HOME_WARNING_Y = FORM_Y + FIELD_HEIGHT + 16;
	private static final int HOME_BUTTON_Y = HOME_WARNING_Y + LINE_HEIGHT * 2 + 8;

	private static final int BUILD_PROTECTION_TOGGLE_WIDTH = 220;
	private static final int BUILD_PROTECTION_TOGGLE_Y = HOME_BUTTON_Y + FIELD_HEIGHT + 16;

	// Top bar's free right-hand slot — same slot/height DashboardScreen's admin toggle and
	// DimensionManagerScreen's create button already use.
	private static final int TOP_BAR_ACTION_WIDTH = 190;
	private static final int TOP_BAR_ACTION_HEIGHT = 20;

	private static final int ADD_ROW_HEIGHT = 20;

	private TextFieldWidget sizeField;
	private ButtonWidget resizeButton;
	private TextFieldWidget authorizedNameField;

	public SpawnManagerScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.spawn.title"), parent);
		ClientPlayNetworking.send(new SpawnStatusRequestC2S());
		ClientPlayNetworking.send(new SpawnBuildProtectionStatusRequestC2S());
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
			return;
		}

		initResizeForm();
		initBuildProtectionSection();

		// Top bar, not the content area: frees the content area from having to make room for it
		// alongside size/sethome/toggle/add-field.
		int authorizedCount = ClientIslandCache.getSpawnAuthorizedPlayers().size();
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.view_authorized_button", authorizedCount),
						button -> onViewAuthorizedClicked())
				.dimensions(this.width - 8 - TOP_BAR_ACTION_WIDTH, (TOP_BAR_HEIGHT - TOP_BAR_ACTION_HEIGHT) / 2,
						TOP_BAR_ACTION_WIDTH, TOP_BAR_ACTION_HEIGHT)
				.build());
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

		// Same row as the sethome button, to its right — reuses spare horizontal room on this row
		// instead of adding a whole new row (this screen's content area is already tight; see the
		// class javadoc). Permisos/General is its own dedicated screen, not a 3rd tab here, since
		// SpawnManagerScreen has no room left for a paginated grid on top of everything else it
		// already shows.
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.flags_button"),
						button -> this.client.setScreen(new SpawnFlagsScreen(this)))
				.dimensions(CONTENT_X + 200 + 8, HOME_BUTTON_Y, PERMISSIONS_BUTTON_WIDTH, FIELD_HEIGHT)
				.build());
	}

	private void initBuildProtectionSection() {
		this.addDrawableChild(new ToggleRow(
				CONTENT_X, BUILD_PROTECTION_TOGGLE_Y, BUILD_PROTECTION_TOGGLE_WIDTH, FIELD_HEIGHT,
				Text.translatable("islandcoreclient.admin.spawn.build_protection_toggle"),
				ClientIslandCache.getSpawnBuildProtectionEnabled(), true,
				this::onBuildProtectionToggled));

		// Left column, bottom-pinned: a quick action, so it stays here instead of moving into
		// SpawnAuthorizedPlayersScreen along with the read-only list.
		int fieldWidth = 160;
		int buttonWidth = 70;
		int addFieldY = this.height - 16 - ADD_ROW_HEIGHT;

		this.authorizedNameField = new TextFieldWidget(this.textRenderer, CONTENT_X, addFieldY, fieldWidth, ADD_ROW_HEIGHT,
				Text.translatable("islandcoreclient.admin.spawn.authorized_add_placeholder"));
		this.authorizedNameField.setPlaceholder(Text.translatable("islandcoreclient.admin.spawn.authorized_add_placeholder"));
		this.authorizedNameField.setMaxLength(32);
		this.addDrawableChild(this.authorizedNameField);

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.spawn.authorized_add_button"),
						button -> onAuthorizedAddClicked())
				.dimensions(CONTENT_X + fieldWidth + 4, addFieldY, buttonWidth, ADD_ROW_HEIGHT)
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

	// ToggleRow already flipped itself optimistically before this runs — same pattern
	// SettingsScreen#onSettingToggled uses for IslandSettingsUpdateC2S. On failure, flip the
	// cached value back and rebuild so the row reflects the real (unchanged) state.
	private void onBuildProtectionToggled(boolean newValue) {
		ClientIslandCache.setSpawnBuildProtectionEnabled(newValue);
		ClientPlayNetworking.send(new SpawnBuildProtectionSetC2S(newValue));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientIslandCache.setSpawnBuildProtectionEnabled(!newValue);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private void onViewAuthorizedClicked() {
		this.client.setScreen(new SpawnAuthorizedPlayersScreen(this));
	}

	private void onAuthorizedAddClicked() {
		String targetName = this.authorizedNameField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SpawnAuthorizedPlayerAddC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// The real name/role for the new entry comes back on the next status refresh;
				// refetch now instead of guessing it locally — same reasoning as MembersScreen's
				// invite flow.
				ClientPlayNetworking.send(new SpawnBuildProtectionStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
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
