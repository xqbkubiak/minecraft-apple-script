package pl.bkubiak.bkrynek.core.manager;

import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.util.CompositeKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientPriceListManager {

    private static final Map<String, List<PriceEntry>> priceLists = new HashMap<>();

    private static final Map<String, Map<String, String>> customLookup = new HashMap<>();

    private static String activeProfile = "default";

    public static void setActiveProfile(String profile) {
        activeProfile = profile;
        priceLists.computeIfAbsent(profile, k -> new ArrayList<>());
        customLookup.computeIfAbsent(profile, k -> new HashMap<>());
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static String listProfiles() {
        if (priceLists.isEmpty()) {
            return "No profiles defined.";
        }
        return String.join(", ", priceLists.keySet());
    }

    public static void addPriceEntry(PriceEntry entry) {
        String compositeKey = CompositeKeyUtil.getCompositeKeyFromEntry(entry);

        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());

        entries.removeIf(pe -> {
            String keyFromEntry = CompositeKeyUtil.getCompositeKeyFromEntry(pe);
            return keyFromEntry.equals(compositeKey);
        });

        entries.add(entry);
    }

    public static void addPriceEntry(String rawItem, double maxPrice) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        String[] parts = compositeKey.split("\\|", -1);
        if (parts.length < 3) {
            return;
        }
        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];
        newEntry.lore = parts[1];
        newEntry.material = parts[2];
        newEntry.enchants = parts.length > 3 ? parts[3] : "";
        newEntry.maxPrice = maxPrice;
        addPriceEntry(newEntry);
    }

    public static void removePriceEntry(String rawItem) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries != null) {
            entries.removeIf(pe -> {
                String keyFromEntry = (pe.name + "|" +
                        (pe.lore == null ? "" : pe.lore) + "|" +
                        (pe.material == null ? "" : pe.material) + "|" +
                        (pe.enchants == null ? "" : pe.enchants)).toLowerCase();
                return keyFromEntry.equals(compositeKey);
            });
        }
    }

    public static void removePriceEntry(PriceEntry pe) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries != null) {
            entries.remove(pe);
        }
    }

    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId,
            String enchantments, int componentCount, Integer customModelData) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null)
            return null;

        PriceEntry best = null;
        int bestScore = -1;

        String lowerName = noColorName.toLowerCase();
        String lowerMaterialId = materialId.toLowerCase();
        String lowerEnchantments = enchantments == null ? "" : enchantments.toLowerCase();

        for (PriceEntry pe : entries) {
            // Strict match check first if entry was added via QuickAdd (has componentCount
            // > 0)
            if (pe.componentCount > 0) {
                if (componentCount < pe.componentCount)
                    continue;
                if (pe.customModelData != null && !pe.customModelData.equals(customModelData))
                    continue;
                // If we also have a material, it must match
                if (pe.material != null && !pe.material.isEmpty() && !materialId.equalsIgnoreCase(pe.material))
                    continue;

                // If it passes component checks, it's a very high probability match
                // We still check name/lore for absolute certainty
            }

            int score = 0;

            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
                score += 1000;
            }

            if (!pe.name.isEmpty()) {
                String lowerEntryName = pe.name.toLowerCase();
                boolean nameMatches = lowerName.contains(lowerEntryName) || lowerMaterialId.contains(lowerEntryName);
                if (!nameMatches) {
                    continue;
                }
                score += Math.min(500, lowerEntryName.length());
                if (lowerName.equals(lowerEntryName)) {
                    score += 200;
                }
            }

            if (pe.lore != null && !pe.lore.isEmpty()) {
                String[] configLoreParts = pe.lore.split(";");
                boolean allPartsFound = true;
                for (String part : configLoreParts) {
                    if (part.trim().isEmpty())
                        continue;
                    boolean partFound = false;
                    for (String line : loreLines) {
                        if (line.toLowerCase().contains(part.toLowerCase())) {
                            partFound = true;
                            break;
                        }
                    }
                    if (!partFound) {
                        allPartsFound = false;
                        break;
                    }
                }
                if (!allPartsFound) {
                    continue;
                }
                score += 50;
            }

            if (pe.enchants != null && !pe.enchants.isEmpty()) {
                if (lowerEnchantments.isEmpty() || !lowerEnchantments.contains(pe.enchants.toLowerCase())) {
                    continue;
                }
                score += 25;
            }

            // Bonus for strict model match
            if (pe.customModelData != null && pe.customModelData.equals(customModelData)) {
                score += 2000;
            }

            if (score > bestScore) {
                bestScore = score;
                best = pe;
            }
        }
        return best;
    }

    public static Map<String, List<PriceEntry>> getAllProfiles() {
        return priceLists;
    }

    public static void clearAllProfiles() {
        priceLists.clear();
        customLookup.clear();
        activeProfile = "default";
    }
}
