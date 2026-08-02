package com.skyframework.islandcoreclient.gui.admin;

import java.util.List;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandListRequestC2S;
import com.skyframework.islandcoreclient.state.ClientAdminIslandSummaryView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Global, server-paginated island list for admins. The request is sent once from the constructor
 * (not from {@link #initContent()}): {@link #refreshFromNetwork()} only rebuilds widgets from
 * whatever {@link ClientIslandCache} now holds — resending the request from initContent() too
 * would re-trigger on every single rebuild this reply itself causes, looping forever while the
 * screen stays open.
 */
public class AdminIslandListScreen extends BaseMenuScreen {
	public static final int PAGE_SIZE = 10;
	private static final int CONTENT_X = 16;
	private static final int SEARCH_Y = TOP_BAR_HEIGHT + 8;
	private static final int ROWS_START_Y = SEARCH_Y + 24;
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int SEARCH_FIELD_WIDTH = 200;
	private static final int SEARCH_BUTTON_WIDTH = 70;
	private static final int PAGE_BUTTON_WIDTH = 60;
	private static final int SPAWN_BUTTON_HEIGHT = 20;

	private String searchQuery = "";
	private TextFieldWidget searchField;

	public AdminIslandListScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.island_list.title"), parent);
		ClientPlayNetworking.send(new AdminIslandListRequestC2S(0, PAGE_SIZE, ""));
	}

	// Called by ClientPacketHandlers when a fresh AdminIslandListS2C lands while this screen is
	// open — same pattern as TeleportsScreen/BiomeScreen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		this.searchField = new TextFieldWidget(this.textRenderer, CONTENT_X, SEARCH_Y, SEARCH_FIELD_WIDTH, 20,
				Text.translatable("islandcoreclient.admin.island_list.search_placeholder"));
		this.searchField.setPlaceholder(Text.translatable("islandcoreclient.admin.island_list.search_placeholder"));
		this.searchField.setMaxLength(32);
		this.searchField.setText(this.searchQuery);
		this.searchField.setChangedListener(text -> this.searchQuery = text);
		this.addDrawableChild(this.searchField);

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_list.search_button"),
						button -> requestPage(0))
				.dimensions(CONTENT_X + SEARCH_FIELD_WIDTH + 8, SEARCH_Y, SEARCH_BUTTON_WIDTH, 20)
				.build());

		List<ClientAdminIslandSummaryView> summaries = ClientIslandCache.getAdminIslands();
		int y = ROWS_START_Y;
		for (ClientAdminIslandSummaryView summary : summaries) {
			boolean deleting = "DELETING".equals(summary.state());
			Text label = Text.literal(summary.ownerName() + " — " + summary.size() + "/" + summary.maxSize()
					+ " " + summary.type() + " · " + summary.state() + " · " + summary.memberCount() + " miembros");
			if (deleting) {
				label = label.copy().formatted(Formatting.RED);
			}

			ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(label,
							b -> this.client.setScreen(new AdminIslandDetailScreen(summary.ownerUuid(), this)))
					.dimensions(CONTENT_X, y, ROW_WIDTH, ROW_HEIGHT)
					.build());
			button.active = !deleting;
			y += ROW_HEIGHT + ROW_GAP;
		}

		int currentPage = ClientIslandCache.getAdminIslandsCurrentPage();
		int totalPages = ClientIslandCache.getAdminIslandsTotalPages();
		int pageRowY = y + 8;

		ButtonWidget prevButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_list.prev_page"),
						button -> requestPage(currentPage - 1))
				.dimensions(CONTENT_X, pageRowY, PAGE_BUTTON_WIDTH, ROW_HEIGHT)
				.build());
		prevButton.active = currentPage > 0;

		ButtonWidget nextButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_list.next_page"),
						button -> requestPage(currentPage + 1))
				.dimensions(CONTENT_X + PAGE_BUTTON_WIDTH + 8, pageRowY, PAGE_BUTTON_WIDTH, ROW_HEIGHT)
				.build());
		nextButton.active = currentPage + 1 < totalPages;

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_list.manage_spawn_button"),
						button -> this.client.setScreen(new SpawnManagerScreen(this)))
				.dimensions(CONTENT_X, this.height - 16 - SPAWN_BUTTON_HEIGHT, 220, SPAWN_BUTTON_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (ClientIslandCache.getAdminIslands().isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_list.empty"), CONTENT_X, ROWS_START_Y, 0xAAAAAA);
		}

		int currentPage = ClientIslandCache.getAdminIslandsCurrentPage();
		int totalPages = ClientIslandCache.getAdminIslandsTotalPages();
		if (totalPages > 0) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_list.page_indicator", currentPage + 1, totalPages),
					CONTENT_X + (PAGE_BUTTON_WIDTH + 8) * 2, this.height - 16 - SPAWN_BUTTON_HEIGHT - 28, 0xAAAAAA);
		}
	}

	private void requestPage(int page) {
		ClientPlayNetworking.send(new AdminIslandListRequestC2S(page, PAGE_SIZE, this.searchQuery));
	}
}
