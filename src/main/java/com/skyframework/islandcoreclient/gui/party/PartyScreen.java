package com.skyframework.islandcoreclient.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ScrollableRowList;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.party.PartyAcceptC2S;
import com.skyframework.islandcoreclient.network.party.PartyAllyAddC2S;
import com.skyframework.islandcoreclient.network.party.PartyAllyRemoveC2S;
import com.skyframework.islandcoreclient.network.party.PartyCreateC2S;
import com.skyframework.islandcoreclient.network.party.PartyDisbandConfirmC2S;
import com.skyframework.islandcoreclient.network.party.PartyDisbandRequestC2S;
import com.skyframework.islandcoreclient.network.party.PartyInviteC2S;
import com.skyframework.islandcoreclient.network.party.PartyKickC2S;
import com.skyframework.islandcoreclient.network.party.PartyLeaveC2S;
import com.skyframework.islandcoreclient.network.party.PartyRenameC2S;
import com.skyframework.islandcoreclient.network.party.PartyStatusRequestC2S;
import com.skyframework.islandcoreclient.state.ClientAlliedPartyView;
import com.skyframework.islandcoreclient.state.ClientIncomingPartyInviteView;
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
 * Independent of any island — reachable from the Dashboard whenever the connection is up,
 * regardless of {@code hasIsland} (see DashboardScreen's party button wiring), and directly via
 * {@link com.skyframework.islandcoreclient.keybind.OpenPartyKeybind} (default key P) or
 * {@code /islandparty}. Reads/writes exclusively through {@link ClientPartyCache}, never
 * {@code ClientIslandCache}.
 *
 * <p>Two states: no party (create form, plus an incoming-invite banner if one is pending) or has a
 * party (member/allied-party lists — each independently scrollable via {@link ScrollableRowList}
 * so a large party or many alliances never pushes the leader forms or the leave/disband row below
 * the screen — plus leader-only controls: invite, rename, disband with a lightweight 15s confirm
 * mirroring {@code DeleteIslandScreen}'s pattern at a fifth the timeout, and ally add/remove).
 * The leader forms and the leave/disband row are always anchored at a fixed position near the
 * bottom of the screen, computed from the bottom up, so they can never be pushed off-screen by
 * list content — see {@link #computeLayout()}.
 */
public class PartyScreen extends BaseMenuScreen {
	// Mirrors the server's PartyDisbandRequests.TIMEOUT — purely for the local countdown display;
	// the server is the actual authority on whether a confirm still lands within the window.
	private static final long DISBAND_CONFIRM_WINDOW_SECONDS = 15L;

	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int FIELD_WIDTH = 150;
	private static final int FIELD_BUTTON_WIDTH = 70;
	private static final int FORM_ROW_HEIGHT = 20;
	private static final int FORM_ROW_GAP = 4;
	private static final int SECTION_GAP = 8;
	// Members get the larger share of the split between the two scrollable lists.
	private static final double MEMBER_SHARE = 0.55;

	private static final int INVITE_BANNER_Y = TOP_BAR_HEIGHT + 8;
	private static final int INVITE_BANNER_HEIGHT = 24;

	private record IndexedWidget(int rowIndex, ClickableWidget widget) {
	}

	// All Y positions for the has-party state, computed bottom-up so the leader forms and the
	// leave/disband row are always anchored at a fixed spot near the bottom regardless of how much
	// member/allied-party content there is above them.
	private record Layout(
			int headerY, int membersHeadingY, int memberListTop, int memberViewportHeight,
			int alliesHeadingY, int alliedListTop, int alliedViewportHeight,
			int leaderFormsTop, int bottomButtonY, boolean pendingDisband, boolean isLeader
	) {
	}

	// 0 = no pending disband request. Screen-local UI flow state, not party data.
	private long pendingDisbandExpiresAtMillis = 0L;

	private TextFieldWidget createNameField;
	private TextFieldWidget inviteField;
	private TextFieldWidget renameField;
	private TextFieldWidget allyField;

	private final ScrollableRowList memberList = new ScrollableRowList(CONTENT_X, 0, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final ScrollableRowList alliedList = new ScrollableRowList(CONTENT_X, 0, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final List<IndexedWidget> memberRowWidgets = new ArrayList<>();
	private final List<IndexedWidget> alliedRowWidgets = new ArrayList<>();

	public PartyScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.party.title"), parent);
		ClientPlayNetworking.send(new PartyStatusRequestC2S());
	}

	// Called by ClientPacketHandlers when a fresh PartyStatusS2C lands while this screen is open.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void initContent() {
		if (!ClientPartyCache.hasParty()) {
			initNoPartyContent();
			return;
		}
		initHasPartyContent();
	}

	private void initNoPartyContent() {
		ClientIncomingPartyInviteView invite = ClientPartyCache.getIncomingInvite();
		int formY = invite != null ? INVITE_BANNER_Y + INVITE_BANNER_HEIGHT + 16 : TOP_BAR_HEIGHT + 24;

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
		}

		this.createNameField = new TextFieldWidget(this.textRenderer, CONTENT_X, formY, FIELD_WIDTH, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.create_name_placeholder"));
		this.createNameField.setPlaceholder(Text.translatable("islandcoreclient.party.create_name_placeholder"));
		this.createNameField.setMaxLength(32);
		this.addDrawableChild(this.createNameField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.create_button"),
						button -> onCreateClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, formY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());
	}

	// Bottom-up: reserve the leave/disband row, then (if leader) the disband countdown line and the
	// 3 leader-form rows above it, then a gap — whatever vertical space remains above that goes to
	// the two scrollable lists, split MEMBER_SHARE/rest between them. Depends only on screen size
	// and isLeader/pendingDisband, not on list content, so it's safe to call from both initContent
	// and renderContent and always get matching Y positions.
	private Layout computeLayout() {
		boolean isLeader = isLocalPlayerLeader();
		boolean pendingDisband = isLeader && pendingDisbandExpiresAtMillis > 0;

		int bottomButtonY = this.height - 16 - FORM_ROW_HEIGHT;
		int cursor = bottomButtonY;
		if (pendingDisband) {
			cursor -= LINE_HEIGHT + 4;
		}
		int leaderFormsTop = cursor;
		if (isLeader) {
			int formsHeight = 3 * FORM_ROW_HEIGHT + 2 * FORM_ROW_GAP;
			cursor -= formsHeight;
			leaderFormsTop = cursor;
			cursor -= SECTION_GAP;
		}
		int listsBottom = cursor;

		int headerY = TOP_BAR_HEIGHT + 8;
		int membersHeadingY = headerY + LINE_HEIGHT + 6;
		int memberListTop = membersHeadingY + LINE_HEIGHT + 6;

		int totalListsHeight = Math.max(0, listsBottom - memberListTop);
		int alliesHeadingReserve = LINE_HEIGHT + 6;
		int splittable = Math.max(0, totalListsHeight - alliesHeadingReserve);
		int memberViewportHeight = Math.max(ROW_HEIGHT, (int) (splittable * MEMBER_SHARE));
		int alliesHeadingY = memberListTop + memberViewportHeight + 6;
		int alliedListTop = alliesHeadingY + LINE_HEIGHT + 6;
		int alliedViewportHeight = Math.max(ROW_HEIGHT, listsBottom - alliedListTop);

		return new Layout(headerY, membersHeadingY, memberListTop, memberViewportHeight,
				alliesHeadingY, alliedListTop, alliedViewportHeight,
				leaderFormsTop, bottomButtonY, pendingDisband, isLeader);
	}

	private void initHasPartyContent() {
		Layout layout = computeLayout();
		int viewportWidth = this.width - CONTENT_X - 16;
		memberList.setViewport(CONTENT_X, layout.memberListTop(), viewportWidth, layout.memberViewportHeight());
		alliedList.setViewport(CONTENT_X, layout.alliedListTop(), viewportWidth, layout.alliedViewportHeight());

		int actionsX = this.width - 16 - ACTION_BUTTON_WIDTH;
		UUID localUuid = localPlayerUuid();

		List<ClientPartyMemberView> members = ClientPartyCache.getMembers();
		memberList.setItemCount(members.size());
		memberRowWidgets.clear();
		for (int i = 0; i < members.size(); i++) {
			ClientPartyMemberView member = members.get(i);
			if (layout.isLeader() && localUuid != null && !member.uuid().equals(localUuid)) {
				int buttonY = memberList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
				boolean rowVisible = memberList.isRowVisible(i);
				ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.kick"),
								b -> onKickClicked(member.uuid()))
						.dimensions(actionsX, buttonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
				button.visible = rowVisible;
				button.active = rowVisible;
				memberRowWidgets.add(new IndexedWidget(i, button));
			}
		}

		List<ClientAlliedPartyView> allied = ClientPartyCache.getAlliedParties();
		alliedList.setItemCount(allied.size());
		alliedRowWidgets.clear();
		for (int i = 0; i < allied.size(); i++) {
			ClientAlliedPartyView party = allied.get(i);
			if (layout.isLeader()) {
				int buttonY = alliedList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
				boolean rowVisible = alliedList.isRowVisible(i);
				ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.ally_remove"),
								b -> onAllyRemoveClicked(party.name()))
						.dimensions(actionsX, buttonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
				button.visible = rowVisible;
				button.active = rowVisible;
				alliedRowWidgets.add(new IndexedWidget(i, button));
			}
		}

		if (layout.isLeader()) {
			initLeaderForms(layout.leaderFormsTop());
		}

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.leave_button"),
						button -> onLeaveClicked())
				.dimensions(CONTENT_X, layout.bottomButtonY(), 140, FORM_ROW_HEIGHT)
				.build());

		if (layout.isLeader()) {
			boolean pending = pendingDisbandExpiresAtMillis > 0;
			ButtonWidget disbandButton = this.addDrawableChild(ButtonWidget.builder(
							(pending
									? Text.translatable("islandcoreclient.party.disband_confirm_button")
									: Text.translatable("islandcoreclient.party.disband_request_button"))
									.formatted(Formatting.RED),
							button -> onDisbandClicked())
					.dimensions(this.width - 16 - 140, layout.bottomButtonY(), 140, FORM_ROW_HEIGHT)
					.build());
			disbandButton.active = true;
		}
	}

	private void initLeaderForms(int fieldY) {
		this.inviteField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.invite_placeholder"));
		this.inviteField.setPlaceholder(Text.translatable("islandcoreclient.party.invite_placeholder"));
		this.inviteField.setMaxLength(32);
		this.addDrawableChild(this.inviteField);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.invite_button"),
						button -> onInviteClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());
		fieldY += FORM_ROW_HEIGHT + FORM_ROW_GAP;

		this.renameField = new TextFieldWidget(this.textRenderer, CONTENT_X, fieldY, FIELD_WIDTH, FORM_ROW_HEIGHT,
				Text.translatable("islandcoreclient.party.rename_placeholder"));
		this.renameField.setPlaceholder(Text.translatable("islandcoreclient.party.rename_placeholder"));
		this.renameField.setMaxLength(32);
		this.addDrawableChild(this.renameField);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.party.rename_button"),
						button -> onRenameClicked())
				.dimensions(CONTENT_X + FIELD_WIDTH + 8, fieldY, FIELD_BUTTON_WIDTH, FORM_ROW_HEIGHT)
				.build());
		fieldY += FORM_ROW_HEIGHT + FORM_ROW_GAP;

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

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (ClientPartyCache.hasParty()) {
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
		if (!ClientPartyCache.hasParty()) {
			renderNoPartyContent(context);
			return;
		}
		renderHasPartyContent(context);
	}

	private void renderNoPartyContent(DrawContext context) {
		ClientIncomingPartyInviteView invite = ClientPartyCache.getIncomingInvite();
		if (invite != null) {
			context.fill(16, INVITE_BANNER_Y, this.width - 16, INVITE_BANNER_Y + INVITE_BANNER_HEIGHT, 0xC0224488);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.invite_banner", invite.inviterName(), invite.partyName()),
					20, INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
		} else {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.no_party"), CONTENT_X, TOP_BAR_HEIGHT + 8, 0xAAAAAA);
		}
	}

	private void renderHasPartyContent(DrawContext context) {
		if (pendingDisbandExpiresAtMillis > 0 && System.currentTimeMillis() >= pendingDisbandExpiresAtMillis) {
			pendingDisbandExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		Layout layout = computeLayout();

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.header", ClientPartyCache.getName(), ClientPartyCache.getLeaderName())
						.formatted(Formatting.BOLD), CONTENT_X, layout.headerY(), 0xFFFFFF);

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.members_heading", ClientPartyCache.getMembers().size())
						.formatted(Formatting.BOLD, Formatting.GOLD), CONTENT_X, layout.membersHeadingY(), 0xFFFFFF);

		List<ClientPartyMemberView> members = ClientPartyCache.getMembers();
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
			context.drawTextWithShadow(this.textRenderer, nameLine, CONTENT_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
		}
		memberList.endClip(context);
		memberList.renderScrollbar(context);

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.party.allies_heading").formatted(Formatting.BOLD, Formatting.YELLOW), CONTENT_X, layout.alliesHeadingY(), 0xFFFFFF);

		List<ClientAlliedPartyView> allied = ClientPartyCache.getAlliedParties();
		if (allied.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.allies_empty"), CONTENT_X, layout.alliedListTop(), 0xAAAAAA);
		} else {
			alliedList.startClip(context);
			for (int i = 0; i < allied.size(); i++) {
				if (!alliedList.isRowVisible(i)) {
					continue;
				}
				ClientAlliedPartyView party = allied.get(i);
				int y = alliedList.getRowY(i);
				context.drawTextWithShadow(this.textRenderer, Text.literal(party.name()), CONTENT_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			}
			alliedList.endClip(context);
			alliedList.renderScrollbar(context);
		}

		if (layout.isLeader() && pendingDisbandExpiresAtMillis > 0) {
			long remaining = Math.max(0L, (pendingDisbandExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.party.disband_pending", remaining).formatted(Formatting.RED),
					CONTENT_X, layout.bottomButtonY() - LINE_HEIGHT - 4, 0xFFFFFF);
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

	private void onAllyAddClicked() {
		String targetPartyName = this.allyField.getText().trim();
		if (targetPartyName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PartyAllyAddC2S(targetPartyName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onAllyRemoveClicked(String targetPartyName) {
		ClientPlayNetworking.send(new PartyAllyRemoveC2S(targetPartyName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new PartyStatusRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
