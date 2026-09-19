package com.skyframework.islandcoreclient.gui.admin;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.FlagPresetRow;
import com.skyframework.islandcoreclient.gui.common.PagedFlagGrid;
import com.skyframework.islandcoreclient.gui.common.TriStateRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.defaults.AdminDefaultsStatusRequestC2S;
import com.skyframework.islandcoreclient.network.admin.defaults.AdminExceptionSetServerDefaultC2S;
import com.skyframework.islandcoreclient.network.admin.defaults.AdminFlagSetServerDefaultC2S;
import com.skyframework.islandcoreclient.network.admin.defaults.AdminGlobalFlagSetServerDefaultC2S;
import com.skyframework.islandcoreclient.state.ClientAdminDefaultsCache;
import com.skyframework.islandcoreclient.state.ClientExceptionGroupView;
import com.skyframework.islandcoreclient.state.ClientFlagView;
import com.skyframework.islandcoreclient.state.ClientTriState;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Server-wide default configuration — not any specific island's, the "servidor" layer
 * AdminDefaultsStatusS2C reports. Operator-only, reached from the admin gateway in DashboardScreen.
 *
 * <p>Same "General"/"Permisos" two-tab split as SettingsScreen (General first, same order fixed
 * there), driven by {@link ClientAdminDefaultsCache} instead of an island's own flags/exceptions:
 * "Permisos" is the {@link PagedFlagGrid} + {@link FlagPresetRow} grid of every ROLE_BASED flag
 * preset and every exception group preset (unchanged from before this tab split existed); "General"
 * is the 3 ISLAND_GLOBAL flags, unpaginated, with {@link TriStateRow} — same component split
 * SettingsScreen already uses, just against the server-wide default instead of an island's own.
 */
public class DefaultConfigScreen extends BaseMenuScreen {
	private enum Tab {
		GENERAL, PERMISSIONS
	}

	private static final int ROW_WIDTH = 220;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int TAB_BUTTON_WIDTH = 110;
	private static final int TAB_BUTTON_HEIGHT = 20;
	private static final int TAB_BUTTON_GAP = 4;
	private static final int TAB_BAR_Y = TOP_BAR_HEIGHT + 6;
	private static final int CONTENT_TOP = TAB_BAR_Y + TAB_BUTTON_HEIGHT + 10;
	private static final int CONTENT_BOTTOM_MARGIN = 12;

	private static final int GRID_MAX_WIDTH = 480;
	private static final int GRID_COLUMN_GAP = 24;
	private static final int PAGINATION_ROW_HEIGHT = 20;
	private static final int PAGINATION_GAP = 8;
	private static final int PAGINATION_BUTTON_WIDTH = 90;

	private Tab activeTab = Tab.GENERAL;
	private final PagedFlagGrid grid = new PagedFlagGrid(0, CONTENT_TOP, 1, 1, ROW_HEIGHT, ROW_SPACING, GRID_COLUMN_GAP);

	public DefaultConfigScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.default_config.title"), parent);
		ClientPlayNetworking.send(new AdminDefaultsStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh AdminDefaultsStatusS2C lands while this screen is
	// open — same pattern as every other status-driven screen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		int tabsWidth = 2 * TAB_BUTTON_WIDTH + TAB_BUTTON_GAP;
		int tabsX = this.width / 2 - tabsWidth / 2;
		addTabButton(tabsX, Tab.GENERAL, Text.translatable("islandcoreclient.settings.tab_general"));
		addTabButton(tabsX + (TAB_BUTTON_WIDTH + TAB_BUTTON_GAP), Tab.PERMISSIONS, Text.translatable("islandcoreclient.settings.tab_permissions"));

		switch (activeTab) {
			case GENERAL -> initGeneralTab();
			case PERMISSIONS -> initPermissionsTab();
		}
	}

