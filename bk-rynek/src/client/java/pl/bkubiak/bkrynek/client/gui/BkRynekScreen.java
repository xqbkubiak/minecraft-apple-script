package pl.bkubiak.bkrynek.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pl.bkubiak.bkrynek.client.BkRynekClient;
import pl.bkubiak.bkrynek.client.keybinding.ToggleScanner;
import pl.bkubiak.bkrynek.client.util.SearchAutomationController;
import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;
import pl.bkubiak.bkrynek.core.util.PriceFormatter;

import java.util.List;

public class BkRynekScreen extends Screen {

    // Panel Colors
    private static final int COLOR_OVERLAY = 0xAA000000;
    private static final int COLOR_PANEL_TOP = 0xE6081810;
    private static final int COLOR_PANEL_BOTTOM = 0xE6040C08;
    private static final int COLOR_ACCENT = 0xFF00DD55;

    // Row Colors
    private static final int COLOR_YELLOW = 0xFFFFDD00;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_CYAN = 0xFF55FFFF;
    private static final int COLOR_ORANGE = 0xFFFFAA00;

    // UI Elements
    private static final int COLOR_ROW_LINE = 0xFF1A2A1A;
    private static final int COLOR_BORDER = 0xFF00AA44;
    private static final int COLOR_HEADER_BG = 0x40005500;

    private int panelX, panelY, panelW, panelH;
    private boolean isCompact = false;
    private int headerH, tableHeadH, rowH, gap, socialH;

    private int scrollOffset = 0;
    private int maxVisibleRows = 8;

    public BkRynekScreen() {
        super(Text.of("BK-Rynek - Autobuy"));
    }

