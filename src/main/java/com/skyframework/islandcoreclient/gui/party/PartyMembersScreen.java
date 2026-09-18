package com.skyframework.islandcoreclient.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ScrollableRowList;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.member.MemberAllyRemoveC2S;
import com.skyframework.islandcoreclient.network.party.PartyKickC2S;
import com.skyframework.islandcoreclient.network.party.PartyStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;
import com.skyframework.islandcoreclient.state.ClientPartyCache;
import com.skyframework.islandcoreclient.state.ClientPartyMemberView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * Full-page list for the local player's own party members AND their island's allies together, one
 * screen instead of two — split out of {@link PartyScreen} the same way
 * {@code AdminIslandMembersScreen} is split out of {@code AdminIslandDetailScreen}: unbounded
 * counts, so a dedicated page with all the room either list could ever need. Reads from the same
 * {@link ClientPartyCache}/{@link ClientIslandCache} {@code PartyScreen} already populated — no
 * separate network request, same {@link #refreshFromNetwork()} pattern as every other
 * status-driven screen for when a fresh {@code PartyStatusS2C} lands while this screen is open
 * (ally add/remove already rebuilds via its own action callback, same convention the old
 * MembersScreen used before the "alianzas" consolidation — no separate hook needed for that half).
 *
 * <p>A search field ({@code searchQuery}, persisted across {@link #clearAndInit()} the same way
 * {@code AdminIslandListScreen#searchQuery} is) filters BOTH lists by name, client-side only — no
 * network round-trip, unlike that screen's server-paginated search: every member/ally is already
 * fully loaded locally, so there's nothing to re-request. Still uses an explicit "Buscar" button
 * rather than filtering on every keystroke, to avoid tearing down (and defocusing) the search field
 * itself on every character typed — same field+button shape as {@code AdminIslandListScreen}.
 *
 * <p>[Expulsar] per member row when the local player is the party leader ({@code PartyKickC2S});
 * [Quitar] per ally row when the local player owns an island ({@code MemberAllyRemoveC2S}) — the
 * same two entry points {@code PartyScreen}'s old embedded lists used before this split.
 *
 * <p>Members (left) and allies (right) sit in two side-by-side columns rather than stacked — no
 * player name needs anywhere near half the screen's width, so splitting horizontally instead of
 * vertically lets BOTH lists use the full available height at once instead of splitting it, roughly
 * doubling how many rows of each are visible before scrolling (see the sprint notes' pixel-math for
 * the exact row count at the project's reference window size).
 */
public class PartyMembersScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int SEARCH_FIELD_WIDTH = 200;
	private static final int SEARCH_BUTTON_WIDTH = 70;
	private static final int SEARCH_ROW_HEIGHT = 20;
	private static final int SECTION_GAP = 8;
	// Same column-gap value SettingsScreen's PagedFlagGrid already uses for its 2-column layout.
	private static final int COLUMN_GAP = 16;

	private static final int SEARCH_Y = TOP_BAR_HEIGHT + 8;

	private record IndexedWidget(int rowIndex, ClickableWidget widget) {
	}

	// Both columns share the same headingY/listTop/viewportHeight — only X and width differ.
	private record Layout(int headingY, int listTop, int viewportHeight, int leftX, int rightX, int columnWidth) {
	}

	private String searchQuery = "";
	private TextFieldWidget searchField;

	private final ScrollableRowList memberList = new ScrollableRowList(CONTENT_X, 0, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final ScrollableRowList alliedList = new ScrollableRowList(CONTENT_X, 0, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final List<IndexedWidget> memberRowWidgets = new ArrayList<>();
	private final List<IndexedWidget> alliedRowWidgets = new ArrayList<>();

	public PartyMembersScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.party.members_title"), parent);
	}

	// Called by ClientPacketHandlers when a fresh PartyStatusS2C lands while this screen is open —
	// same pattern as every other status-driven screen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	private Layout computeLayout() {
		int listsBottom = this.height - 16;
		int headingY = SEARCH_Y + SEARCH_ROW_HEIGHT + SECTION_GAP;
		int listTop = headingY + LINE_HEIGHT + 6;
		int viewportHeight = Math.max(ROW_HEIGHT, listsBottom - listTop);

		int totalWidth = this.width - CONTENT_X - 16;
		int columnWidth = (totalWidth - COLUMN_GAP) / 2;
		int leftX = CONTENT_X;
		int rightX = CONTENT_X + columnWidth + COLUMN_GAP;

		return new Layout(headingY, listTop, viewportHeight, leftX, rightX, columnWidth);
	}

	@Override
	protected void initContent() {
		Layout layout = computeLayout();

		this.searchField = new TextFieldWidget(this.textRenderer, CONTENT_X, SEARCH_Y, SEARCH_FIELD_WIDTH, SEARCH_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.search_placeholder"));
		this.searchField.setPlaceholder(Text.translatable("islandcoreclient.party.search_placeholder"));
		this.searchField.setMaxLength(32);
		this.searchField.setText(this.searchQuery);
		this.searchField.setChangedListener(text -> this.searchQuery = text);
		this.addDrawableChild(this.searchField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.search_button"),
						button -> this.clearAndInit())
				.dimensions(CONTENT_X + SEARCH_FIELD_WIDTH + 8, SEARCH_Y, SEARCH_BUTTON_WIDTH, SEARCH_ROW_HEIGHT)
				.build());

		boolean isLeader = isLocalPlayerLeader();
		UUID localUuid = localPlayerUuid();
		int memberActionsX = layout.leftX() + layout.columnWidth() - ACTION_BUTTON_WIDTH;
		int allyActionsX = layout.rightX() + layout.columnWidth() - ACTION_BUTTON_WIDTH;

		List<ClientPartyMemberView> members = filteredMembers();
		memberList.setViewport(layout.leftX(), layout.listTop(), layout.columnWidth(), layout.viewportHeight());
		memberList.setItemCount(members.size());
		memberRowWidgets.clear();
		for (int i = 0; i < members.size(); i++) {
			ClientPartyMemberView member = members.get(i);
			if (isLeader && localUuid != null && !member.uuid().equals(localUuid)) {
				int buttonY = memberList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
				boolean rowVisible = memberList.isRowVisible(i);
				ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.kick"),
								b -> onKickClicked(member.uuid()))
						.dimensions(memberActionsX, buttonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
				button.visible = rowVisible;
				button.active = rowVisible;
				memberRowWidgets.add(new IndexedWidget(i, button));
			}
		}

		List<ClientMemberView> allies = filteredAllies();
		alliedList.setViewport(layout.rightX(), layout.listTop(), layout.columnWidth(), layout.viewportHeight());
		alliedList.setItemCount(allies.size());
		alliedRowWidgets.clear();
		for (int i = 0; i < allies.size(); i++) {
			ClientMemberView ally = allies.get(i);
			int buttonY = alliedList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
			boolean rowVisible = alliedList.isRowVisible(i);
			ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.ally_remove"),
							b -> onAllyRemoveClicked(ally.uuid()))
					.dimensions(allyActionsX, buttonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
					.build());
			button.visible = rowVisible;
			button.active = rowVisible;
			alliedRowWidgets.add(new IndexedWidget(i, button));
		}
	}

	private List<ClientPartyMemberView> filteredMembers() {
		String needle = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		List<ClientPartyMemberView> members = ClientPartyCache.getMembers();
		if (needle.isEmpty()) {
			return members;
		}
		return members.stream().filter(member -> member.name().toLowerCase(Locale.ROOT).contains(needle)).toList();
	}

	private List<ClientMemberView> filteredAllies() {
		String needle = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		List<ClientMemberView> allies = ClientIslandCache.getMembers().stream()
				.filter(member -> member.role() == ClientMemberView.Role.ALLY).toList();
		if (needle.isEmpty()) {
			return allies;
		}
		return allies.stream().filter(ally -> ally.name().toLowerCase(Locale.ROOT).contains(needle)).toList();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (memberList.isMouseOver(mouseX, mouseY)) {
			memberList.scroll(verticalAmount);
			repositionRows(memberList, memberRowWidgets);
			return true;
		}
		if (alliedList.isMouseOver(mouseX, mouseY)) {
			alliedList.scroll(verticalAmount);
			repositionRows(alliedList, alliedRowWidgets);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private static void repositionRows(ScrollableRowList list, List<IndexedWidget> widgets) {
		for (IndexedWidget iw : widgets) {
			boolean visible = list.isRowVisible(iw.rowIndex());
			iw.widget().setY(list.getRowY(iw.rowIndex()));
			iw.widget().visible = visible;
			iw.widget().active = visible;
		}
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		Layout layout = computeLayout();

		List<ClientPartyMemberView> members = filteredMembers();
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.members_heading_count", members.size()).formatted(Formatting.BOLD, Formatting.GOLD),
				layout.leftX(), layout.headingY(), 0xFFFFFF);

		if (members.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.members_empty"), layout.leftX(), layout.listTop(), 0xAAAAAA);
		} else {
			memberList.startClip(context);
			for (int i = 0; i < members.size(); i++) {
				if (!memberList.isRowVisible(i)) {
					continue;
				}
				ClientPartyMemberView member = members.get(i);
				boolean isLeaderRow = member.uuid().equals(ClientPartyCache.getLeaderUuid());
				Text nameLine = Text.literal(member.name())
						.append(isLeaderRow ? Text.translatable("islandcoreclient.party.leader_suffix").formatted(Formatting.GOLD) : Text.empty());
				int y = memberList.getRowY(i);
				context.drawTextWithShadow(this.textRenderer, nameLine, layout.leftX(), y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			}
			memberList.endClip(context);
			memberList.renderScrollbar(context);
		}

		List<ClientMemberView> allies = filteredAllies();
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.allies_heading_count", allies.size()).formatted(Formatting.BOLD, Formatting.YELLOW),
				layout.rightX(), layout.headingY(), 0xFFFFFF);

		if (allies.isEmpty()) {
			Text emptyText = ClientIslandCache.hasIsland()
					? Text.translatable("islandcoreclient.party.allies_empty")
					: Text.translatable("islandcoreclient.party.no_island_for_allies").formatted(Formatting.GRAY);
			context.drawTextWithShadow(this.textRenderer, emptyText, layout.rightX(), layout.listTop(), 0xAAAAAA);
		} else {
			alliedList.startClip(context);
			for (int i = 0; i < allies.size(); i++) {
				if (!alliedList.isRowVisible(i)) {
					continue;
				}
				ClientMemberView ally = allies.get(i);
				int y = alliedList.getRowY(i);
				context.drawTextWithShadow(this.textRenderer, Text.literal(ally.name()), layout.rightX(), y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			}
			alliedList.endClip(context);
			alliedList.renderScrollbar(context);
		}
	}

	@Nullable
	private static UUID localPlayerUuid() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null ? client.player.getUuid() : null;
	}

	private static boolean isLocalPlayerLeader() {
		UUID localUuid = localPlayerUuid();
		return localUuid != null && ClientPartyCache.isLeader(localUuid);
	}

	private void onKickClicked(UUID targetUuid) {
		ClientPlayNetworking.send(new PartyKickC2S(targetUuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onAllyRemoveClicked(UUID uuid) {
		ClientPlayNetworking.send(new MemberAllyRemoveC2S(uuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientIslandCache.removeMember(uuid);
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
