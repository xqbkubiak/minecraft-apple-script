package pl.bkubiak.bkrynek.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import pl.bkubiak.bkrynek.client.BkRynekClient;
import pl.bkubiak.bkrynek.client.command.ClientCommandRegistration;
import pl.bkubiak.bkrynek.core.config.ServerEntry;

public class SetupManager {
    public enum SetupState {
        INACTIVE,
        SETTING_REFRESH,
        SETTING_CONFIRM,
        SETTING_NEXT_PAGE
    }

    private static SetupState currentState = SetupState.INACTIVE;

    public static void startSetup() {
        currentState = SetupState.SETTING_REFRESH;
    }

    public static boolean isActive() {
        return currentState != SetupState.INACTIVE;
    }

    public static String getSetupInstruction() {
        switch (currentState) {
            case SETTING_REFRESH:
                return "&e[KROK 1] &fNajedź na przycisk &aODŚWIEŻ&f i naciśnij &6Ctrl+S";
            case SETTING_CONFIRM:
                return "&e[KROK 2] &fNajedź na przycisk &dPOTWIERDZENIA&f i naciśnij &6Ctrl+S";
            case SETTING_NEXT_PAGE:
                return "&e[KROK 3] &fNajedź na przycisk &bNASTĘPNA STRONA&f i naciśnij &6Ctrl+S";
            default:
                return "";
        }
    }

    public static void handleSlotSelect(Slot slot) {
        if (!isActive())
            return;

        String address = BkRynekClient.getServerAddress();
        ServerEntry entry = BkRynekClient.findServerEntry(address);

        if (entry == null) {
            sendFeedback("&c[BŁĄD] &fNie znaleziono konfiguracji dla tego serwera!");
            currentState = SetupState.INACTIVE;
            return;
        }

        int slotId = slot.id;

        switch (currentState) {
            case SETTING_REFRESH:
                entry.refreshSlot = slotId;
                sendFeedback("&a[OK] &fUstawiono przycisk &aODŚWIEŻ&f na slot: &e" + slotId);
                currentState = SetupState.SETTING_CONFIRM;
                break;
            case SETTING_CONFIRM:
                entry.confirmSlot = slotId;
                sendFeedback("&a[OK] &fUstawiono przycisk &dPOTWIERDZENIA&f na slot: &e" + slotId);
                currentState = SetupState.SETTING_NEXT_PAGE;
                break;
            case SETTING_NEXT_PAGE:
                entry.marketNextPageSlot = slotId;
                sendFeedback("&a[OK] &fUstawiono przycisk &bNASTĘPNA STRONA&f na slot: &e" + slotId);
                sendFeedback("&2[SUKCES] &fKonfiguracja zakończona! Dane zostały zapisane.");
                currentState = SetupState.INACTIVE;
                ClientCommandRegistration.syncMemoryToConfig();
                break;
            default:
                break;
        }
    }

    public static void showInfo() {
        String address = BkRynekClient.getServerAddress();
        ServerEntry entry = BkRynekClient.findServerEntry(address);

        if (entry == null) {
            sendFeedback("&c[BŁĄD] &fNie znaleziono konfiguracji dla: &e" + address);
            return;
        }

        sendFeedback("&6[INFO] &fKonfiguracja dla: &e" + address);
        sendFeedback(
                "&f- Odśwież (Refresh Slot): &b" + (entry.refreshSlot != null ? entry.refreshSlot : "&cNie ustawiono"));
        sendFeedback("&f- Potwierdź (Confirm Slot): &b"
                + (entry.confirmSlot != null ? entry.confirmSlot : "&cNie ustawiono"));
        sendFeedback("&f- Następna (Next Slot): &b"
                + (entry.marketNextPageSlot != null ? entry.marketNextPageSlot : "&cNie ustawiono"));
    }

    private static void sendFeedback(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String prefix = "&8[&2B&aK &fRynek&8] ";
            client.player.sendMessage(ColorUtils.translateColorCodes(prefix + message), false);
        }
    }

    public static void stopSetup() {
        if (isActive()) {
            currentState = SetupState.INACTIVE;
            sendFeedback("&c[SETUP] &fPrzerwano tryb ustawiania.");
        }
    }
}
