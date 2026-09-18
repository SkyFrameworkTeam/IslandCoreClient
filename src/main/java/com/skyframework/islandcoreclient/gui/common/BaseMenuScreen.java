package com.skyframework.islandcoreclient.gui.common;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Full-screen base for every IslandCore Client menu: draws a fixed top bar (screen title,
 * optional "Volver" button) and delegates the rest of the screen to subclasses.
 */
public abstract class BaseMenuScreen extends Screen {
	protected static final int TOP_BAR_HEIGHT = 28;
	private static final int BAR_PADDING = 8;
	private static final int BACK_BUTTON_WIDTH = 70;
	private static final int BACK_BUTTON_HEIGHT = 20;

	@Nullable
	private final Screen parent;

	protected BaseMenuScreen(Text title, @Nullable Screen parent) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected final void init() {
		if (this.parent != null) {
			this.addDrawableChild(ButtonWidget.builder(Text.literal("← Volver"), button -> this.close())
					.dimensions(BAR_PADDING, (TOP_BAR_HEIGHT - BACK_BUTTON_HEIGHT) / 2, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)
					.build());
		}
		this.initContent();
	}

	// Subclasses add their own widgets here instead of overriding init(), which this class
	// reserves for the shared top bar.
	protected void initContent() {
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.renderTopBar(context);
		this.renderContent(context, mouseX, mouseY, delta);
	}

	private void renderTopBar(DrawContext context) {
		context.fill(0, 0, this.width, TOP_BAR_HEIGHT, 0xC0000000);
		context.drawHorizontalLine(0, this.width - 1, TOP_BAR_HEIGHT, 0xFFFFFFFF);

		int titleX = titleX();
		int titleY = (TOP_BAR_HEIGHT - this.textRenderer.fontHeight) / 2;
		context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFF);

		// Right edge intentionally left blank: reserved for a future "Admin" button.
	}

	private int titleX() {
		return this.parent != null ? BAR_PADDING + BACK_BUTTON_WIDTH + BAR_PADDING : BAR_PADDING;
	}

	// Called every frame, below the shared top bar.
	protected abstract void renderContent(DrawContext context, int mouseX, int mouseY, float delta);
}
