package com.skyframework.islandcoreclient.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ScrollableRowList;
import com.skyframework.islandcoreclient.gui.common.ToggleRow;
import com.skyframework.islandcoreclient.gui.island.PendingInvitesScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.alliance.LocationSharingSetC2S;
import com.skyframework.islandcoreclient.network.alliance.LocationSharingStatusRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.member.MemberAllyAddC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteC2S;
import com.skyframework.islandcoreclient.network.member.MemberRemoveC2S;
import com.skyframework.islandcoreclient.network.member.MemberTrustC2S;
import com.skyframework.islandcoreclient.network.party.PartyAcceptC2S;
import com.skyframework.islandcoreclient.network.party.PartyCreateC2S;
import com.skyframework.islandcoreclient.network.party.PartyDisbandConfirmC2S;
import com.skyframework.islandcoreclient.network.party.PartyDisbandRequestC2S;
import com.skyframework.islandcoreclient.network.party.PartyInviteC2S;
import com.skyframework.islandcoreclient.network.party.PartyLeaveC2S;
import com.skyframework.islandcoreclient.network.party.PartyRenameC2S;
import com.skyframework.islandcoreclient.network.party.PartyStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientAllyLocationView;
import com.skyframework.islandcoreclient.state.ClientAllyLocationsCache;
import com.skyframework.islandcoreclient.state.ClientIncomingPartyInviteView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientLocationSharingCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;
import com.skyframework.islandcoreclient.state.ClientPartyCache;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * Reached from the Dashboard's single "Party" button (replacing the old separate "Miembros" button
 * and the retired {@code OpenPartyKeybind}/{@code /islandparty} access) — always clickable regardless
 * of {@code ClientIslandCache.hasIsland()}, since the party half of this screen never depended on an
 * island to begin with. As of the "alianzas" consolidation sprint, this screen ALSO hosts
 * individual-player alliance management (an island concept — {@code IslandRole.ALLY}, see
 * {@code ClientIslandCache}) and all four location-sharing toggles, replacing the retired
 * AllianceScreen/Dashboard alliance tab entirely. As of the "fusión Miembros+Party" sprint, it also
 * absorbs the old standalone {@code MembersScreen} (island member list, invite, trust/remove) as its
 * own first page — see {@link Page#MEMBERS}.
 *
 * <p>3 pages (see {@link Page}): {@code MEMBERS} was folded in as a new first page rather than a
 * separate screen, reusing the exact same pagination this screen already had for PARTY/SHARING —
 * consistent with the member LIST itself (the actually unbounded part) already living in its own
 * page/screen elsewhere ({@link PartyMembersScreen} for party members+allies).
 *
 * <p>Navigation: "&lt;&lt; Anterior" / "Siguiente &gt;&gt;" at the BOTTOM, left/right-aligned with a
 * centered page indicator between them — the exact same {@code islandcoreclient.pagination.*}
 * labels AND position {@code SettingsScreen}'s {@code PagedFlagGrid} pagination row already uses.
 * {@link #page} is a plain persistent field surviving {@link #clearAndInit()} the same way
 * {@code PagedFlagGrid#currentPage} does.
 *
 * <p><b>Page 1 (MEMBERS)</b>: the island's member list (name+role), invite field+button, and
 * trust/remove per row — identical to the old {@code MembersScreen}, except when the player has no
 * island: the whole section renders dimmed/empty (same {@code 0x777777} pattern
 * {@code DashboardScreen} already uses for its own "no island" state) instead of disappearing, since
 * the Party button — and this screen — must stay usable either way. <b>Page 2 (PARTY)</b>: party
 * name/leader, invite field+button (leader only), rename field+button (leader only), add-ally
 * field+button (gated on {@code ClientIslandCache.hasIsland()}, independent of party state/
 * leadership), then "Salir de la party" / "Disolver party" side by side in the SAME row (leader-only
 * for Disolver). <b>Page 3 (SHARING)</b>: the 4 location-sharing toggles + [DEBUG], unchanged.
 */
public class PartyScreen extends BaseMenuScreen {
	private enum Page {
		MEMBERS, PARTY, SHARING
	}

	private record IndexedWidget(int rowIndex, ClickableWidget widget) {
	}

	// Mirrors the server's PartyDisbandRequests.TIMEOUT — purely for the local countdown display;
	// the server is the actual authority on whether a confirm still lands within the window.
	private static final long DISBAND_CONFIRM_WINDOW_SECONDS = 15L;

	// See AllianceScreen's old javadoc for this same trick, relocated here verbatim: a north offset,
	// not a random one — "near a known fixed point" for exercising AllyHudRenderer's projection
	// solo, without a second connected player actually sharing their position.
	private static final double DEBUG_NORTH_OFFSET = 20.0;

	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int FIELD_WIDTH = 150;
	private static final int FIELD_BUTTON_WIDTH = 70;
	private static final int FORM_ROW_HEIGHT = 20;
	private static final int SECTION_GAP = 8;
	private static final int SMALL_GAP = 4;
	private static final int BOTTOM_BUTTON_WIDTH = 140;

	private static final int INVITE_BANNER_Y = TOP_BAR_HEIGHT + 8;
	private static final int INVITE_BANNER_HEIGHT = 24;
	private static final int CONTENT_TOP = TOP_BAR_HEIGHT + 8;

	// Same slot DashboardScreen's admin toggle and AdminIslandDetailScreen's "Ver miembros" button
	// already use — the top bar's reserved right-hand area. Shared by both the PARTY/SHARING pages'
	// "Ver miembros y aliados (N/M)" button and the MEMBERS page's "Invitaciones pendientes (N)"
	// button (only one is ever built at a time, per page — see initTopBar) — widened to 180 (from
	// the "N/M" label's own 170) so the longer "Invitaciones pendientes" label, previously
	// MembersScreen's own dedicated TOP_BAR_ACTION_WIDTH, fits too.
	private static final int TOP_BAR_ACTION_WIDTH = 180;
	private static final int TOP_BAR_ACTION_HEIGHT = 20;

	// Exact same constants/position as SettingsScreen's own pagination row (bottom, not top bar).
	private static final int PAGINATION_ROW_HEIGHT = 20;
	private static final int PAGINATION_BUTTON_WIDTH = 90;
	private static final int CONTENT_BOTTOM_MARGIN = 12;

	// MEMBERS page — same values the old standalone MembersScreen used.
	private static final int MEMBERS_ROW_HEIGHT = 20;
	private static final int MEMBERS_ROW_SPACING = 4;
	private static final int MEMBERS_ACTION_BUTTON_WIDTH = 60;
	private static final int MEMBERS_ACTION_BUTTON_HEIGHT = 16;
	private static final int MEMBERS_FIRST_ROW_Y = TOP_BAR_HEIGHT + 12;
	private static final int MEMBERS_INVITE_ROW_HEIGHT = 20;

	// 0 = no pending disband request. Screen-local UI flow state, not party data.
	private long pendingDisbandExpiresAtMillis = 0L;
	// Persistent across clearAndInit() (a page switch, or any status refresh) — same reasoning as
	// PagedFlagGrid#currentPage: resetting to page 1 on every unrelated rebuild would be jarring.
	private Page page = Page.PARTY;

	private TextFieldWidget createNameField;
	private TextFieldWidget inviteField;
	private TextFieldWidget renameField;
	private TextFieldWidget allyField;

	// MEMBERS page state — same shape as the old standalone MembersScreen.
	private final ScrollableRowList memberList =
			new ScrollableRowList(CONTENT_X, MEMBERS_FIRST_ROW_Y, 1, 1, MEMBERS_ROW_HEIGHT, MEMBERS_ROW_SPACING);
	private final List<IndexedWidget> memberRowWidgets = new ArrayList<>();
	private TextFieldWidget membersInviteField;

	public PartyScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.party.title"), parent);
		ClientPlayNetworking.send(new PartyStatusRequestC2S());
		// Sent once here, not from initContent() — see BiomeScreen's own fix for why: initContent()
		// reruns on every clearAndInit(), including the one refreshFromNetwork() below does when the
		// reply to THIS exact request lands, which would otherwise turn into a self-perpetuating
		// request/rebuild loop.
		ClientPlayNetworking.send(new LocationSharingStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh PartyStatusS2C/LocationSharingStatusS2C lands
	// while this screen is open.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		initTopBar();
		initPagination();

		switch (page) {
			case MEMBERS -> initMembersPage();
			case PARTY -> initPartyPage();
			case SHARING -> initSharingPage();
		}
	}

	// One reserved top-bar-right slot, shared by two mutually-exclusive buttons depending on the
	// current page: "Invitaciones pendientes (N)" on MEMBERS (only when there's an island to have
	// pending invites for — same button the old standalone MembersScreen had), or "Ver miembros y
	// aliados (N/M)" on PARTY/SHARING (unchanged from before this screen absorbed MEMBERS).
	private void initTopBar() {
		if (page == Page.MEMBERS) {
			if (!ClientIslandCache.hasIsland()) {
				return;
			}
			this.addDrawableChild(ButtonWidget.builder(
							Text.translatable("islandcoreclient.members.pending_invites_button", ClientIslandCache.getPendingInvites().size()),
							button -> this.client.setScreen(new PendingInvitesScreen(this)))
					.dimensions(this.width - 8 - TOP_BAR_ACTION_WIDTH, (TOP_BAR_HEIGHT - TOP_BAR_ACTION_HEIGHT) / 2,
							TOP_BAR_ACTION_WIDTH, TOP_BAR_ACTION_HEIGHT)
					.build());
			return;
		}

		boolean hasParty = ClientPartyCache.hasParty();
		boolean hasIsland = ClientIslandCache.hasIsland();
		if (!hasParty && !hasIsland) {
			return;
		}
		int memberCount = hasParty ? ClientPartyCache.getMembers().size() : 0;
		int allyCount = currentAllies().size();
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.party.view_members_button", memberCount, allyCount),
						button -> this.client.setScreen(new PartyMembersScreen(this)))
				.dimensions(this.width - 8 - TOP_BAR_ACTION_WIDTH, (TOP_BAR_HEIGHT - TOP_BAR_ACTION_HEIGHT) / 2,
						TOP_BAR_ACTION_WIDTH, TOP_BAR_ACTION_HEIGHT)
				.build());
	}

	// "<< Anterior" / "Siguiente >>", bottom-left/bottom-right with a centered page indicator (drawn
	// in renderContent) — exact same position/labels as SettingsScreen's PagedFlagGrid pagination.
	private void initPagination() {
		int paginationY = paginationRowY();
		Page[] pages = Page.values();
		int currentIndex = page.ordinal();

		ButtonWidget prevButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.pagination.prev"),
						button -> onPageChanged(pages[currentIndex - 1]))
				.dimensions(CONTENT_X, paginationY, PAGINATION_BUTTON_WIDTH, PAGINATION_ROW_HEIGHT)
				.build());
		prevButton.active = currentIndex > 0;

		ButtonWidget nextButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.pagination.next"),
						button -> onPageChanged(pages[currentIndex + 1]))
				.dimensions(this.width - 16 - PAGINATION_BUTTON_WIDTH, paginationY, PAGINATION_BUTTON_WIDTH, PAGINATION_ROW_HEIGHT)
				.build());
		nextButton.active = currentIndex < pages.length - 1;
	}

	private int paginationRowY() {
		return this.height - CONTENT_BOTTOM_MARGIN - PAGINATION_ROW_HEIGHT;
	}

	private void onPageChanged(Page target) {
		this.page = target;
		this.clearAndInit();
	}

	// Absorbed verbatim from the old standalone MembersScreen, with one addition: when the player has
	// no island, the member list/invite form are simply not built (members stays an empty list,
	// hasIsland-gated widgets are skipped) — renderMembersPage draws a dimmed "no island" message in
	// their place instead, same pattern DashboardScreen already uses for its own no-island state. The
	// screen (and its Party page) stays fully usable either way.
	private void initMembersPage() {
		boolean hasIsland = ClientIslandCache.hasIsland();

		int fieldWidth = 160;
		int buttonWidth = 70;
		int inviteFieldY = this.height - 16 - MEMBERS_INVITE_ROW_HEIGHT;
		int fieldX = this.width / 2 - (fieldWidth + 4 + buttonWidth) / 2;
		int listBottom = inviteFieldY - SECTION_GAP;

		int viewportWidth = this.width - CONTENT_X - 16;
		int viewportHeight = Math.max(MEMBERS_ROW_HEIGHT, listBottom - MEMBERS_FIRST_ROW_Y);
		memberList.setViewport(CONTENT_X, MEMBERS_FIRST_ROW_Y, viewportWidth, viewportHeight);

		List<ClientMemberView> members = hasIsland ? ClientIslandCache.getMembers() : List.of();
		memberList.setItemCount(members.size());
		memberRowWidgets.clear();
		int actionsX = this.width - 16 - MEMBERS_ACTION_BUTTON_WIDTH;
		for (int i = 0; i < members.size(); i++) {
			ClientMemberView member = members.get(i);
			int buttonY = memberList.getRowY(i) + (MEMBERS_ROW_HEIGHT - MEMBERS_ACTION_BUTTON_HEIGHT) / 2;
			boolean rowVisible = memberList.isRowVisible(i);
			if (member.role() == ClientMemberView.Role.MEMBER || member.role() == ClientMemberView.Role.CO_OWNER) {
				addMemberRowButton(trustButtonLabel(member.role() == ClientMemberView.Role.CO_OWNER),
						actionsX - MEMBERS_ACTION_BUTTON_WIDTH - 4, buttonY, rowVisible, i,
						() -> onTrustClicked(member.uuid(), member.role()));
				addMemberRowButton(Text.translatable("islandcoreclient.members.remove"), actionsX, buttonY, rowVisible, i,
						() -> onRemoveClicked(member.uuid()));
			}
		}

		if (hasIsland) {
			this.membersInviteField = new TextFieldWidget(this.textRenderer, fieldX, inviteFieldY, fieldWidth, MEMBERS_INVITE_ROW_HEIGHT,
					Text.translatable("islandcoreclient.members.invite_placeholder"));
			this.membersInviteField.setPlaceholder(Text.translatable("islandcoreclient.members.invite_placeholder"));
			this.membersInviteField.setMaxLength(32);
			this.addDrawableChild(this.membersInviteField);

			this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.invite_button"),
							button -> onMemberInviteClicked())
					.dimensions(fieldX + fieldWidth + 4, inviteFieldY, buttonWidth, MEMBERS_INVITE_ROW_HEIGHT)
					.build());
		}
	}

	// Bold + aqua (same color ClientMemberView.Role.label() uses for CO_OWNER) when the row is
	// currently CO_OWNER, plain otherwise — a highlighted "Trust" button reads as "already trusted",
	// same visual language ToggleRow-style active states already use elsewhere.
	private static Text trustButtonLabel(boolean isCoOwner) {
		Text base = Text.translatable("islandcoreclient.members.trust");
		return isCoOwner ? base.copy().formatted(Formatting.AQUA, Formatting.BOLD) : base;
	}

	private void addMemberRowButton(Text text, int x, int y, boolean rowVisible, int rowIndex, Runnable onClick) {
		ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(text, b -> onClick.run())
				.dimensions(x, y, MEMBERS_ACTION_BUTTON_WIDTH, MEMBERS_ACTION_BUTTON_HEIGHT)
				.build());
		button.visible = rowVisible;
		button.active = rowVisible;
		memberRowWidgets.add(new IndexedWidget(rowIndex, button));
	}

	// Purely top-down: invite/rename (leader only) -> add-ally (hasIsland only) -> leave/disband row
	// — content ends well above the pagination row even in the leader+hasIsland worst case, so no
	// bottom-up anchoring is needed here (unlike the earlier 3-page design's INFO page) — see the
	// class javadoc's pixel-math note for the exact numbers.
	private int bottomRowY(boolean isLeader, boolean hasIsland) {
		int fieldY = CONTENT_TOP + LINE_HEIGHT + SECTION_GAP;
		if (isLeader) {
			fieldY += 2 * (FORM_ROW_HEIGHT + SECTION_GAP); // invite + rename
		}
		if (hasIsland) {
			fieldY += FORM_ROW_HEIGHT + SECTION_GAP; // add-ally
		}
		return fieldY;
	}

	private void initPartyPage() {
		if (!ClientPartyCache.hasParty()) {
			initNoPartyContent();
			return;
		}

		boolean isLeader = isLocalPlayerLeader();
		boolean hasIsland = ClientIslandCache.hasIsland();
		int fieldY = CONTENT_TOP + LINE_HEIGHT + SECTION_GAP;

		if (isLeader) {
			this.inviteField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
					Text.translatable("islandcoreclient.party.invite_placeholder"));
			this.inviteField.setPlaceholder(Text.translatable("islandcoreclient.party.invite_placeholder"));
			this.inviteField.setMaxLength(32);
			this.addDrawableChild(this.inviteField);
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.invite_button"),
							button -> onInviteClicked())
					.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
					.build());
			fieldY += FORM_ROW_HEIGHT + SECTION_GAP;

			this.renameField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
					Text.translatable("islandcoreclient.party.rename_placeholder"));
			this.renameField.setPlaceholder(Text.translatable("islandcoreclient.party.rename_placeholder"));
			this.renameField.setMaxLength(32);
			this.addDrawableChild(this.renameField);
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.rename_button"),
							button -> onRenameClicked())
					.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
					.build());
			fieldY += FORM_ROW_HEIGHT + SECTION_GAP;
		}

		if (hasIsland) {
			initAllyForm(fieldY);
			fieldY += FORM_ROW_HEIGHT + SECTION_GAP;
		}

		int rowY = fieldY;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.leave_button"),
						button -> onLeaveClicked())
				.dimensions(CONTENT_X, rowY, BOTTOM_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());

		if (isLeader) {
			boolean pending = pendingDisbandExpiresAtMillis > 0;
			ButtonWidget disbandButton = this.addDrawableChild(ButtonWidget.builder(
							(pending
									? Text.translatable("islandcoreclient.party.disband_confirm_button")
									: Text.translatable("islandcoreclient.party.disband_request_button"))
									.formatted(Formatting.RED),
							button -> onDisbandClicked())
					.dimensions(this.width - 16 - BOTTOM_BUTTON_WIDTH, rowY, BOTTOM_BUTTON_WIDTH, FORM_ROW_HEIGHT)
					.build());
			disbandButton.active = true;
		}
	}

	private void initAllyForm(int fieldY) {
		this.allyField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.ally_add_placeholder"));
		this.allyField.setPlaceholder(Text.translatable("islandcoreclient.party.ally_add_placeholder"));
		this.allyField.setMaxLength(32);
		this.addDrawableChild(this.allyField);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.ally_add_button"),
						button -> onAllyAddClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());
	}

	private void initNoPartyContent() {
		ClientIncomingPartyInviteView invite = ClientPartyCache.getIncomingInvite();
		int fieldY;
		if (invite != null) {
			int buttonY = INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - 16) / 2;
			int ignoreX = this.width - 16 - 66;
			int acceptX = ignoreX - 4 - 66;
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.invite_accept"),
							button -> onAcceptInviteClicked())
					.dimensions(acceptX, buttonY, 66, 16)
					.build());
			// No decline call on the wire, same as the island invite banner: ignoring is local-only,
			// the invite simply expires server-side on its own.
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.invite_ignore"),
							button -> ClientPartyCache.setIncomingInvite(null))
					.dimensions(ignoreX, buttonY, 66, 16)
					.build());
			fieldY = INVITE_BANNER_Y + INVITE_BANNER_HEIGHT + 16;
		} else {
			fieldY = CONTENT_TOP + LINE_HEIGHT + SECTION_GAP;
		}

		this.createNameField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.create_name_placeholder"));
		this.createNameField.setPlaceholder(Text.translatable("islandcoreclient.party.create_name_placeholder"));
		this.createNameField.setMaxLength(32);
		this.addDrawableChild(this.createNameField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.create_button"),
						button -> onCreateClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());

		// Ally management is island-scoped, not party-scoped — still offered here with no party at
		// all, same as it always has been.
		if (ClientIslandCache.hasIsland()) {
			initAllyForm(fieldY + FORM_ROW_HEIGHT + SECTION_GAP);
		}
	}

	private static List<ClientMemberView> currentAllies() {
		return ClientIslandCache.getMembers().stream().filter(member -> member.role() == ClientMemberView.Role.ALLY).toList();
	}

	// Page 2: the 4 independent location-sharing toggles (see PlayerLocationSharingConfig
	// server-side) plus [DEBUG] — always shown regardless of party/island state (toggling "share
	// with my party" with no party, or "share with my allies" with no island, is simply inert
	// server-side, not an error). Alone on its own page, plenty of fixed top-down room.
	private void initSharingPage() {
		int toggleWidth = (this.width - CONTENT_X - 16 - SMALL_GAP) / 2;
		int y = CONTENT_TOP;

		this.addDrawableChild(new ToggleRow(CONTENT_X, y, toggleWidth, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.send_to_party_label"),
				ClientLocationSharingCache.isSendPositionToPartyEnabled(), true, this::onSendToPartyToggled));
		this.addDrawableChild(new ToggleRow(CONTENT_X + toggleWidth + SMALL_GAP, y, toggleWidth, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.receive_from_party_label"),
				ClientLocationSharingCache.isReceivePositionsFromPartyEnabled(), true, this::onReceiveFromPartyToggled));
		y += FORM_ROW_HEIGHT + SMALL_GAP;

		this.addDrawableChild(new ToggleRow(CONTENT_X, y, toggleWidth, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.send_to_allies_label"),
				ClientLocationSharingCache.isSendPositionToAlliesEnabled(), true, this::onSendToAlliesToggled));
		this.addDrawableChild(new ToggleRow(CONTENT_X + toggleWidth + SMALL_GAP, y, toggleWidth, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.receive_from_allies_label"),
				ClientLocationSharingCache.isReceivePositionsFromAlliesEnabled(), true, this::onReceiveFromAlliesToggled));
		y += FORM_ROW_HEIGHT + SECTION_GAP;

		this.addDrawableChild(ButtonWidget.builder(
						ClientAllyLocationsCache.hasDebugEntry()
								? Text.translatable("islandcoreclient.party.debug_remove_button")
								: Text.translatable("islandcoreclient.party.debug_add_button"),
						button -> onDebugToggleClicked())
				.dimensions(CONTENT_X, y, FIELD_WIDTH, FORM_ROW_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (pendingDisbandExpiresAtMillis > 0 && System.currentTimeMillis() >= pendingDisbandExpiresAtMillis) {
			pendingDisbandExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		Page[] pages = Page.values();
		Text indicator = Text.translatable("islandcoreclient.pagination.page_indicator", page.ordinal() + 1, pages.length);
		int indicatorWidth = this.textRenderer.getWidth(indicator);
		context.drawTextWithShadow(this.textRenderer, indicator, this.width / 2 - indicatorWidth / 2,
				paginationRowY() + (PAGINATION_ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xAAAAAA);

		if (page == Page.MEMBERS) {
			renderMembersPage(context);
		} else if (page == Page.PARTY) {
			renderPartyPage(context);
		}
	}

	// Scrolling only matters on the MEMBERS page's list — same guard/delegation MembersScreen used.
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (page == Page.MEMBERS && memberList.isMouseOver(mouseX, mouseY)) {
			memberList.scroll(verticalAmount);
			for (IndexedWidget iw : memberRowWidgets) {
				boolean visible = memberList.isRowVisible(iw.rowIndex());
				iw.widget().setY(memberList.getRowY(iw.rowIndex()));
				iw.widget().visible = visible;
				iw.widget().active = visible;
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// Dimmed "no island" message, same color DashboardScreen's own no-island member rows use,
	// instead of an empty list with no explanation — the Party half of this screen stays fully
	// functional regardless, so this section alone goes quiet rather than the whole screen.
	private void renderMembersPage(DrawContext context) {
		if (!ClientIslandCache.hasIsland()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.reason.no_island"), CONTENT_X, MEMBERS_FIRST_ROW_Y, 0x777777);
			return;
		}

		List<ClientMemberView> members = ClientIslandCache.getMembers();
		memberList.startClip(context);
		for (int i = 0; i < members.size(); i++) {
			if (!memberList.isRowVisible(i)) {
				continue;
			}
			ClientMemberView member = members.get(i);
			Text line = Text.literal(member.name() + " ").append(member.role().label());
			int y = memberList.getRowY(i);
			context.drawTextWithShadow(this.textRenderer, line, CONTENT_X, y + (MEMBERS_ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
		}
		memberList.endClip(context);
		memberList.renderScrollbar(context);
	}

	// MemberTrustC2S toggles by the target's CURRENT role server-side (see
	// MembershipService#toggleCoOwner), so currentRole (captured at click time) is what decides the
	// optimistic new role here too.
	private void onTrustClicked(UUID uuid, ClientMemberView.Role currentRole) {
		ClientMemberView.Role newRole = currentRole == ClientMemberView.Role.CO_OWNER
				? ClientMemberView.Role.MEMBER : ClientMemberView.Role.CO_OWNER;
		ClientPlayNetworking.send(new MemberTrustC2S(uuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientIslandCache.setMemberRole(uuid, newRole);
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onRemoveClicked(UUID uuid) {
		ClientPlayNetworking.send(new MemberRemoveC2S(uuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientIslandCache.removeMember(uuid);
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onMemberInviteClicked() {
		String targetName = this.membersInviteField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new MemberInviteC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// The real expiry (5 minutes) comes back on the next snapshot refresh; refetch now
				// instead of guessing it locally.
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void renderPartyPage(DrawContext context) {
		if (!ClientPartyCache.hasParty()) {
			ClientIncomingPartyInviteView invite = ClientPartyCache.getIncomingInvite();
			if (invite != null) {
				context.fill(16, INVITE_BANNER_Y, this.width - 16, INVITE_BANNER_Y + INVITE_BANNER_HEIGHT, 0xC0224488);
				context.drawTextWithShadow(this.textRenderer,
						Text.translatable("islandcoreclient.party.invite_banner", invite.inviterName(), invite.partyName()),
						20, INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			} else {
				context.drawTextWithShadow(this.textRenderer,
						Text.translatable("islandcoreclient.party.no_party"), CONTENT_X, CONTENT_TOP, 0xAAAAAA);
			}
			return;
		}

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.header", ClientPartyCache.getName(), ClientPartyCache.getLeaderName())
						.formatted(Formatting.BOLD), CONTENT_X, CONTENT_TOP, 0xFFFFFF);

		boolean isLeader = isLocalPlayerLeader();
		if (isLeader && pendingDisbandExpiresAtMillis > 0) {
			long remaining = Math.max(0L, (pendingDisbandExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			// Below the leave/disband row, full width — NOT above the (narrow, 140px) Disolver
			// button column: this string is ~170px+ rendered, too wide to fit stacked there without
			// running past the window edge or into the ally-form column on the left.
			int rowY = bottomRowY(true, ClientIslandCache.hasIsland());
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.disband_pending", remaining).formatted(Formatting.RED),
					CONTENT_X, rowY + FORM_ROW_HEIGHT + 4, 0xFFFFFF);
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

	private void onCreateClicked() {
		String name = this.createNameField.getText().trim();
		if (name.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PartyCreateC2S(name));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onAcceptInviteClicked() {
		ClientPlayNetworking.send(new PartyAcceptC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			ClientPartyCache.setIncomingInvite(null);
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onInviteClicked() {
		String targetName = this.inviteField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PartyInviteC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onRenameClicked() {
		String newName = this.renameField.getText().trim();
		if (newName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PartyRenameC2S(newName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onLeaveClicked() {
		ClientPlayNetworking.send(new PartyLeaveC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onDisbandClicked() {
		if (pendingDisbandExpiresAtMillis > 0) {
			confirmDisband();
			return;
		}
		ClientPlayNetworking.send(new PartyDisbandRequestC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				pendingDisbandExpiresAtMillis = System.currentTimeMillis() + DISBAND_CONFIRM_WINDOW_SECONDS * 1000L;
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void confirmDisband() {
		ClientPlayNetworking.send(new PartyDisbandConfirmC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			pendingDisbandExpiresAtMillis = 0L;
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	// An ally doesn't have to already be a member of anything — same "type a name, resolve
	// server-side" flow the invite field uses.
	private void onAllyAddClicked() {
		String targetName = this.allyField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new MemberAllyAddC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// The real uuid/name for the new ALLY entry comes back on the next island snapshot —
				// ClientIslandCache stays the single source of truth for it, not guessed locally here.
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	// ToggleRow already flipped itself optimistically before each of these runs — same pattern
	// SettingsScreen's flag toggles use. LocationSharingSetC2S always carries all four fields, so the
	// other three are read straight from the cache rather than assumed unchanged.
	private void onSendToPartyToggled(boolean newValue) {
		boolean previous = ClientLocationSharingCache.isSendPositionToPartyEnabled();
		ClientLocationSharingCache.setSendPositionToParty(newValue);
		sendLocationSharingUpdate(previous, ClientLocationSharingCache::setSendPositionToParty);
	}

	private void onReceiveFromPartyToggled(boolean newValue) {
		boolean previous = ClientLocationSharingCache.isReceivePositionsFromPartyEnabled();
		ClientLocationSharingCache.setReceivePositionsFromParty(newValue);
		sendLocationSharingUpdate(previous, ClientLocationSharingCache::setReceivePositionsFromParty);
	}

	private void onSendToAlliesToggled(boolean newValue) {
		boolean previous = ClientLocationSharingCache.isSendPositionToAlliesEnabled();
		ClientLocationSharingCache.setSendPositionToAllies(newValue);
		sendLocationSharingUpdate(previous, ClientLocationSharingCache::setSendPositionToAllies);
	}

	private void onReceiveFromAlliesToggled(boolean newValue) {
		boolean previous = ClientLocationSharingCache.isReceivePositionsFromAlliesEnabled();
		ClientLocationSharingCache.setReceivePositionsFromAllies(newValue);
		sendLocationSharingUpdate(previous, ClientLocationSharingCache::setReceivePositionsFromAllies);
	}

	@FunctionalInterface
	private interface Revert {
		void revert(boolean previousValue);
	}

	private void sendLocationSharingUpdate(boolean previousValueForRevert, Revert revert) {
		ClientPlayNetworking.send(new LocationSharingSetC2S(
				ClientLocationSharingCache.isSendPositionToPartyEnabled(),
				ClientLocationSharingCache.isReceivePositionsFromPartyEnabled(),
				ClientLocationSharingCache.isSendPositionToAlliesEnabled(),
				ClientLocationSharingCache.isReceivePositionsFromAlliesEnabled()));
		PendingActionTracker.await((success, reasonKey) -> {
			if (!success) {
				revert.revert(previousValueForRevert);
				ClientErrorToasts.showReason(reasonKey);
				this.clearAndInit();
			}
		});
	}

	// [DEBUG] Injects (or removes) a fake ally DEBUG_NORTH_OFFSET blocks north of the local player
	// into ClientAllyLocationsCache, entirely client-side (no packet sent) — lets AllyHudRenderer's
	// on-screen/off-screen projection be exercised solo, without a second connected player actually
	// sharing their position. Relocated verbatim from the old AllianceScreen.
	private void onDebugToggleClicked() {
		if (ClientAllyLocationsCache.hasDebugEntry()) {
			ClientAllyLocationsCache.setDebugEntry(null);
		} else {
			PlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) {
				ClientAllyLocationsCache.setDebugEntry(new ClientAllyLocationView(
						UUID.randomUUID(), "DEBUG", player.getX(), player.getY(), player.getZ() - DEBUG_NORTH_OFFSET));
			}
		}
		this.clearAndInit();
	}
}
