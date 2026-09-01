package com.skyframework.islandcoreclient.gui.island;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.ScrollableRowList;
import com.skyframework.islandcoreclient.gui.common.TimeFormat;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.member.MemberAllyAddC2S;
import com.skyframework.islandcoreclient.network.member.MemberAllyRemoveC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteC2S;
import com.skyframework.islandcoreclient.network.member.MemberRemoveC2S;
import com.skyframework.islandcoreclient.network.member.MemberTrustC2S;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;
import com.skyframework.islandcoreclient.state.ClientPendingInviteView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * The member list and the pending-invite list each scroll independently inside their own fixed
 * viewport ({@link ScrollableRowList}) so an island with many members/invites never pushes the
 * invite/ally forms below the screen or off the clickable area — the two forms stay pinned near
 * the bottom, outside both scroll regions.
 */
public class MembersScreen extends BaseMenuScreen {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int LINE_HEIGHT = 11;
	private static final int NAME_X = 16;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int FIRST_ROW_Y = TOP_BAR_HEIGHT + 12;
	private static final int INVITE_ROW_HEIGHT = 20;
	private static final int ALLY_ROW_HEIGHT = 20;
	private static final int BOTTOM_ROW_GAP = 4;
	private static final int SECTION_GAP = 8;
	private static final int PENDING_HEADING_HEIGHT = LINE_HEIGHT + 4;
	// Members get the larger share of the split — the list owners interact with most.
	private static final double MEMBER_SHARE = 0.6;

	private record IndexedWidget(int rowIndex, ClickableWidget widget) {
	}

	private TextFieldWidget inviteField;
	private TextFieldWidget allyField;

	private final ScrollableRowList memberList = new ScrollableRowList(NAME_X, FIRST_ROW_Y, 1, 1, ROW_HEIGHT, ROW_SPACING);
	private final ScrollableRowList pendingList = new ScrollableRowList(NAME_X, FIRST_ROW_Y, 1, 1, LINE_HEIGHT, 0);
	private final List<IndexedWidget> memberRowWidgets = new ArrayList<>();

