package com.skyframework.islandcoreclient.gui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientAdminIslandSummaryView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Global island list for admins. Rows are all created once in {@link #initContent()} and just
 * repositioned/hidden every frame based on the search field, rather than rebuilt on every
 * keystroke — rebuilding would drop keyboard focus from the field on each character typed.
 */
public class AdminIslandListScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int SEARCH_Y = TOP_BAR_HEIGHT + 8;
	private static final int ROWS_START_Y = SEARCH_Y + 24;
	private static final int ROW_WIDTH = 360;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int SPAWN_BUTTON_HEIGHT = 20;

	private final List<ClientAdminIslandSummaryView> summaries = new ArrayList<>(ClientIslandCache.getAdminIslands());
	private final List<ButtonWidget> rowButtons = new ArrayList<>();

	private TextFieldWidget searchField;

	public AdminIslandListScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.admin.island_list.title"), parent);
	}

	@Override
	protected void initContent() {
		this.rowButtons.clear();

		this.searchField = new TextFieldWidget(this.textRenderer, CONTENT_X, SEARCH_Y, ROW_WIDTH, 20,
				Text.translatable("islandcoreclient.admin.island_list.search_placeholder"));
		this.searchField.setPlaceholder(Text.translatable("islandcoreclient.admin.island_list.search_placeholder"));
		this.searchField.setMaxLength(32);
		this.addDrawableChild(this.searchField);

		for (ClientAdminIslandSummaryView summary : this.summaries) {
			boolean deleting = "DELETING".equals(summary.state());
			Text label = Text.literal(summary.ownerName() + " — " + summary.size() + "/" + summary.maxSize()
					+ " " + summary.type() + " · " + summary.state() + " · " + summary.memberCount() + " miembros");
			if (deleting) {
				label = label.copy().formatted(Formatting.RED);
			}

			ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(label,
							b -> this.client.setScreen(new AdminIslandDetailScreen(summary.ownerUuid(), this)))
					.dimensions(CONTENT_X, ROWS_START_Y, ROW_WIDTH, ROW_HEIGHT)
					.build());
			button.active = !deleting;
			this.rowButtons.add(button);
		}

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_list.manage_spawn_button"),
						button -> this.client.setScreen(new SpawnManagerScreen(this)))
				.dimensions(CONTENT_X, this.height - 16 - SPAWN_BUTTON_HEIGHT, 220, SPAWN_BUTTON_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		String query = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
		int y = ROWS_START_Y;

		for (int i = 0; i < this.summaries.size(); i++) {
			ClientAdminIslandSummaryView summary = this.summaries.get(i);
			ButtonWidget button = this.rowButtons.get(i);
			boolean matches = query.isEmpty() || summary.ownerName().toLowerCase(Locale.ROOT).contains(query);
			button.visible = matches;
			if (matches) {
				button.setY(y);
				y += ROW_HEIGHT + ROW_GAP;
			}
		}

		if (this.summaries.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_list.empty"), CONTENT_X, ROWS_START_Y, 0xAAAAAA);
		}
	}
}
