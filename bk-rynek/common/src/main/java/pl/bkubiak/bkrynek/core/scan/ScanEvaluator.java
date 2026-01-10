package pl.bkubiak.bkrynek.core.scan;

import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.config.ServerEntry;
import pl.bkubiak.bkrynek.core.config.ServersConfig;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScanEvaluator {
    private ScanEvaluator() {
    }

    public static ScanResult evaluate(ScanInput input, ServersConfig config, boolean isSumMode) {
        if (input == null || config == null || config.servers == null) {
            return ScanResult.noHighlight();
        }
        String activeProfile = ClientPriceListManager.getActiveProfile();
        ServerEntry entry = findServerEntryByProfile(config, activeProfile);
        if (entry == null)
            return ScanResult.noHighlight();

        String loreRegex = entry.loreRegex;
        String colorStr = entry.highlightColor;
        String colorStackStr = (entry.highlightColorStack == null || entry.highlightColorStack.isEmpty())
                ? colorStr
                : entry.highlightColorStack;
        int highlightColor = parseColor(colorStr);
        int highlightColorStack = parseColor(colorStackStr);

        double foundPrice = -1;
        Pattern pattern = Pattern.compile(loreRegex, Pattern.CASE_INSENSITIVE);
        for (String plain : input.loreLines) {
            Matcher m = pattern.matcher(plain);
            if (m.find()) {
                String priceGroup = m.group(1);
                double parsedPrice = parsePriceWithSuffix(priceGroup);
                if (parsedPrice >= 0) {
                    foundPrice = parsedPrice;
                    break;
                }
            }
        }
        if (foundPrice < 0)
            return ScanResult.noHighlight();

        int stackSize = input.stackSize;
        boolean isStack = stackSize > 1;
        double finalPrice = isStack ? (foundPrice / stackSize) : foundPrice;

        PriceEntry matchedEntry = ClientPriceListManager.findMatchingPriceEntry(
                input.noColorName,
                input.loreLines,
                input.materialId,
                input.enchantments,
                input.componentCount,
                input.customModelData);
        if (matchedEntry == null) {
            return ScanResult.noHighlight();
        }

        if (isSumMode && matchedEntry.requiredCount > 0 && input.stackSize != matchedEntry.requiredCount) {
            return ScanResult.noHighlight();
        }

        double maxPrice = matchedEntry.maxPrice;

        // Mode switch: Sum vs Unit price
        double comparisonPrice = isSumMode ? foundPrice : finalPrice;

        if (comparisonPrice <= maxPrice) {
            // Determine if it's a bargain (<) or hit the limit (==)
            // Using a small epsilon for double comparison safety
            boolean isBargain = comparisonPrice < (maxPrice - 0.0001);

            int baseRGB;
            if (isBargain) {
                // Bargain found! Use the main configured color (usually Green)
                // We ignore stack-specific color for bargains to ensure clear "Green means Go"
                // signal
                baseRGB = (highlightColor & 0x00FFFFFF);
            } else {
                // Price hits the limit exactly - use Orange/Gold to warn/indicate limit
                baseRGB = 0xFFA500;
            }

            // Ensure good visibility opacity
            // Alpha 255 because we handle opacity in the Mixin renderer now (filling vs
            // border)
            // But we pass full color here.
            int dynamicColor = (0xFF << 24) | baseRGB;
            return new ScanResult(true, dynamicColor, foundPrice);
        }

        return ScanResult.noHighlight();
    }

    private static ServerEntry findServerEntryByProfile(ServersConfig config, String profileName) {
        if (config == null || config.servers == null)
            return null;
        for (ServerEntry se : config.servers) {
            if (se.profileName.equals(profileName)) {
                return se;
            }
        }
        return null;
    }

    private static int parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return 0xFFFFFFFF;
        }
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }
        if (colorStr.length() == 6) {
            colorStr = "FF" + colorStr;
        }
        long argb = Long.parseLong(colorStr, 16);
        return (int) (argb & 0xFFFFFFFF);
    }

    private static double parsePriceWithSuffix(String raw) {
        raw = raw.trim().replaceAll("[\\s\\u00A0\\u202F]+", "");
        String lower = raw.toLowerCase();
        double multiplier = 1.0;
        if (lower.endsWith("mld")) {
            multiplier = 1_000_000_000.0;
            raw = raw.substring(0, raw.length() - 3);
        } else if (lower.endsWith("m")) {
            multiplier = 1_000_000.0;
            raw = raw.substring(0, raw.length() - 1);
        } else if (lower.endsWith("k")) {
            multiplier = 1000.0;
            raw = raw.substring(0, raw.length() - 1);
        }

        int lastDot = raw.lastIndexOf('.');
        int lastComma = raw.lastIndexOf(',');
        int lastSep = Math.max(lastDot, lastComma);
        if (lastSep != -1) {
            int digitsAfter = raw.length() - lastSep - 1;
            if (digitsAfter > 0 && digitsAfter <= 2) {
                String intPart = raw.substring(0, lastSep).replaceAll("[.,]", "");
                String fracPart = raw.substring(lastSep + 1).replaceAll("[.,]", "");
                raw = intPart + "." + fracPart;
            } else {
                raw = raw.replaceAll("[.,]", "");
            }
        } else {
            raw = raw.replaceAll("[.,]", "");
        }

        try {
            double base = Double.parseDouble(raw);
            return base * multiplier;
        } catch (NumberFormatException e) {
            String digitsOnly = raw.replaceAll("\\D+", "");
            if (digitsOnly.isEmpty())
                return -1;
            try {
                double base = Double.parseDouble(digitsOnly);
                return base * multiplier;
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
    }
}