	public MembersScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.members.title"), parent);
	}

	@Override
	protected void initContent() {
		int fieldWidth = 160;
		int buttonWidth = 70;
		int inviteFieldY = this.height - 16 - INVITE_ROW_HEIGHT;
		int allyFieldY = inviteFieldY - BOTTOM_ROW_GAP - ALLY_ROW_HEIGHT;
		int fieldX = this.width / 2 - (fieldWidth + 4 + buttonWidth) / 2;
		int listsBottom = allyFieldY - SECTION_GAP;

		relayoutLists(listsBottom);

		List<ClientMemberView> members = ClientIslandCache.getMembers();
		memberList.setItemCount(members.size());
		memberRowWidgets.clear();
		int actionsX = this.width - 16 - ACTION_BUTTON_WIDTH;
		for (int i = 0; i < members.size(); i++) {
			ClientMemberView member = members.get(i);
			int buttonY = memberList.getRowY(i) + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
			boolean rowVisible = memberList.isRowVisible(i);
			if (member.role() == ClientMemberView.Role.MEMBER) {
				addRowButton(Text.translatable("islandcoreclient.members.trust"), actionsX - ACTION_BUTTON_WIDTH - 4, buttonY, rowVisible, i,
						() -> onTrustClicked(member.uuid()));
				addRowButton(Text.translatable("islandcoreclient.members.remove"), actionsX, buttonY, rowVisible, i,
						() -> onRemoveClicked(member.uuid()));
			} else if (member.role() == ClientMemberView.Role.TRUSTED) {
				addRowButton(Text.translatable("islandcoreclient.members.remove"), actionsX, buttonY, rowVisible, i,
						() -> onRemoveClicked(member.uuid()));
			} else if (member.role() == ClientMemberView.Role.ALLY) {
				addRowButton(Text.translatable("islandcoreclient.members.remove_ally"), actionsX, buttonY, rowVisible, i,
						() -> onAllyRemoveClicked(member.uuid()));
			}
		}

		pendingList.setItemCount(ClientIslandCache.getPendingInvites().size());

		this.inviteField = new TextFieldWidget(this.textRenderer, fieldX, inviteFieldY, fieldWidth, INVITE_ROW_HEIGHT,
				Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setPlaceholder(Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setMaxLength(32);
		this.addDrawableChild(this.inviteField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.invite_button"),
						button -> onInviteClicked())
				.dimensions(fieldX + fieldWidth + 4, inviteFieldY, buttonWidth, INVITE_ROW_HEIGHT)
				.build());

		// An ally doesn't have to already be a member — same "type a name, resolve server-side" flow
		// as the invite field above, not a per-row button (there's nothing to attach it to for a
		// player who isn't already listed).
		this.allyField = new TextFieldWidget(this.textRenderer, fieldX, allyFieldY, fieldWidth, ALLY_ROW_HEIGHT,
				Text.translatable("islandcoreclient.members.ally_add_placeholder"));
		this.allyField.setPlaceholder(Text.translatable("islandcoreclient.members.ally_add_placeholder"));
		this.allyField.setMaxLength(32);
		this.addDrawableChild(this.allyField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.ally_add_button"),
						button -> onAllyAddClicked())
				.dimensions(fieldX + fieldWidth + 4, allyFieldY, buttonWidth, ALLY_ROW_HEIGHT)
				.build());
	}

	// Recomputes both viewports from the current screen size only (never from item counts), so
	// calling this from mouseScrolled-triggered relayouts would be safe too — kept here for now
	// since only initContent needs it. viewportWidth spans from NAME_X to the action-button column.
	private void relayoutLists(int listsBottom) {
		int viewportWidth = this.width - NAME_X - 16;
		int splittable = Math.max(0, listsBottom - PENDING_HEADING_HEIGHT - SECTION_GAP - FIRST_ROW_Y);
		int memberViewportHeight = Math.max(ROW_HEIGHT, (int) (splittable * MEMBER_SHARE));
		int pendingViewportY = FIRST_ROW_Y + memberViewportHeight + SECTION_GAP + PENDING_HEADING_HEIGHT;
		int pendingViewportHeight = Math.max(LINE_HEIGHT, listsBottom - pendingViewportY);

		memberList.setViewport(NAME_X, FIRST_ROW_Y, viewportWidth, memberViewportHeight);
		pendingList.setViewport(NAME_X, pendingViewportY, viewportWidth, pendingViewportHeight);
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
		if (pendingList.isMouseOver(mouseX, mouseY)) {
			pendingList.scroll(verticalAmount);
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

		int headingY = memberList.getViewportY() + memberList.getViewportHeight() + SECTION_GAP;
		context.drawTextWithShadow(this.textRenderer, Text.translatable("islandcoreclient.members.pending_heading"), NAME_X, headingY, 0xAAAAAA);

		List<ClientPendingInviteView> invites = ClientIslandCache.getPendingInvites();
		pendingList.startClip(context);
		for (int i = 0; i < invites.size(); i++) {
			if (!pendingList.isRowVisible(i)) {
				continue;
			}
			ClientPendingInviteView invite = invites.get(i);
			long remaining = invite.getRemainingSeconds();
			String time = TimeFormat.minutesSeconds(remaining);
			context.drawTextWithShadow(this.textRenderer, Text.literal(invite.targetName() + " (" + time + ")"), NAME_X, pendingList.getRowY(i), 0x999999);
		}
		pendingList.endClip(context);
		pendingList.renderScrollbar(context);
	}

	private void onTrustClicked(UUID uuid) {
		ClientPlayNetworking.send(new MemberTrustC2S(uuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientIslandCache.promoteToTrusted(uuid);
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

	private void onAllyAddClicked() {
		String targetName = this.allyField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new MemberAllyAddC2S(targetName));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				// The real uuid/name for the new ALLY entry comes back on the next snapshot
				// refresh — same reasoning as the invite flow above.
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
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
