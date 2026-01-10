package pl.bkubiak.bkrynek.client.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    // Detects:
    // 1. Standard URLs (http/https/www)
    // 2. Specific domain: dc.bkubiak.dev
    // 3. Specific domain: tipply.pl (optionally followed by path)
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://|www\\.)\\S+|dc\\.bkubiak\\.dev|tipply\\.pl\\S*",
            Pattern.CASE_INSENSITIVE);

    public static Text translateColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return Text.empty();
        }
        MutableText result = Text.empty();
        StringBuilder currentSegment = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 1 < chars.length) {
                if (currentSegment.length() > 0) {
                    appendWithLinks(result, currentSegment.toString(), currentStyle);
                    currentSegment.setLength(0);
                }
                char code = Character.toLowerCase(chars[++i]);
                currentStyle = applyColorCode(code);
            } else {
                currentSegment.append(c);
            }
        }
        if (currentSegment.length() > 0) {
            appendWithLinks(result, currentSegment.toString(), currentStyle);
        }
        return result;
    }

    private static void appendWithLinks(MutableText root, String text, Style style) {
        Matcher matcher = URL_PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Append part before link
            if (start > lastEnd) {
                String pre = text.substring(lastEnd, start);
                root.append(Text.literal(pre).setStyle(style));
            }

            // Process link
            String url = text.substring(start, end);
            String target = url;
            // Ensure protocol for opening
            if (!target.toLowerCase().startsWith("http")) {
                target = "https://" + target;
            }

            Style linkStyle = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, target))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Otwórz: " + target)));

            root.append(Text.literal(url).setStyle(linkStyle));

            lastEnd = end;
        }
        // Append remaining
        if (lastEnd < text.length()) {
            String post = text.substring(lastEnd);
            root.append(Text.literal(post).setStyle(style));
        }
    }

    private static Style applyColorCode(char code) {
        if (code == 'r') {
            return Style.EMPTY;
        }
        switch (code) {
            case '0':
                return Style.EMPTY.withColor(Formatting.BLACK);
            case '1':
                return Style.EMPTY.withColor(Formatting.DARK_BLUE);
            case '2':
                return Style.EMPTY.withColor(Formatting.DARK_GREEN);
            case '3':
                return Style.EMPTY.withColor(Formatting.DARK_AQUA);
            case '4':
                return Style.EMPTY.withColor(Formatting.DARK_RED);
            case '5':
                return Style.EMPTY.withColor(Formatting.DARK_PURPLE);
            case '6':
                return Style.EMPTY.withColor(Formatting.GOLD);
            case '7':
                return Style.EMPTY.withColor(Formatting.GRAY);
            case '8':
                return Style.EMPTY.withColor(Formatting.DARK_GRAY);
            case '9':
                return Style.EMPTY.withColor(Formatting.BLUE);
            case 'a':
                return Style.EMPTY.withColor(Formatting.GREEN);
            case 'b':
                return Style.EMPTY.withColor(Formatting.AQUA);
            case 'c':
                return Style.EMPTY.withColor(Formatting.RED);
            case 'd':
                return Style.EMPTY.withColor(Formatting.LIGHT_PURPLE);
            case 'e':
                return Style.EMPTY.withColor(Formatting.YELLOW);
            case 'f':
                return Style.EMPTY.withColor(Formatting.WHITE);
            default:
                return Style.EMPTY;
        }
    }
}