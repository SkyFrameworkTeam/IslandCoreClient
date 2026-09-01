package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.FlagPresetRow;
import com.skyframework.islandcoreclient.gui.common.PagedFlagGrid;
import com.skyframework.islandcoreclient.gui.common.TriStateRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.flag.ExceptionGroupSetPresetC2S;
import com.skyframework.islandcoreclient.network.flag.ExceptionGroupsStatusRequestC2S;
import com.skyframework.islandcoreclient.network.flag.FlagSetC2S;
import com.skyframework.islandcoreclient.network.flag.FlagSetPresetC2S;
import com.skyframework.islandcoreclient.network.flag.FlagsStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientExceptionGroupView;
import com.skyframework.islandcoreclient.state.ClientFlagView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
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
 * Replaces the old 3-toggle legacy IslandSetting view (firespread/pvp/mobdamage via
 * IslandSettingsUpdateC2S) with the real flag engine: the 9 flags (6 ROLE_BASED, 3 ISLAND_GLOBAL)
 * and the server's exception groups — see FlagsStatusRequestC2S/ExceptionGroupsStatusRequestC2S.
 * The server already treats firespread/pvp/mobdamage as aliases of 3 of these 9 flags, so nothing
 * from the old view is lost.
 *
 * <p>Two tabs: "Permisos" — the 6 ROLE_BASED flags and every exception group (now that the server
 * resolves both per role via the same 4-level preset, see FlagSetPresetC2S/
 * ExceptionGroupSetPresetC2S) in one flat 2-column, paginated grid ({@link PagedFlagGrid}), all
 * shown with {@link FlagPresetRow} — and "General" — the 3 ISLAND_GLOBAL flags, unpaginated, with
 * {@link TriStateRow}, since a preset is meaningless for a flag with no per-role distinction and
 * 3 items never need pagination.
 */
public class SettingsScreen extends BaseMenuScreen {
	private enum Tab {
		PERMISSIONS, GENERAL
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

