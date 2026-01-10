package pl.bkubiak.bkrynek.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GreenButton extends ButtonWidget {
    protected int borderColor;

    public GreenButton(int x, int y, int width, int height, String text, PressAction onPress, int borderColor) {
        super(x, y, width, height, Text.of(text), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.borderColor = borderColor;
    }

    public void update(String text, int color) {
        this.setMessage(Text.of(text));
        this.borderColor = color;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int bgColor = this.isHovered() ? 0xFF1A3A1A : 0xFF0A1A0A;
        context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        int colorToDraw = borderColor;
        // Border
        context.fill(getX(), getY(), getX() + width, getY() + 1, colorToDraw);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, colorToDraw);
        context.fill(getX(), getY(), getX() + 1, getY() + height, colorToDraw);
        context.fill(getX() + width - 1, getY(), getX() + width, getY() + height, colorToDraw);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text msg = getMessage();
        int textWidth = tr.getWidth(msg);
        int textX = getX() + (width - textWidth) / 2;
        int textY = getY() + (height - 8) / 2;
        int textColor = this.isHovered() ? 0xFFFFFFFF : colorToDraw;
        context.drawTextWithShadow(tr, msg, textX, textY, textColor);
    }
}
