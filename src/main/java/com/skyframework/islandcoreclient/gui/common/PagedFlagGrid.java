package com.skyframework.islandcoreclient.gui.common;

/**
 * Layout/pagination helper for a flat list of fixed-height rows laid out in a fixed number of
 * columns (2, for every current use), paginated with a fixed page size instead of scrolled —
 * shared by SettingsScreen's "Permisos" tab and the admin "Configuración por Defecto" screen, both
 * of which show a {@code FlagPresetRow} per item. Deliberately NOT a {@code ClickableWidget}
 * itself, same reasoning as {@link ScrollableRowList}: every row is still a real widget added to
 * the {@code Screen} via {@code addDrawableChild}, this class only computes where each row's X/Y
 * should be given the current page and tells the caller which rows are on the current page at all.
 *
 * <p>Unlike {@link ScrollableRowList}, this has no scroll state and no {@code mouseScrolled}
 * integration — navigation is exclusively via {@link #prevPage()}/{@link #nextPage()}, meant to be
 * wired to explicit "&lt;&lt; Prev" / "Next &gt;&gt;" buttons that call
 * {@code Screen#clearAndInit()} afterward (same as a tab switch), not a per-frame reposition.
 *
 * <p>Row-major fill: item 0 is top-left, item 1 is to its right, item 2 starts the second row, and
 * so on — reads the same way as flags in registration order or exception groups in registration
 * order already print via "/island flags"/"/island exceptions".
 */
public final class PagedFlagGrid {
	private static final int COLUMNS = 2;

	private int viewportX;
	private int viewportY;
	private int viewportWidth;
	private int viewportHeight;
	private final int rowHeight;
	private final int rowSpacing;
	private final int columnGap;

	private int itemCount;
	private int currentPage;

	public PagedFlagGrid(int viewportX, int viewportY, int viewportWidth, int viewportHeight, int rowHeight, int rowSpacing, int columnGap) {
		this.viewportX = viewportX;
		this.viewportY = viewportY;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.rowHeight = rowHeight;
		this.rowSpacing = rowSpacing;
		this.columnGap = columnGap;
	}

	// Re-anchors the viewport (e.g. after a screen resize/re-init, when this grid is kept as a
	// persistent field so currentPage survives across Screen#initContent() calls instead of
	// resetting to page 0 every time — in particular right after Next/Prev's own clearAndInit()).
	public void setViewport(int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
		this.viewportX = viewportX;
		this.viewportY = viewportY;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
	}

	// Call once per Screen#initContent(), before positioning any row, with the real item count.
	// Re-clamps a stale currentPage if the list shrunk below it (e.g. an admin removed an exception
	// group from the config and reloaded), same re-clamp reasoning as ScrollableRowList#setItemCount.
	public void setItemCount(int itemCount) {
		this.itemCount = Math.max(0, itemCount);
		int lastPage = Math.max(0, totalPages() - 1);
		if (currentPage > lastPage) {
			currentPage = lastPage;
		}
	}

	private int rowsPerPage() {
		return Math.max(1, (viewportHeight + rowSpacing) / (rowHeight + rowSpacing));
	}

	private int itemsPerPage() {
		return COLUMNS * rowsPerPage();
	}

	// Always at least 1, even for an empty list — an empty grid still shows one (empty) page rather
	// than a division-by-zero-shaped "page 0 of 0".
	public int totalPages() {
		int perPage = itemsPerPage();
		return Math.max(1, (itemCount + perPage - 1) / perPage);
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public boolean hasPrevPage() {
		return currentPage > 0;
	}

	public boolean hasNextPage() {
		return currentPage + 1 < totalPages();
	}

	public void prevPage() {
		if (hasPrevPage()) {
			currentPage--;
		}
	}

	public void nextPage() {
		if (hasNextPage()) {
			currentPage++;
		}
	}

	// Whether global item index i falls on the current page — callers must set both .visible and
	// .active to this on every row widget, same contract as ScrollableRowList#isRowVisible.
	public boolean isItemOnCurrentPage(int index) {
		int perPage = itemsPerPage();
		int pageStart = currentPage * perPage;
		return index >= pageStart && index < pageStart + perPage;
	}

	private int columnWidth() {
		return (viewportWidth - (COLUMNS - 1) * columnGap) / COLUMNS;
	}

	public int getItemWidth() {
		return columnWidth();
	}

	public int getItemX(int index) {
		int posInPage = index - currentPage * itemsPerPage();
		int col = posInPage % COLUMNS;
		return viewportX + col * (columnWidth() + columnGap);
	}

	public int getItemY(int index) {
		int posInPage = index - currentPage * itemsPerPage();
		int row = posInPage / COLUMNS;
		return viewportY + row * (rowHeight + rowSpacing);
	}
}
