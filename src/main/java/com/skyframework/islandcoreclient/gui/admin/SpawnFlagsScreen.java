package com.skyframework.islandcoreclient.gui.admin;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.FlagPresetRow;
import com.skyframework.islandcoreclient.gui.common.PagedFlagGrid;
import com.skyframework.islandcoreclient.gui.common.TriStateRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnExceptionGroupSetPresetC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnExceptionGroupsStatusRequestC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnFlagSetC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnFlagSetPresetC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnFlagsStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientExceptionGroupView;
import com.skyframework.islandcoreclient.state.ClientFlagView;
import com.skyframework.islandcoreclient.state.ClientSpawnFlagsCache;
import com.skyframework.islandcoreclient.state.ClientTriState;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Spawn's own "Permisos"/"General" configuration — treated as a normal island's flags/exceptions
 * for this purpose (B.2 of the "teletransportes dinámicos" sprint), reached from SpawnManagerScreen
 * (its own content is already dense — see that screen's class javadoc — so this lives on its own
 * page instead of adding a 3rd tab there, same "split into a dedicated screen" pattern
 * AdminIslandDetailScreen→AdminIslandMembersScreen already established).
 *
 * <p>Exact same tab split/component reuse as SettingsScreen and DefaultConfigScreen (General
 * first, same order fixed there): "Permisos" is {@link PagedFlagGrid} + {@link FlagPresetRow} for
 * every ROLE_BASED flag preset and every exception group preset; "General" is the 3 ISLAND_GLOBAL
 * flags, unpaginated, with {@link TriStateRow} — driven by {@link ClientSpawnFlagsCache} (a
 * separate cache from ClientIslandCache's own, so this never collides with the acting admin's own
 * island flags if they have one).
 */
public class SpawnFlagsScreen extends BaseMenuScreen {
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

	private Tab activeTab = Tab.PERMISSIONS;
	private final PagedFlagGrid grid = new PagedFlagGrid(0, CONTENT_TOP, 1, 1, ROW_HEIGHT, ROW_SPACING, GRID_COLUMN_GAP);

	public SpawnFlagsScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.spawn.flags_title"), parent);
		ClientPlayNetworking.send(new SpawnFlagsStatusRequestC2S());
		ClientPlayNetworking.send(new SpawnExceptionGroupsStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh SpawnFlagsStatusS2C/SpawnExceptionGroupsStatusS2C
	// lands while this screen is open — same pattern as every other status-driven screen.
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
		button.active = !isActiveTab;
	}

	private void onTabClicked(Tab tab) {
		this.activeTab = tab;
		this.clearAndInit();
	}

	private void initGeneralTab() {
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = CONTENT_TOP;
		for (ClientFlagView flag : globalFlags()) {
			TriStateRow row = new TriStateRow(x, y, ROW_WIDTH, ROW_HEIGHT, flag.label(), flag.islandOverride(), true,
					newValue -> onFlagOverrideChanged(flag.flagId(), newValue));
			row.setTooltip(Tooltip.of(flag.description()));
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

		List<ClientFlagView> roleFlags = roleBasedFlags();
		List<ClientExceptionGroupView> groups = ClientSpawnFlagsCache.getExceptionGroups();
		grid.setItemCount(roleFlags.size() + groups.size());

		for (int i = 0; i < roleFlags.size(); i++) {
			if (!grid.isItemOnCurrentPage(i)) {
				continue;
			}
			ClientFlagView flag = roleFlags.get(i);
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					flag.label(), flag.currentPreset(), true, preset -> onFlagPresetChanged(flag.flagId(), preset));
			row.setTooltip(Tooltip.of(flag.description()));
			this.addDrawableChild(row);
		}

		int groupsOffset = roleFlags.size();
		for (int j = 0; j < groups.size(); j++) {
			int i = groupsOffset + j;
			if (!grid.isItemOnCurrentPage(i)) {
				continue;
			}
			ClientExceptionGroupView group = groups.get(j);
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					group.label(), group.currentPreset(), true, preset -> onExceptionPresetChanged(group.groupId(), preset));
			row.setTooltip(Tooltip.of(group.description()));
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

	private void onFlagOverrideChanged(String flagId, ClientTriState newValue) {
		ClientTriState previous = findFlag(flagId).map(ClientFlagView::islandOverride).orElse(ClientTriState.DEFAULT);
		ClientSpawnFlagsCache.updateFlagOverride(flagId, newValue);
		ClientPlayNetworking.send(new SpawnFlagSetC2S(flagId, newValue.name().toLowerCase(Locale.ROOT)));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientSpawnFlagsCache.updateFlagOverride(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private void onFlagPresetChanged(String flagId, String preset) {
		String previous = findFlag(flagId).map(ClientFlagView::currentPreset).orElse("custom");
		ClientSpawnFlagsCache.updateFlagPreset(flagId, preset);
		ClientPlayNetworking.send(new SpawnFlagSetPresetC2S(flagId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnFlagsStatusRequestC2S());
			} else {
				ClientSpawnFlagsCache.updateFlagPreset(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private void onExceptionPresetChanged(String groupId, String preset) {
		String previous = findExceptionGroup(groupId).map(ClientExceptionGroupView::currentPreset).orElse("custom");
		ClientSpawnFlagsCache.updateExceptionGroupPreset(groupId, preset);
		ClientPlayNetworking.send(new SpawnExceptionGroupSetPresetC2S(groupId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new SpawnExceptionGroupsStatusRequestC2S());
			} else {
				ClientSpawnFlagsCache.updateExceptionGroupPreset(groupId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private static List<ClientFlagView> roleBasedFlags() {
		return ClientSpawnFlagsCache.getFlags().stream().filter(flag -> flag.category() == ClientFlagView.Category.ROLE_BASED).toList();
	}

	private static List<ClientFlagView> globalFlags() {
		return ClientSpawnFlagsCache.getFlags().stream().filter(flag -> flag.category() == ClientFlagView.Category.ISLAND_GLOBAL).toList();
	}

	private static Optional<ClientFlagView> findFlag(String flagId) {
		return ClientSpawnFlagsCache.getFlags().stream().filter(flag -> flag.flagId().equals(flagId)).findFirst();
	}

	private static Optional<ClientExceptionGroupView> findExceptionGroup(String groupId) {
		return ClientSpawnFlagsCache.getExceptionGroups().stream().filter(group -> group.groupId().equals(groupId)).findFirst();
	}
}