    protected void applyBlur() {
        // Disable MC 1.21 blur
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't draw default background
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void close() {
        System.out.println("[BK-Rynek] Screen close() called.");
        super.close();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    protected void init() {
        try {
            panelW = Math.min(460, this.width - 20);
            panelX = (this.width - panelW) / 2;
            int margin = 15;
            isCompact = this.height < 450;

            if (isCompact) {
                headerH = 34;
                tableHeadH = 14;
                rowH = 14;
                gap = 4;
                socialH = 16;
                maxVisibleRows = 8;
            } else {
                headerH = 60;
                tableHeadH = 22;
                rowH = 24;
                gap = 10;
                socialH = 24;
                maxVisibleRows = Math.min(12, (this.height - 250) / rowH);
                if (maxVisibleRows < 6)
                    maxVisibleRows = 6;
            }

            int layoutBtnHeight = isCompact ? 20 : 26;
            int hintAreaH = 28;
            int contentHeight = headerH + tableHeadH + (rowH * maxVisibleRows) + gap * 4 + layoutBtnHeight + socialH
                    + hintAreaH + 20;
            panelH = contentHeight;
            panelY = (this.height - panelH) / 2;

            if (panelY < 10)
                panelY = 10;
            if (panelY + panelH > this.height - 10) {
                panelH = this.height - panelY - 10;
            }

            int tableEndY = panelY + headerH + tableHeadH + (rowH * maxVisibleRows);
            int hintY = tableEndY + gap;
            int btnY = hintY + hintAreaH + gap;
            int socialY = btnY + layoutBtnHeight + gap;

            int btnHeight = isCompact ? 20 : 26;
            int btnAreaWidth = panelW - (margin * 2);
            int numButtons = 5;
            int btnGap = 6;
            int btnWidth = (btnAreaWidth - (btnGap * (numButtons - 1))) / numButtons;
            int startX = panelX + margin;

            this.addDrawableChild(new GreenButton(startX, btnY, btnWidth, btnHeight, "> Start", b -> {
                SearchAutomationController.start(-1);
            }, COLOR_GREEN));

            this.addDrawableChild(
                    new GreenButton(startX + (btnWidth + btnGap), btnY, btnWidth, btnHeight, "# Stop", b -> {
                        SearchAutomationController.cancel();
                    }, COLOR_RED));

            // $/sum toggle
            this.addDrawableChild(new GreenButton(startX + (btnWidth + btnGap) * 2, btnY, btnWidth, btnHeight,
                    BkRynekClient.isSumMode ? "$/sum" : "$/szt", b -> {
                        BkRynekClient.isSumMode = !BkRynekClient.isSumMode;
                        GreenButton gb = (GreenButton) b;
                        gb.update(BkRynekClient.isSumMode ? "$/sum" : "$/szt",
                                BkRynekClient.isSumMode ? COLOR_YELLOW : COLOR_GREEN);
                    }, BkRynekClient.isSumMode ? COLOR_YELLOW : COLOR_GREEN));

            // Sniper toggle
            this.addDrawableChild(new GreenButton(startX + (btnWidth + btnGap) * 3, btnY, btnWidth, btnHeight,
                    BkRynekClient.isSniperMode ? "Sniper" : "Scan", b -> {
                        BkRynekClient.isSniperMode = !BkRynekClient.isSniperMode;
                        GreenButton gb = (GreenButton) b;
                        gb.update(BkRynekClient.isSniperMode ? "Sniper" : "Scan",
                                BkRynekClient.isSniperMode ? COLOR_ORANGE : COLOR_CYAN);
                    }, BkRynekClient.isSniperMode ? COLOR_ORANGE : COLOR_CYAN));

            this.addDrawableChild(
                    new GreenButton(startX + (btnWidth + btnGap) * 4, btnY, btnWidth, btnHeight, "X", b -> {
                        this.client.setScreen(null);
                    }, COLOR_GRAY));

            System.out.println("[BK-Rynek] init() - adding social buttons...");
            int socialBtnWidth = 90;
            int socialGap = 10;
            int socialTotalWidth = socialBtnWidth * 2 + socialGap;
            int socialStartX = panelX + (panelW - socialTotalWidth) / 2;

            this.addDrawableChild(new GreenButton(socialStartX, socialY, socialBtnWidth, 18, "GitHub", b -> {
                Util.getOperatingSystem().open("https://github.com/xqbkubiak");
            }, 0xFF333333));
            this.addDrawableChild(new GreenButton(socialStartX + socialBtnWidth + socialGap, socialY, socialBtnWidth,
                    18, "Discord", b -> {
                        Util.getOperatingSystem().open("https://bkubiak.dev/discord");
                    }, 0xFF5865F2));

            System.out.println("[BK-Rynek] init() - success.");
        } catch (Throwable e) {
            System.out.println("[BK-Rynek] CRASH in init(): " + e.toString());
            e.printStackTrace();
        }
        System.out.println("[BK-Rynek] init() finished.");
    }

    private static boolean firstRender = true;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowStartY = panelY + headerH + tableHeadH;
        String profileName = ClientPriceListManager.getActiveProfile();
        List<PriceEntry> entries = ClientPriceListManager.getAllProfiles().get(profileName);

        if (entries != null) {
            int visibleCount = Math.min(entries.size() - scrollOffset, maxVisibleRows);
            for (int i = 0; i < visibleCount; i++) {
                int entryIndex = i + scrollOffset;
                if (entryIndex >= entries.size())
                    break;

                int rowY = rowStartY + (i * rowH);

                // Check if clicked the 'X' button
                if (mouseX >= panelX + panelW - 45 && mouseX <= panelX + panelW - 10 && mouseY >= rowY - 2
                        && mouseY < rowY + rowH + 2) {
                    PriceEntry pe = entries.get(entryIndex);
                    ClientPriceListManager.removePriceEntry(pe);
                    return true;
                }

                // Check if clicked the row to edit
                if (mouseX >= panelX + 8 && mouseX <= panelX + panelW - 45 && mouseY >= rowY && mouseY < rowY + rowH) {
                    PriceEntry pe = entries.get(entryIndex);
                    this.client.setScreen(new QuickAddPriceScreen(pe, this, success -> {
                    }));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (firstRender) {
            System.out.println("[BK-Rynek] First render() call.");
            firstRender = false;
        }

        // Overlay
        context.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        // Panel background
        drawGradientPanel(context, panelX, panelY, panelW, panelH);
        drawBorder(context, panelX, panelY, panelW, panelH, COLOR_BORDER);

        // Header
        int headerY = panelY + (isCompact ? 10 : 14);
        context.fill(panelX + 4, headerY - 4, panelX + panelW - 4, headerY + 14, COLOR_HEADER_BG);
        context.drawTextWithShadow(this.textRenderer, Text.of("§2BK-§aRynek"), panelX + 15, headerY, COLOR_ACCENT);
        context.drawTextWithShadow(this.textRenderer, Text.of("- Autobuy"), panelX + 80, headerY, COLOR_WHITE);

        // Status indicator
        boolean isActive = ToggleScanner.scanningEnabled;
        String statusText = isActive ? "AKTYWNY" : "NIEAKTYWNY";
        int statusColor = isActive ? COLOR_GREEN : COLOR_RED;
        context.drawTextWithShadow(this.textRenderer, Text.of("• " + statusText), panelX + panelW - 95, headerY,
                statusColor);

        // Profile name
        String profileName = ClientPriceListManager.getActiveProfile();
        context.drawTextWithShadow(this.textRenderer, Text.of("Profil: §7" + profileName), panelX + 15,
                headerY + (isCompact ? 18 : 24), COLOR_GRAY);

        // Table header
        int tableHeaderY = panelY + headerH;
        context.drawTextWithShadow(this.textRenderer, Text.of("#"), panelX + 15, tableHeaderY, 0xFF666666);
        context.drawTextWithShadow(this.textRenderer, Text.of("Nazwa"), panelX + 35, tableHeaderY, 0xFF666666);
        context.drawTextWithShadow(this.textRenderer, Text.of("Max cena"), panelX + panelW - 80, tableHeaderY,
                0xFF666666);
        context.fill(panelX + 8, tableHeaderY + (isCompact ? 12 : 16), panelX + panelW - 8,
                tableHeaderY + (isCompact ? 13 : 17), COLOR_ROW_LINE);

        // Item list
        int rowStartY = tableHeaderY + tableHeadH;
        List<PriceEntry> entries = ClientPriceListManager.getAllProfiles().get(profileName);

        if (entries == null || entries.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.of("Brak wpisów na liście..."), panelX + 15,
                    rowStartY + 6, COLOR_GRAY);
        } else {
            int visibleCount = Math.min(entries.size() - scrollOffset, maxVisibleRows);
            for (int i = 0; i < visibleCount; i++) {
                int entryIndex = i + scrollOffset;
                if (entryIndex >= entries.size())
                    break;

                PriceEntry pe = entries.get(entryIndex);
                String displayName = pe.name;
                if (displayName == null || displayName.isEmpty()) {
                    displayName = pe.material;
                }
                if (pe.requiredCount > 1) {
                    displayName += " x" + pe.requiredCount;
                }
                String priceStr = PriceFormatter.formatPrice(pe.maxPrice);

                int rowY = rowStartY + (i * rowH);

                // Draw row hover background
                if (mouseX >= panelX + 8 && mouseX <= panelX + panelW - 8 && mouseY >= rowY && mouseY < rowY + rowH) {
                    context.fill(panelX + 8, rowY - 1, panelX + panelW - 8, rowY + rowH - 1, 0x20FFFFFF);
                }

                drawTableRow(context, entryIndex + 1, displayName, priceStr, COLOR_WHITE, COLOR_GREEN, rowY);

                // Delete button
                context.drawTextWithShadow(this.textRenderer, Text.of("§c✖"), panelX + panelW - 30, rowY, COLOR_RED);
            }
        }

        // Hints
        int tableEndY = panelY + headerH + tableHeadH + (rowH * maxVisibleRows);
        int hintY = tableEndY + gap;
        String hint1 = "Ctrl+A = Dodaj przedmiot do listy";
        String hint2 = "Kliknij wiersz, aby edytować cenę";
        int hint1Width = this.textRenderer.getWidth(hint1);
        int hint2Width = this.textRenderer.getWidth(hint2);
        context.drawTextWithShadow(this.textRenderer, Text.of(hint1), panelX + (panelW - hint1Width) / 2, hintY,
                COLOR_GRAY);
        context.drawTextWithShadow(this.textRenderer, Text.of(hint2), panelX + (panelW - hint2Width) / 2, hintY + 12,
                0xFF555555);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawGradientPanel(DrawContext context, int x, int y, int w, int h) {
        for (int i = 0; i < h; i++) {
            float ratio = (float) i / h;
            int color = interpolateColor(COLOR_PANEL_TOP, COLOR_PANEL_BOTTOM, ratio);
            context.fill(x, y + i, x + w, y + i + 1, color);
        }
    }

    private int interpolateColor(int c1, int c2, float ratio) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawTableRow(DrawContext context, int num, String label, String value, int labelColor, int valueColor,
            int y) {
        context.drawTextWithShadow(this.textRenderer, Text.of(String.valueOf(num)), panelX + 15, y, COLOR_GRAY);

        // Truncate label if too long
        String displayLabel = label;
        int maxLabelWidth = panelW - 130;
        if (this.textRenderer.getWidth(displayLabel) > maxLabelWidth) {
            while (this.textRenderer.getWidth(displayLabel + "...") > maxLabelWidth && displayLabel.length() > 0) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - 1);
            }
            displayLabel += "...";
        }

        context.drawTextWithShadow(this.textRenderer, Text.of(displayLabel), panelX + 35, y, labelColor);
        context.drawTextWithShadow(this.textRenderer, Text.of(value), panelX + panelW - 80, y, valueColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        String profileName = ClientPriceListManager.getActiveProfile();
        List<PriceEntry> entries = ClientPriceListManager.getAllProfiles().get(profileName);
        if (entries != null && entries.size() > maxVisibleRows) {
            int maxScroll = entries.size() - maxVisibleRows;
            scrollOffset -= (int) verticalAmount;
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > maxScroll)
                scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