	private void addTabButton(int x, Tab tab, Text label) {
		boolean isActiveTab = tab == activeTab;
		Text message = isActiveTab ? label.copy().formatted(Formatting.UNDERLINE, Formatting.BOLD) : label;
		ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(message, b -> onTabClicked(tab))
				.dimensions(x, TAB_BAR_Y, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT)
				.build());
		// The active tab's own button is inert (you're already on it) — only the other one navigates.
		button.active = !isActiveTab;
	}

	private void onTabClicked(Tab tab) {
		this.activeTab = tab;
		this.clearAndInit();
	}

	private void initGeneralTab() {
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = CONTENT_TOP;
		for (ClientAdminDefaultsCache.GlobalDefaultView entry : ClientAdminDefaultsCache.getGlobalDefaults()) {
			TriStateRow row = new TriStateRow(x, y, ROW_WIDTH, ROW_HEIGHT, ClientFlagView.labelFor(entry.flagId()),
					entry.currentValue(), true, newValue -> onGlobalDefaultChanged(entry.flagId(), newValue));
			this.addDrawableChild(row);
			y += ROW_HEIGHT + ROW_SPACING;
		}
	}

	private void initPermissionsTab() {
		int gridWidth = Math.min(GRID_MAX_WIDTH, this.width - 32);
		int gridX = this.width / 2 - gridWidth / 2;
		int gridViewportHeight = Math.max(ROW_HEIGHT,
				this.height - CONTENT_BOTTOM_MARGIN - PAGINATION_ROW_HEIGHT - PAGINATION_GAP - CONTENT_TOP);
		grid.setViewport(gridX, CONTENT_TOP, gridWidth, gridViewportHeight);

		List<ClientAdminDefaultsCache.FlagDefaultView> flagDefaults = ClientAdminDefaultsCache.getFlagDefaults();
		List<ClientAdminDefaultsCache.ExceptionDefaultView> exceptionDefaults = ClientAdminDefaultsCache.getExceptionDefaults();
		grid.setItemCount(flagDefaults.size() + exceptionDefaults.size());

		for (int i = 0; i < flagDefaults.size(); i++) {
			if (!grid.isItemOnCurrentPage(i)) {
				continue;
			}
			ClientAdminDefaultsCache.FlagDefaultView entry = flagDefaults.get(i);
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					ClientFlagView.labelFor(entry.flagId()), entry.currentPreset(), true,
					preset -> onFlagDefaultChanged(entry.flagId(), preset));
			this.addDrawableChild(row);
		}

		int groupsOffset = flagDefaults.size();
		for (int j = 0; j < exceptionDefaults.size(); j++) {
			int i = groupsOffset + j;
			if (!grid.isItemOnCurrentPage(i)) {
				continue;
			}
			ClientAdminDefaultsCache.ExceptionDefaultView entry = exceptionDefaults.get(j);
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					ClientExceptionGroupView.labelFor(entry.groupId()), entry.currentPreset(), true,
					preset -> onExceptionDefaultChanged(entry.groupId(), preset));
			this.addDrawableChild(row);
		}

		int paginationY = paginationRowY();
		ButtonWidget prevButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.pagination.prev"),
						b -> {
							grid.prevPage();
							this.clearAndInit();
						})
				.dimensions(gridX, paginationY, PAGINATION_BUTTON_WIDTH, PAGINATION_ROW_HEIGHT)
				.build());
		prevButton.active = grid.hasPrevPage();

		ButtonWidget nextButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.pagination.next"),
						b -> {
							grid.nextPage();
							this.clearAndInit();
						})
				.dimensions(gridX + gridWidth - PAGINATION_BUTTON_WIDTH, paginationY, PAGINATION_BUTTON_WIDTH, PAGINATION_ROW_HEIGHT)
				.build());
		nextButton.active = grid.hasNextPage();
	}

	private int paginationRowY() {
		return this.height - CONTENT_BOTTOM_MARGIN - PAGINATION_ROW_HEIGHT;
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (activeTab != Tab.PERMISSIONS) {
			return;
		}
		Text indicator = Text.translatable("islandcoreclient.pagination.page_indicator", grid.getCurrentPage() + 1, grid.totalPages());
		int textWidth = this.textRenderer.getWidth(indicator);
		context.drawTextWithShadow(this.textRenderer, indicator, this.width / 2 - textWidth / 2,
				paginationRowY() + (PAGINATION_ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xAAAAAA);
	}

	// TriStateRow already cycled itself optimistically before this runs — same optimistic-then-
	// refetch-on-failure pattern SettingsScreen#onFlagOverrideChanged uses for an island's own
	// ISLAND_GLOBAL override, just against the server-wide default instead.
	private void onGlobalDefaultChanged(String flagId, ClientTriState newValue) {
		ClientTriState previous = findGlobalDefault(flagId).map(ClientAdminDefaultsCache.GlobalDefaultView::currentValue).orElse(ClientTriState.DEFAULT);
		ClientAdminDefaultsCache.updateGlobalDefaultValue(flagId, newValue);
		ClientPlayNetworking.send(new AdminGlobalFlagSetServerDefaultC2S(flagId, newValue.name().toLowerCase(Locale.ROOT)));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientAdminDefaultsCache.updateGlobalDefaultValue(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	// FlagPresetRow already switched itself optimistically before this runs — same
	// optimistic-then-refetch pattern SettingsScreen#onFlagPresetChanged uses for an island's own
	// flags, just against the server-wide default instead.
	private void onFlagDefaultChanged(String flagId, String preset) {
		String previous = findFlagDefault(flagId).map(ClientAdminDefaultsCache.FlagDefaultView::currentPreset).orElse("custom");
		ClientAdminDefaultsCache.updateFlagDefaultPreset(flagId, preset);
		ClientPlayNetworking.send(new AdminFlagSetServerDefaultC2S(flagId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new AdminDefaultsStatusRequestC2S());
			} else {
				ClientAdminDefaultsCache.updateFlagDefaultPreset(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private void onExceptionDefaultChanged(String groupId, String preset) {
		String previous = findExceptionDefault(groupId).map(ClientAdminDefaultsCache.ExceptionDefaultView::currentPreset).orElse("custom");
		ClientAdminDefaultsCache.updateExceptionDefaultPreset(groupId, preset);
		ClientPlayNetworking.send(new AdminExceptionSetServerDefaultC2S(groupId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new AdminDefaultsStatusRequestC2S());
			} else {
				ClientAdminDefaultsCache.updateExceptionDefaultPreset(groupId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private static Optional<ClientAdminDefaultsCache.GlobalDefaultView> findGlobalDefault(String flagId) {
		return ClientAdminDefaultsCache.getGlobalDefaults().stream().filter(entry -> entry.flagId().equals(flagId)).findFirst();
	}

	private static Optional<ClientAdminDefaultsCache.FlagDefaultView> findFlagDefault(String flagId) {
		return ClientAdminDefaultsCache.getFlagDefaults().stream().filter(entry -> entry.flagId().equals(flagId)).findFirst();
	}

	private static Optional<ClientAdminDefaultsCache.ExceptionDefaultView> findExceptionDefault(String groupId) {
		return ClientAdminDefaultsCache.getExceptionDefaults().stream().filter(entry -> entry.groupId().equals(groupId)).findFirst();
	}
}
