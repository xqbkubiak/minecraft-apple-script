package pl.bkubiak.bkrynek.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;

import java.util.function.Consumer;

public class QuickAddPriceScreen extends Screen {
    private static final int COLOR_OVERLAY = 0xAA000000;
    private static final int COLOR_PANEL_TOP = 0xE6081810;
    private static final int COLOR_PANEL_BOTTOM = 0xE6040C08;
    private static final int COLOR_BORDER = 0xFF00AA44;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_RED = 0xFFFF5555;

    private final PriceEntry pendingEntry;
    private final Screen parent;
    private TextFieldWidget priceField;
    private final Consumer<Boolean> callback;

    public QuickAddPriceScreen(PriceEntry entry, Screen parent, Consumer<Boolean> callback) {
        super(Text.of("Quick Add Price"));
        this.pendingEntry = entry;
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int w = 240;
        int h = 130;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        this.priceField = new TextFieldWidget(this.textRenderer, x + 20, y + 60, 200, 20, Text.of("Cena"));
        this.priceField.setMaxLength(15);
        this.addSelectableChild(this.priceField);
        this.addDrawableChild(this.priceField);
        this.setInitialFocus(this.priceField);

        this.addDrawableChild(new GreenButton(x + 20, y + 90, 95, 24, "Dodaj", button -> {
            saveAndClose();
        }, COLOR_GREEN));

        this.addDrawableChild(new GreenButton(x + 125, y + 90, 95, 24, "Anuluj", button -> {
            this.client.setScreen(parent);
            callback.accept(false);
        }, COLOR_RED));
    }

    protected void applyBlur() {
        // Disable MC 1.21 blur
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't draw default background
    }

    private void saveAndClose() {
        try {
            String val = priceField.getText().replace(",", ".");
            if (val.isEmpty())
                return;
            pendingEntry.maxPrice = Double.parseDouble(val);
            ClientPriceListManager.addPriceEntry(pendingEntry);
            pl.bkubiak.bkrynek.client.command.ClientCommandRegistration.syncMemoryToConfig();
            this.client.setScreen(parent);
            callback.accept(true);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(parent);
            callback.accept(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Overlay
        context.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        // Reset color state
        // RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Removed in 1.21.8

        int w = 240;
        int h = 130;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Panel background & Border
        drawGradientPanel(context, x, y, w, h);
        drawBorder(context, x, y, w, h, COLOR_BORDER);

        context.drawTextWithShadow(this.textRenderer, Text.of("Quick Add: §a" + pendingEntry.name), x + 20, y + 15,
                0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.of("§7Lore: " + (pendingEntry.lore.isEmpty() ? "Brak"
                : pendingEntry.lore.substring(0, Math.min(pendingEntry.lore.length(), 30)) + "...")),
                x + 20, y + 28, COLOR_GRAY);
        context.drawTextWithShadow(this.textRenderer, Text.of("§ePodaj cenę maksymalną:"), x + 20, y + 45, 0xFFFFFFFF);

        // Render widgets (buttons AND priceField)
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
}
