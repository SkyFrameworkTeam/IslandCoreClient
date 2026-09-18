package com.skyframework.islandcoreclient.gui.island;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ScrollableRowList;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteC2S;
import com.skyframework.islandcoreclient.network.member.MemberRemoveC2S;
import com.skyframework.islandcoreclient.network.member.MemberTrustC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The member list scrolls inside its own fixed viewport ({@link ScrollableRowList}) so an island
 * with many members never pushes the invite form below the screen or off the clickable area — it
 * stays pinned near the bottom.
 *
 * <p>Pending (outgoing) invites moved out into their own screen ({@link PendingInvitesScreen}, same
 * split {@code AdminIslandDetailScreen} uses for {@code AdminIslandMembersScreen}), reached via a
 * top-bar "Invitaciones pendientes (N)" button — they used to share this screen in a fixed 40%-height
 * split (see the old {@code MEMBER_SHARE}) that stayed mostly empty whenever there were 0-1 pending
 * invites, the common case, while also capping how many members could show without scrolling.
 *
 * <p>Adding/removing an individual-player ALLY (IslandRole.ALLY) is managed from PartyScreen's own
 * Página 1 instead of here (see the "alianzas" consolidation sprint) — an ALLY row still shows up in
 * the member list below like any other relationship, just without its own action button, the same as
 * a VISITOR/DENIED row already has none.
 */
public class MembersScreen extends BaseMenuScreen {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int NAME_X = 16;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int FIRST_ROW_Y = TOP_BAR_HEIGHT + 12;
	private static final int INVITE_ROW_HEIGHT = 20;
	private static final int SECTION_GAP = 8;

	// Same reserved top-bar slot AdminIslandDetailScreen's "Ver miembros" button and PartyScreen's
	// "Ver miembros y aliados" button already use — widened for this specific label's length.
	private static final int TOP_BAR_ACTION_WIDTH = 180;
	private static final int TOP_BAR_ACTION_HEIGHT = 20;

	private record IndexedWidget(int rowIndex, ClickableWidget widget) {
	}

	private TextFieldWidget inviteField;

	private final ScrollableRowList memberList = new ScrollableRowList(NAME_X, FIRST_ROW_Y, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final List<IndexedWidget> memberRowWidgets = new ArrayList<>();

	public MembersScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.members.title"), parent);
	}

	@Override
	protected void initContent() {
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.members.pending_invites_button", ClientIslandCache.getPendingInvites().size()),
						button -> this.client.setScreen(new PendingInvitesScreen(this)))
				.dimensions(this.width - 8 - TOP_BAR_ACTION_WIDTH, (TOP_BAR_HEIGHT - TOP_BAR_ACTION_HEIGHT) / 2,
						TOP_BAR_ACTION_WIDTH, TOP_BAR_ACTION_HEIGHT)
				.build());

		int fieldWidth = 160;
		int buttonWidth = 70;
		int inviteFieldY = this.height - 16 - INVITE_ROW_HEIGHT;
		int fieldX = this.width / 2 - (fieldWidth + 4 + buttonWidth) / 2;
		int listBottom = inviteFieldY - SECTION_GAP;

		int viewportWidth = this.width - NAME_X - 16;
		int viewportHeight = Math.max(ROW_HEIGHT, listBottom - FIRST_ROW_Y);
		memberList.setViewport(NAME_X, FIRST_ROW_Y, viewportWidth, viewportHeight);

		List<ClientMemberView> members = ClientIslandCache.getMembers();
		memberList.setItemCount(members.size());
		memberRowWidgets.clear();
		int actionsX = this.width - 16 - ACTION_BUTTON_WIDTH;
		for (int i = 0; i < members.size(); i++) {
			ClientMemberView member = members.get(i);
			int buttonY = memberList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
			boolean rowVisible = memberList.isRowVisible(i);
			if (member.role() == ClientMemberView.Role.MEMBER || member.role() == ClientMemberView.Role.CO_OWNER) {
				// Both roles get the same two buttons: "Trust" toggles MEMBER<->CO_OWNER (server-side
				// MemberTrustC2S now always toggles by current role — see
				// MembershipService#toggleCoOwner) and "Quitar" always fully expels regardless of
				// role (MemberRemoveC2S — see MembershipService#removeMember). trustButtonLabel
				// highlights the button when the row is currently CO_OWNER, so its state is visible
				// without reading the role text next to the name.
				addRowButton(trustButtonLabel(member.role() == ClientMemberView.Role.CO_OWNER),
						actionsX - ACTION_BUTTON_WIDTH - 4, buttonY, rowVisible, i,
						() -> onTrustClicked(member.uuid(), member.role()));
				addRowButton(Text.translatable("islandcoreclient.members.remove"), actionsX, buttonY, rowVisible, i,
						() -> onRemoveClicked(member.uuid()));
			}
		}

		this.inviteField = new TextFieldWidget(this.textRenderer, fieldX, inviteFieldY, fieldWidth, INVITE_ROW_HEIGHT,
				Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setPlaceholder(Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setMaxLength(32);
		this.addDrawableChild(this.inviteField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.invite_button"),
						button -> onInviteClicked())
				.dimensions(fieldX + fieldWidth + 4, inviteFieldY, buttonWidth, INVITE_ROW_HEIGHT)
				.build());
	}

	// Bold + aqua (same color MEMBERS.role().label() uses for CO_OWNER) when the row is currently
	// CO_OWNER, plain otherwise — a highlighted "Trust" button reads as "already trusted", same
	// visual language ToggleRow-style active states already use elsewhere.
	private static Text trustButtonLabel(boolean isCoOwner) {
		Text base = Text.translatable("islandcoreclient.members.trust");
		return isCoOwner ? base.copy().formatted(Formatting.AQUA, Formatting.BOLD) : base;
	}

	private void addRowButton(Text text, int x, int y, boolean rowVisible, int rowIndex, Runnable onClick) {
		ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(text, b -> onClick.run())
				.dimensions(x, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());
		button.visible = rowVisible;
		button.active = rowVisible;
		memberRowWidgets.add(new IndexedWidget(rowIndex, button));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (memberList.isMouseOver(mouseX, mouseY)) {
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

	// Called by ClientPacketHandlers when a fresh FlagsStatusS2C/snapshot lands while this screen is
	// open — same pattern as every other status-driven screen. Rebuilds from scratch, so scroll
	// resets to the top; acceptable since the underlying list contents just changed anyway.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		List<ClientMemberView> members = ClientIslandCache.getMembers();
		memberList.startClip(context);
		for (int i = 0; i < members.size(); i++) {
			if (!memberList.isRowVisible(i)) {
				continue;
			}
			ClientMemberView member = members.get(i);
			Text line = Text.literal(member.name() + " ").append(member.role().label());
			int y = memberList.getRowY(i);
			context.drawTextWithShadow(this.textRenderer, line, NAME_X, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
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

	private void onInviteClicked() {
		String targetName = this.inviteField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new MemberInviteC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// The real expiry (5 minutes) comes back on the next snapshot refresh; refetch
				// now instead of guessing it locally.
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}
}
