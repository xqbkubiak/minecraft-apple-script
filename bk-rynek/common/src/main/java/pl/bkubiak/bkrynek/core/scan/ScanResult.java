package pl.bkubiak.bkrynek.core.scan;

public class ScanResult {
    public final boolean highlight;
    public final int color;
    public final double foundPrice;

    public ScanResult(boolean highlight, int color, double foundPrice) {
        this.highlight = highlight;
        this.color = color;
        this.foundPrice = foundPrice;
    }

    public static ScanResult noHighlight() {
        return new ScanResult(false, 0, -1);
    }
}
