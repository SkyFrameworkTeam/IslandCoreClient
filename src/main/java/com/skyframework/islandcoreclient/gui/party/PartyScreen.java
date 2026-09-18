package com.skyframework.islandcoreclient.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ToggleRow;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.alliance.LocationSharingSetC2S;
import com.skyframework.islandcoreclient.network.alliance.LocationSharingStatusRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.member.MemberAllyAddC2S;
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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * Independent of any island for the party half of this screen — reachable via
 * {@link com.skyframework.islandcoreclient.keybind.OpenPartyKeybind} (default key P) or
 * {@code /islandparty} regardless of {@code ClientIslandCache.hasIsland()}. As of the "alianzas"
 * consolidation sprint, this screen ALSO hosts individual-player alliance management (an island
 * concept — {@code IslandRole.ALLY}, see {@code ClientIslandCache}) and all four location-sharing
 * toggles, replacing the retired AllianceScreen/Dashboard alliance tab entirely.
 *
 * <p>Back down to 2 pages (see {@link Page}) after a 3-page detour: splitting Aliados onto its own
 * page turned out to be more separation than needed once the member LIST itself (the actually
 * unbounded part) already moved out to {@link PartyMembersScreen} — the compact add-ally FORM
 * (2 widgets, fixed height) fits comfortably back on Página 1 alongside invite/rename. That screen
 * now shows members AND allies together (same split {@code AdminIslandDetailScreen} uses for
 * {@code AdminIslandMembersScreen}), reached via a top-bar "Ver miembros y aliados (N/M)" button in
 * the same reserved right-hand slot.
 *
 * <p>Navigation: "&lt;&lt; Anterior" / "Siguiente &gt;&gt;" at the BOTTOM, left/right-aligned with a
 * centered page indicator between them — the exact same {@code islandcoreclient.pagination.*}
 * labels AND position {@code SettingsScreen}'s {@code PagedFlagGrid} pagination row already uses.
 * {@link #page} is a plain persistent field surviving {@link #clearAndInit()} the same way
 * {@code PagedFlagGrid#currentPage} does.
 *
 * <p><b>Page 1 (PARTY)</b>: party name/leader, invite field+button (leader only), rename field+
 * button (leader only), add-ally field+button (gated on {@code ClientIslandCache.hasIsland()},
 * independent of party state/leadership), then "Salir de la party" / "Disolver party" side by side
 * in the SAME row (leader-only for Disolver). <b>Page 2 (SHARING)</b>: the 4 location-sharing
 * toggles + [DEBUG], unchanged.
 */
public class PartyScreen extends BaseMenuScreen {
	private enum Page {
		PARTY, SHARING
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
	// already use — the top bar's reserved right-hand area. Widened from that button's 150 to fit
	// the combined "N/M" label comfortably.
	private static final int TOP_BAR_ACTION_WIDTH = 170;
	private static final int TOP_BAR_ACTION_HEIGHT = 20;

	// Exact same constants/position as SettingsScreen's own pagination row (bottom, not top bar).
	private static final int PAGINATION_ROW_HEIGHT = 20;
	private static final int PAGINATION_BUTTON_WIDTH = 90;
	private static final int CONTENT_BOTTOM_MARGIN = 12;

	// 0 = no pending disband request. Screen-local UI flow state, not party data.
	private long pendingDisbandExpiresAtMillis = 0L;
	// Persistent across clearAndInit() (a page switch, or any status refresh) — same reasoning as
	// PagedFlagGrid#currentPage: resetting to page 1 on every unrelated rebuild would be jarring.
	private Page page = Page.PARTY;

	private TextFieldWidget createNameField;
	private TextFieldWidget inviteField;
	private TextFieldWidget renameField;
	private TextFieldWidget allyField;

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
			case PARTY -> initPartyPage();
			case SHARING -> initSharingPage();
		}
	}

	// "Ver miembros y aliados (N/M)" — same top-bar reserved slot AdminIslandDetailScreen's own
	// view-members button uses, present on both pages (shared top-bar content). Shown whenever
	// there's anything to view on either list: a party (even with 0 other members) OR an island
	// (for its allies) — the merged PartyMembersScreen no longer requires a party to be useful.
	private void initTopBar() {
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

		if (page == Page.PARTY) {
			renderPartyPage(context);
		}
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
