package com.skyframework.islandcoreclient.gui.common;

import net.minecraft.client.gui.DrawContext;

/**
 * Layout/scroll helper for a vertically-stacked list of fixed-height rows inside a fixed-size
 * viewport, for when the row count might exceed how many rows fit. Deliberately NOT a
 * {@code ClickableWidget} itself — every row is still a real {@code ButtonWidget}/
 * {@code TextFieldWidget}/{@link ToggleRow}/{@link TriStateRow} added to the {@code Screen} via
 * {@code addDrawableChild} as usual (this project's established pattern everywhere else); this
 * class only computes where each row's Y should be given the current scroll offset, clips
 * rendering to the viewport (so a row straddling the edge is cut off instead of bleeding into the
 * rest of the screen), and tells the caller which rows are fully scrolled out of view so their
 * widgets can be hidden/deactivated — a row scrolled away must never stay clickable.
 *
 * <p>Edge cases (verified against a standalone copy of this exact math before writing this class —
 * see the sprint notes): an empty list or a list short enough to fit the viewport both resolve to
 * {@link #isScrollable()} {@code false} — {@link #scroll} becomes a no-op and
 * {@link #renderScrollbar} draws nothing, so a short list never shows a scrollbar it doesn't need.
 * {@link #setItemCount} also re-clamps a stale scroll offset if the list shrinks (e.g. after a
 * kick/remove) so it can never get stuck scrolled past the new, shorter content.
 */
public final class ScrollableRowList {
	private static final int SCROLL_STEP_PX = 16;

	private int viewportX;
	private int viewportY;
	private int viewportWidth;
	private int viewportHeight;
	private final int rowHeight;
	private final int rowSpacing;

	private int itemCount;
	private int scrollOffset;

	public ScrollableRowList(int viewportX, int viewportY, int viewportWidth, int viewportHeight, int rowHeight, int rowSpacing) {
		this.viewportX = viewportX;
		this.viewportY = viewportY;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.rowHeight = rowHeight;
		this.rowSpacing = rowSpacing;
	}

	// Call once per Screen#initContent(), before positioning any row, with the real row count.
	public void setItemCount(int itemCount) {
		this.itemCount = Math.max(0, itemCount);
		this.scrollOffset = clamp(this.scrollOffset);
	}

	// Re-anchors the viewport (e.g. after a screen resize/re-init, when this list is kept as a
	// persistent field so scrollOffset survives across Screen#initContent() calls instead of
	// resetting to the top every time). Re-clamps the current scroll offset against the new bounds,
	// same as setItemCount above.
	public void setViewport(int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
		this.viewportX = viewportX;
		this.viewportY = viewportY;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.scrollOffset = clamp(this.scrollOffset);
	}

	public int getViewportX() {
		return viewportX;
	}

	public int getViewportY() {
		return viewportY;
	}

	public int getViewportWidth() {
		return viewportWidth;
	}

	public int getViewportHeight() {
		return viewportHeight;
	}

	private int contentHeight() {
		if (itemCount <= 0) {
			return 0;
		}
		return itemCount * rowHeight + (itemCount - 1) * rowSpacing;
	}

	private int maxScrollOffset() {
		return Math.max(0, contentHeight() - viewportHeight);
	}

	// True only when the content is actually taller than the viewport — an empty list or a single
	// short row never reports true.
	public boolean isScrollable() {
		return maxScrollOffset() > 0;
	}

	// wheelAmount matches Screen#mouseScrolled's own vertical-amount parameter (positive = scroll
	// up/toward the start of the content, matching vanilla list widgets).
	public void scroll(double wheelAmount) {
		if (!isScrollable()) {
			return;
		}
		scrollOffset = clamp(scrollOffset - (int) (wheelAmount * SCROLL_STEP_PX));
	}

	private int clamp(int value) {
		return Math.max(0, Math.min(value, maxScrollOffset()));
	}

	// Y position (screen space) for row index i, given the current scroll offset.
	public int getRowY(int index) {
		return viewportY - scrollOffset + index * (rowHeight + rowSpacing);
	}

	// Whether row index i is at least partially visible inside the viewport right now — callers
	// must set both .visible and .active to this on every row widget, so a row scrolled fully out
	// of view can neither render nor be clicked.
	public boolean isRowVisible(int index) {
		int rowTop = getRowY(index);
		int rowBottom = rowTop + rowHeight;
		return rowBottom > viewportY && rowTop < viewportY + viewportHeight;
	}

	// Wrap the content you draw/position between these two calls so a row straddling the viewport
	// edge is cleanly cut off instead of bleeding into the rest of the screen. Widgets added via
	// addDrawableChild render through the Screen's own pass, not here — this scissor is for
	// renderContent's own manual drawText/fill calls inside the viewport; ButtonWidget/
	// TextFieldWidget already clip themselves to their own bounds so isRowVisible's visible/active
	// gating is what actually keeps them from rendering or reacting outside the viewport.
	public void startClip(DrawContext context) {
		context.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
	}

	public void endClip(DrawContext context) {
		context.disableScissor();
	}

	// Draws a thin scrollbar thumb along the right edge of the viewport — a no-op when
	// isScrollable() is false, so a list that fits shows nothing.
	public void renderScrollbar(DrawContext context) {
		if (!isScrollable()) {
			return;
		}
		int contentHeight = contentHeight();
		int trackX = viewportX + viewportWidth - 3;
		int thumbHeight = Math.max(8, viewportHeight * viewportHeight / contentHeight);
		int trackRange = viewportHeight - thumbHeight;
		int thumbY = viewportY + (trackRange <= 0 ? 0 : trackRange * scrollOffset / maxScrollOffset());
		context.fill(trackX, viewportY, trackX + 2, viewportY + viewportHeight, 0x40FFFFFF);
		context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFFAAAAAA);
	}

	// Whether (mouseX, mouseY) falls inside this list's viewport — screens should only forward
	// mouseScrolled to this list when the cursor is actually over it, so scrolling one list on a
	// screen with several doesn't also scroll the others.
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= viewportX && mouseX < viewportX + viewportWidth
				&& mouseY >= viewportY && mouseY < viewportY + viewportHeight;
	}
}
