package com.skyframework.islandcoreclient.gui.island;

import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;
import com.skyframework.islandcoreclient.state.ClientPendingInviteView;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class MembersScreen extends BaseMenuScreen {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int LINE_HEIGHT = 11;
	private static final int NAME_X = 16;
	private static final int ACTION_BUTTON_WIDTH = 60;
	private static final int ACTION_BUTTON_HEIGHT = 16;
	private static final int FIRST_ROW_Y = TOP_BAR_HEIGHT + 12;
	private static final int INVITE_ROW_HEIGHT = 20;

	private TextFieldWidget inviteField;

	public MembersScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.members.title"), parent);
	}

	@Override
	protected void initContent() {
		int rowY = FIRST_ROW_Y;
		int actionsX = this.width - 16 - ACTION_BUTTON_WIDTH;

		for (ClientMemberView member : ClientIslandCache.getMembers()) {
			int thisButtonY = rowY + (ROW_HEIGHT - ACTION_BUTTON_HEIGHT) / 2;
			if (member.role() == ClientMemberView.Role.MEMBER) {
				this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.trust"),
								button -> simulateTrust(member.uuid()))
						.dimensions(actionsX - ACTION_BUTTON_WIDTH - 4, thisButtonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
				this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.remove"),
								button -> simulateRemove(member.uuid()))
						.dimensions(actionsX, thisButtonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
			} else if (member.role() == ClientMemberView.Role.TRUSTED) {
				this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.remove"),
								button -> simulateRemove(member.uuid()))
						.dimensions(actionsX, thisButtonY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
						.build());
			}
			rowY += ROW_HEIGHT + ROW_SPACING;
		}

		int fieldWidth = 160;
		int buttonWidth = 70;
		int fieldY = this.height - 16 - INVITE_ROW_HEIGHT;
		int fieldX = this.width / 2 - (fieldWidth + 4 + buttonWidth) / 2;

		this.inviteField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, INVITE_ROW_HEIGHT,
				Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setPlaceholder(Text.translatable("islandcoreclient.members.invite_placeholder"));
		this.inviteField.setMaxLength(32);
		this.addDrawableChild(this.inviteField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("islandcoreclient.members.invite_button"),
						button -> simulateInvite())
				.dimensions(fieldX + fieldWidth + 4, fieldY, buttonWidth, INVITE_ROW_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = NAME_X;
		int y = FIRST_ROW_Y;

		for (ClientMemberView member : ClientIslandCache.getMembers()) {
			Text line = Text.literal(member.name() + " ").append(member.role().label());
			context.drawTextWithShadow(this.textRenderer, line, x, y + (ROW_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
			y += ROW_HEIGHT + ROW_SPACING;
		}

		y += 8;
		context.drawTextWithShadow(this.textRenderer, Text.translatable("islandcoreclient.members.pending_heading"), x, y, 0xAAAAAA);
		y += LINE_HEIGHT;
		for (ClientPendingInviteView invite : ClientIslandCache.getPendingInvites()) {
			long remaining = invite.getRemainingSeconds();
			String time = String.format("%02d:%02d", remaining / 60, remaining % 60);
			context.drawTextWithShadow(this.textRenderer, Text.literal(invite.targetName() + " (" + time + ")"), x, y, 0x999999);
			y += LINE_HEIGHT;
		}
	}

	private void simulateTrust(UUID uuid) {
		simulateMemberTrust(uuid);
		this.clearAndInit();
	}

	private void simulateRemove(UUID uuid) {
		simulateMemberRemove(uuid);
		this.clearAndInit();
	}

	private void simulateInvite() {
		String targetName = this.inviteField.getText().trim();
		if (targetName.isEmpty()) {
			return;
		}
		simulateMemberInvite(targetName);
		this.clearAndInit();
	}

	// TODO: replace with sending MemberTrustC2S and awaiting ActionResultS2C once IslandCore
	// implements the member protocol. The row layout above should not need to change.
	private static void simulateMemberTrust(UUID uuid) {
		ClientIslandCache.promoteToTrusted(uuid);
	}

	// TODO: replace with sending MemberRemoveC2S and awaiting ActionResultS2C once IslandCore
	// implements the member protocol.
	private static void simulateMemberRemove(UUID uuid) {
		ClientIslandCache.removeMember(uuid);
	}

	// TODO: replace with sending MemberInviteC2S once IslandCore implements the member protocol.
	private static void simulateMemberInvite(String targetName) {
		ClientIslandCache.addPendingInvite(targetName);
	}
}