	public SettingsScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.settings.title"), parent);
		ClientPlayNetworking.send(new FlagsStatusRequestC2S());
		ClientPlayNetworking.send(new ExceptionGroupsStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh FlagsStatusS2C/ExceptionGroupsStatusS2C lands
	// while this screen is open — same pattern as every other status-driven screen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		int tabsWidth = 2 * TAB_BUTTON_WIDTH + TAB_BUTTON_GAP;
		int tabsX = this.width / 2 - tabsWidth / 2;
		addTabButton(tabsX, Tab.PERMISSIONS, Text.translatable("islandcoreclient.settings.tab_permissions"));
		addTabButton(tabsX + (TAB_BUTTON_WIDTH + TAB_BUTTON_GAP), Tab.GENERAL, Text.translatable("islandcoreclient.settings.tab_general"));

		switch (activeTab) {
			case PERMISSIONS -> initPermissionsTab();
			case GENERAL -> initGeneralTab();
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

	private void initPermissionsTab() {
		int gridWidth = Math.min(GRID_MAX_WIDTH, this.width - 32);
		int gridX = this.width / 2 - gridWidth / 2;
		int gridViewportHeight = Math.max(ROW_HEIGHT,
				this.height - CONTENT_BOTTOM_MARGIN - PAGINATION_ROW_HEIGHT - PAGINATION_GAP - CONTENT_TOP);
		grid.setViewport(gridX, CONTENT_TOP, gridWidth, gridViewportHeight);

		boolean owner = ClientIslandCache.isOwner();
		List<ClientFlagView> roleFlags = roleBasedFlags();
		List<ClientExceptionGroupView> groups = ClientIslandCache.getExceptionGroups();
		grid.setItemCount(roleFlags.size() + groups.size());

		for (int i = 0; i < roleFlags.size(); i++) {
			if (!grid.isItemOnCurrentPage(i)) {
				continue;
			}
			ClientFlagView flag = roleFlags.get(i);
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					flag.label(), flag.currentPreset(), owner, preset -> onFlagPresetChanged(flag.flagId(), preset));
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
			boolean rowEditable = owner && group.ownerConfigurable();
			FlagPresetRow row = new FlagPresetRow(grid.getItemX(i), grid.getItemY(i), grid.getItemWidth(), ROW_HEIGHT,
					group.label(), group.currentPreset(), rowEditable, preset -> onExceptionPresetChanged(group.groupId(), preset));
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

	private void initGeneralTab() {
		boolean owner = ClientIslandCache.isOwner();
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = CONTENT_TOP;
		for (ClientFlagView flag : globalFlags()) {
			TriStateRow row = new TriStateRow(x, y, ROW_WIDTH, ROW_HEIGHT, flag.label(), flag.islandOverride(), owner,
					newValue -> onFlagOverrideChanged(flag.flagId(), newValue));
			row.setTooltip(Tooltip.of(flag.description()));
			this.addDrawableChild(row);
			y += ROW_HEIGHT + ROW_SPACING;
		}
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

	private static List<ClientFlagView> roleBasedFlags() {
		return ClientIslandCache.getFlags().stream().filter(flag -> flag.category() == ClientFlagView.Category.ROLE_BASED).toList();
	}

	private static List<ClientFlagView> globalFlags() {
		return ClientIslandCache.getFlags().stream().filter(flag -> flag.category() == ClientFlagView.Category.ISLAND_GLOBAL).toList();
	}

	// FlagPresetRow already switched itself optimistically before this runs. On success, the exact
	// resolved values (and every other flag view derived from them) depend on server/code defaults
	// this client doesn't replicate, so refetch instead of guessing — the FlagsStatusS2C reply
	// rebuilds this screen automatically (see ClientPacketHandlers). On failure, revert the cached
	// preset immediately instead of waiting on a refetch that isn't coming.
	private void onFlagPresetChanged(String flagId, String preset) {
		String previous = findFlag(flagId).map(ClientFlagView::currentPreset).orElse("custom");
		ClientIslandCache.updateFlagPreset(flagId, preset);
		ClientPlayNetworking.send(new FlagSetPresetC2S(flagId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new FlagsStatusRequestC2S());
			} else {
				ClientIslandCache.updateFlagPreset(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	// Exact mirror of onFlagPresetChanged above, for exception groups (ExceptionGroupSetPresetC2S).
	private void onExceptionPresetChanged(String groupId, String preset) {
		String previous = findExceptionGroup(groupId).map(ClientExceptionGroupView::currentPreset).orElse("custom");
		ClientIslandCache.updateExceptionGroupPreset(groupId, preset);
		ClientPlayNetworking.send(new ExceptionGroupSetPresetC2S(groupId, preset));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new ExceptionGroupsStatusRequestC2S());
			} else {
				ClientIslandCache.updateExceptionGroupPreset(groupId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	// TriStateRow already cycled itself optimistically before this runs. On failure, revert the
	// cached override and rebuild so the row reflects the real (unchanged) state. On success, left
	// as-is (no refetch) — same optimistic pattern ToggleRow-based rows already use elsewhere.
	private void onFlagOverrideChanged(String flagId, ClientTriState newValue) {
		ClientTriState previous = findFlag(flagId).map(ClientFlagView::islandOverride).orElse(ClientTriState.DEFAULT);
		ClientIslandCache.updateFlagOverride(flagId, newValue);
		ClientPlayNetworking.send(new FlagSetC2S(flagId, newValue.name().toLowerCase(Locale.ROOT)));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientIslandCache.updateFlagOverride(flagId, previous);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	private static Optional<ClientFlagView> findFlag(String flagId) {
		return ClientIslandCache.getFlags().stream().filter(flag -> flag.flagId().equals(flagId)).findFirst();
	}

	private static Optional<ClientExceptionGroupView> findExceptionGroup(String groupId) {
		return ClientIslandCache.getExceptionGroups().stream().filter(group -> group.groupId().equals(groupId)).findFirst();
	}
}
