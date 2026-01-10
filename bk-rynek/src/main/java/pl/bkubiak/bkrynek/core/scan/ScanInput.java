package pl.bkubiak.bkrynek.core.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanInput {
    public final String noColorName;
    public final List<String> loreLines;
    public final String materialId;
    public final String enchantments;
    public final int stackSize;
    public final int slotId;
    public final int componentCount;
    public final Integer customModelData;

    public ScanInput(String noColorName,
            List<String> loreLines,
            String materialId,
            String enchantments,
            int stackSize,
            int slotId,
            int componentCount,
            Integer customModelData) {
        this.noColorName = noColorName;
        this.loreLines = loreLines == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(loreLines));
        this.materialId = materialId;
        this.enchantments = enchantments;
        this.stackSize = stackSize;
        this.slotId = slotId;
        this.componentCount = componentCount;
        this.customModelData = customModelData;
    }
}
