package pl.bkubiak.bkrynek.client.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import pl.bkubiak.bkrynek.client.util.ColorUtils;

import java.util.concurrent.CompletableFuture;

public final class CommandUi {
    private CommandUi() {
    }

    public static MutableText colored(String text) {
        MutableText out = Text.empty();
        out.append(ColorUtils.translateColorCodes(text));
        return out;
    }

    public static MutableText clickable(String text, ClickEvent.Action action, String command, String hoverText) {
        MutableText out = colored(text);
        Style style = Style.EMPTY.withClickEvent(new ClickEvent(action, command));
        if (hoverText != null && !hoverText.isEmpty()) {
            style = style.withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal(hoverText)));
        }
        out.setStyle(style);
        return out;
    }

    public static CompletableFuture<Suggestions> suggestItemIds(SuggestionsBuilder builder) {
        String remainingRaw = builder.getRemaining().toLowerCase();
        if (remainingRaw.startsWith("mc:")) {
            remainingRaw = "minecraft:" + remainingRaw.substring(3);
        }
        final String remaining = remainingRaw;

        if (remaining.contains("minecraft:")) {
            net.minecraft.registry.Registries.ITEM.getIds().stream()
                    .map(id -> id.toString())
                    .filter(s -> s.contains(remaining))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}
