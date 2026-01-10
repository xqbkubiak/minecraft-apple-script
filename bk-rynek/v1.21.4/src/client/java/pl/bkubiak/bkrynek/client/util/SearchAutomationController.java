package pl.bkubiak.bkrynek.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import pl.bkubiak.bkrynek.client.BkRynekClient;
import pl.bkubiak.bkrynek.core.config.ServerEntry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchAutomationController {
    private static final State state = new State();

    private SearchAutomationController() {
    }

    public static void start(int pages) {
        reset();
        state.active = true;
        sendFeedback("&2Uruchomiono tryb: &e" + (BkRynekClient.isSniperMode ? "SNIPER" : "SCAN"));
        sendFeedback("&7Wpisz &f/ah&7 aby rozpocząć.");
    }

    public static void cancel() {
        if (state.active) {
            state.active = false;
            sendFeedback("&cZatrzymano automatyzację.");
        }
        reset();
    }

    public static boolean isActive() {
        return state.active;
    }

    public static void onScreenRender(Text title, ScreenHandler handler, List<Slot> slots) {
        if (!state.active)
            return;

        ServerEntry entry = findEntry();
        if (entry == null)
            return;

        long now = System.currentTimeMillis();
        String currentTitle = title.getString().toLowerCase();

        // 1. Determine screen type
        boolean titleIsMarket = currentTitle.contains("rynek") || currentTitle.contains("aukcje")
                || currentTitle.contains("ah") || currentTitle.contains("market") ||
                currentTitle.contains("auction") || currentTitle.contains("itemy") ||
                (entry.marketGuiTitle != null && currentTitle.contains(entry.marketGuiTitle.toLowerCase()));

        boolean hasMarketItems = false;
        boolean confirmSlotHasMarketLore = false;

        // Scan slots to find if any of them look like market items
        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            Slot s = slots.get(i);
            if (s != null && s.hasStack()) {
                String lore = s.getStack().getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT, null,
                        net.minecraft.item.tooltip.TooltipType.BASIC).toString().toLowerCase();
                if (lore.contains("wystawi") || lore.contains("seller")) {
                    hasMarketItems = true;
                    if (entry.confirmSlot != null && i == entry.confirmSlot) {
                        confirmSlotHasMarketLore = true;
                    }
                }
            }
        }

        boolean confirmKeywords = currentTitle.contains("potwierdz") || currentTitle.contains("potwierdź") ||
                currentTitle.contains("confirm") || currentTitle.contains("na pewno") || currentTitle.contains("sure")
                || (currentTitle.contains("zakup") && !hasMarketItems);

        // Advanced detection: checking items in slots if title is ambiguous
        if (!confirmKeywords && entry.confirmSlot != null && entry.confirmSlot < slots.size()) {
            Slot s = slots.get(entry.confirmSlot);
            if (s != null && !s.getStack().isEmpty()) {
                String name = s.getStack().getName().getString().toLowerCase();
                if (name.contains("zakup") || name.contains("potwierdz") || name.contains("potwierdź") ||
                        name.contains("confirm") || name.contains("kliknij")) {
                    confirmKeywords = true;
                }
            }
        }

        // Final safety: if the confirm slot contains seller info, it's NOT a confirm
        // screen
        if (confirmSlotHasMarketLore) {
            confirmKeywords = false;
        }

        boolean isMarket = hasMarketItems || (titleIsMarket && !confirmKeywords);

        if (isMarket) {
            if (state.marketFirstSeenTime == 0)
                state.marketFirstSeenTime = now;
        } else {
            state.marketFirstSeenTime = 0;
        }

        // 2. Handle Waiting for Chat Message state
        if (state.waitingForConfirmMsg) {
            // Speed up if in market, otherwise wait for timeout
            long timeoutLimit = isMarket ? state.confirmMsgTimeout - 2000 : state.confirmMsgTimeout;
            if (now > timeoutLimit) {
                state.waitingForConfirmMsg = false;
                sendFeedback("&eBrak potwierdzenia na chacie. Kontynuuję...");
                if (!isMarket)
                    restartScan(now);
            }
            return;
        }

        // 3. Main Logic Branching
        if (isMarket) {
            // Safety: Reset buying state after a short while to prevent getting stuck
            if (state.buyingItem && (now - state.buyingStartTime > 500)) {
                state.buyingItem = false;
            }
        } else if (state.buyingItem || confirmKeywords) {
            if (state.buyingItem || confirmKeywords) {
                // Safety timeout
                if (state.buyingItem && now - state.buyingStartTime > 4000) {
                    sendFeedback("&cZakup przekroczył limit czasu. Resetuję...");
                    state.buyingItem = false;
                    restartScan(now);
                    return;
                }

                boolean titleChanged = state.startBuyTitle.isEmpty() || !currentTitle.equals(state.startBuyTitle);

                // Use custom confirm delay or default 400ms
                int confirmDelay = (entry.marketConfirmDelayMs != null && entry.marketConfirmDelayMs > 0)
                        ? entry.marketConfirmDelayMs
                        : 400;

                if (titleChanged || confirmKeywords || (now - state.buyingStartTime > 600)) {
                    if (now - state.lastActionMs > confirmDelay) {
                        if (entry.confirmSlot != null) {
                            clickSlot(handler, entry.confirmSlot);
                            state.buyingItem = false;
                            state.lastActionMs = now;
                            sendFeedback("&aPotwierdzam zakup...");
                            state.waitingForConfirmMsg = true;
                            state.confirmMsgTimeout = now + 4000;
                        } else {
                            state.buyingItem = false;
                            sendFeedback("&c[Błąd] &fNie ustawiono slotu potwierdzenia! Użyj &e/bkr setup&f.");
                        }
                    }
                }
            }
            return;
        } else {
            // Not a market and not a confirm screen
            return;
        }

        // 4. Logic for SNIPER / SCAN
        // Dynamic delay from config or current super-fast defaults
        int baseDelay = (entry.marketNextDelayMs != null && entry.marketNextDelayMs > 0)
                ? entry.marketNextDelayMs
                : (BkRynekClient.isSniperMode ? 150 : 200);

        if (now - state.lastActionMs < baseDelay)
            return;

        if (BkRynekClient.isSniperMode) {
            if (entry.refreshSlot != null) {
                clickSlot(handler, entry.refreshSlot);
                state.lastActionMs = now;
            }
        } else {
            // SCAN Mode
            Slot nextSlot = findSlot(slots, entry.marketNextPageSlot);

            // 1. Try to extract numbers from title or next page button
            boolean isLastPage = false;
            String nextSlotName = (nextSlot != null) ? nextSlot.getStack().getName().getString() : "";
            int[] pages = extractPageNumbers(nextSlotName);
            if (pages == null) {
                pages = extractPageNumbers(title.getString());
            }

            if (pages != null) {
                if (pages[0] >= pages[1] && pages[1] > 0) {
                    isLastPage = true;
                }
            }

            if (isLastPage) {
                sendFeedback("&eKoniec stron. Powrót na start.");
                restartScan(now);
                return;
            }

            // 2. Signature detection fallback
            String currentSignature = buildPageSignature(slots);
            if (nextSlot != null && !nextSlot.getStack().isEmpty()) {
                // If it looks like we clicked but items didn't change for 400ms -> probably
                // last page
                if (currentSignature.equals(state.lastPageSignature)) {
                    if (now - state.lastClickMs > 800) { // Increased from 400 to 800
                        restartScan(now);
                    }
                } else {
                    clickSlot(handler, entry.marketNextPageSlot);
                    state.lastClickMs = now;
                    state.lastActionMs = now;
                    state.lastPageSignature = currentSignature;
                }
            } else {
                if (now - state.marketFirstSeenTime > 1500) {
                    restartScan(now);
                }
            }
        }
    }

    private static void restartScan(long now) {
        if (state.isRestarting)
            return;
        state.isRestarting = true;

        ServerEntry entry = findEntry();
        int restartDelay = (entry != null && entry.marketOpenDelayMs != null && entry.marketOpenDelayMs > 0)
                ? entry.marketOpenDelayMs
                : 800;

        sendFeedback("&eRestartuję rynek...");
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.closeHandledScreen();

                new Thread(() -> {
                    try {
                        Thread.sleep(restartDelay);
                        MinecraftClient.getInstance().execute(() -> {
                            state.isRestarting = false;
                            state.buyingItem = false;
                            state.waitingForConfirmMsg = false;
                            if (state.active) {
                                sendMarketCommand();
                            }
                        });
                    } catch (Exception e) {
                        state.isRestarting = false;
                        e.printStackTrace();
                    }
                }).start();
            } else {
                state.isRestarting = false;
            }
        });
        state.lastActionMs = now + Math.max(restartDelay + 500, 1500);
        state.lastPageSignature = "";
    }

    public static void onItemMatch(Slot slot, double price) {
        // Broad market detection to avoid buying outside of AH (e.g. in inventory)
        // Usually, AH slots are 0-44 or 0-53. Inventory slots start after that.
        if (slot.id > 53)
            return;

        // We allow buying even if detection is not 100% sure about market title,
        // as long as we are NOT in a confirm screen and NOT already buying.
        if (!state.active || state.buyingItem || state.waitingForConfirmMsg)
            return;

        long now = System.currentTimeMillis();
        // Lower safety delay to 100ms for better responsiveness
        if (now - state.lastActionMs < 100)
            return;

        state.buyingItem = true;
        state.buyingStartTime = now;
        state.lastActionMs = now;
        state.startBuyTitle = MinecraftClient.getInstance().player.currentScreenHandler.getClass().getSimpleName(); // Fallback
                                                                                                                    // start
                                                                                                                    // title
                                                                                                                    // info

        // Better title capture if possible
        var screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null) {
            state.startBuyTitle = screen.getTitle().getString().toLowerCase();
        }

        state.lastMatchedItemName = slot.getStack().getName().getString();

        // Format price for webhook
        state.lastMatchedPrice = pl.bkubiak.bkrynek.core.util.PriceFormatter.formatPrice(price);

        clickSlot(MinecraftClient.getInstance().player.currentScreenHandler, slot.id);
        sendFeedback("&6Znaleziono przedmiot! Kupuję...");
    }

    public static void onChatMessage(Text message) {
        if (!state.active)
            return;

        long now = System.currentTimeMillis();
        if (state.waitingForConfirmMsg && now > state.confirmMsgTimeout) {
            state.waitingForConfirmMsg = false;
        }

        if (!state.waitingForConfirmMsg && !state.buyingItem)
            return;

        // Verify if it's a real system message by checking formatting
        // We use string representation with codes to be sure (contains '§')
        String rawText = message.getString(); // We still use clean for matching
        String cleanText = pl.bkubiak.bkrynek.core.util.ColorStripUtils.stripAllColorsAndFormats(rawText).toLowerCase();

        // Extra check: System messages on these servers always have colors/formatting
        boolean hasFormatting = !message.getStyle().isEmpty()
                || message.getSiblings().stream().anyMatch(s -> !s.getStyle().isEmpty());

        // Debug for user: help identify why message is not matching
        // if (BkRynekClient.debugMode) sendFeedback("&8[DEBUG] Chat: " + cleanText + "
        // (fmt=" + hasFormatting + ")");

        if (!hasFormatting)
            return;

        // Check for various forms of purchase confirmation
        boolean hasSuccess = cleanText.contains("kupiles przedmiot") ||
                cleanText.contains("kupiłeś przedmiot") ||
                cleanText.contains("pomyslnie zakupiles") ||
                cleanText.contains("pomyślnie zakupiłeś") ||
                cleanText.contains("zakupiono przedmiot") ||
                cleanText.contains("zakupiono produkt") ||
                cleanText.contains("zakupiles produkt") ||
                cleanText.contains("kupiłeś produkt");

        // Custom success messages check
        ServerEntry entry = findEntry();
        if (entry != null && entry.successMessages != null) {
            for (String msg : entry.successMessages) {
                if (cleanText.contains(msg.toLowerCase())) {
                    hasSuccess = true;
                    break;
                }
            }
        }

        if (hasSuccess) {
            state.waitingForConfirmMsg = false;
            sendFeedback("&a[WEBHOOK] &fSukces zakupu potwierdzony! Wysyłam powiadomienie.");

            String webhook = BkRynekClient.serversConfig.discordWebhookUrl;
            if (webhook != null && !webhook.isEmpty()) {
                DiscordWebhook.sendPurchaseNotification(webhook,
                        state.lastMatchedItemName,
                        state.lastMatchedPrice,
                        BkRynekClient.getServerAddress());
            }
            return;
        }

        // Error detection: Item already sold or removed
        boolean hasError = cleanText.contains("juz prawdopodobnie usuniety")
                || cleanText.contains("już prawdopodobnie usunięty")
                || cleanText.contains("sprzedany") || cleanText.contains("nie jest juz dostepny")
                || cleanText.contains("nie jest już dostępny") || cleanText.contains("produkt ten zostal juz")
                || cleanText.contains("produkt ten został już");

        // Custom error messages check
        if (entry != null && entry.errorMessages != null) {
            for (String msg : entry.errorMessages) {
                if (cleanText.contains(msg.toLowerCase())) {
                    hasError = true;
                    break;
                }
            }
        }

        if (hasError) {
            if (state.buyingItem || state.waitingForConfirmMsg) {
                state.buyingItem = false;
                state.waitingForConfirmMsg = false;
                sendFeedback("&ePrzedmiot został już sprzedany. Szukam dalej...");
                restartScan(now);
            }
            return;
        }

        // Error detection: No money / Insufficient funds
        if (cleanText.contains("nie stac cie") || cleanText.contains("nie stać cię")
                || cleanText.contains("nie stac ciebie") || cleanText.contains("nie stać ciebie")
                || cleanText.contains("nie posiadasz wystarczajaco")
                || cleanText.contains("nie posiadasz wystarczająco")) {

            state.active = false;
            state.buyingItem = false;
            state.waitingForConfirmMsg = false;

            String errorMsg = "Brak środków na koncie! Zatrzymuję bota.";
            sendFeedback("&c" + errorMsg);

            String webhook = BkRynekClient.serversConfig.discordWebhookUrl;
            if (webhook != null && !webhook.isEmpty()) {
                DiscordWebhook.sendErrorNotification(webhook, errorMsg, BkRynekClient.getServerAddress());
            }
        }
    }

    private static int[] extractPageNumbers(String text) {
        if (text == null)
            return null;
        Pattern p = Pattern.compile("(\\d+)/(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static void onScreenClosed() {
    }

    private static Slot findSlot(List<Slot> slots, Integer id) {
        if (id == null)
            return null;
        for (Slot s : slots) {
            if (s.id == id)
                return s;
        }
        return null;
    }

    private static void clickSlot(ScreenHandler handler, int slotId) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null)
            return;
        client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }

    private static String buildPageSignature(List<Slot> slots) {
        if (slots == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            Slot s = slots.get(i);
            if (s != null && !s.getStack().isEmpty()) {
                sb.append(s.getStack().getName().getString());
            }
            sb.append("|");
        }
        return sb.toString();
    }

    private static void sendMarketCommand() {
        ServerEntry entry = findEntry();
        String cmd = (entry != null && entry.marketCommands != null && !entry.marketCommands.isEmpty())
                ? entry.marketCommands.get(0)
                : "ah";
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);

        sendFeedback("&7Wysyłam: &f/" + cmd);
        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand(cmd);
    }

    private static ServerEntry findEntry() {
        return BkRynekClient.findServerEntry(BkRynekClient.getServerAddress());
    }

    private static void reset() {
        state.active = false;
        state.buyingItem = false;
        state.buyingStartTime = 0;
        state.lastActionMs = 0;
        state.lastClickMs = 0;
        state.lastPageSignature = "";
        state.waitingForConfirmMsg = false;
        state.isRestarting = false;
        state.marketFirstSeenTime = 0;
    }

    private static void sendFeedback(String msg) {
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            String prefix = "&8[&2B&aK &fRynek&8] ";
            player.sendMessage(ColorUtils.translateColorCodes(prefix + msg), false);
        }
    }

    private static class State {
        boolean active = false;
        long buyingStartTime = 0;
        String startBuyTitle = "";
        boolean buyingItem = false;
        long lastActionMs = 0;
        long lastClickMs = 0;
        String lastPageSignature = "";

        boolean waitingForConfirmMsg = false;
        long confirmMsgTimeout = 0;
        String lastMatchedItemName = "";
        String lastMatchedPrice = "";

        long marketFirstSeenTime = 0;
        boolean isRestarting = false; // Flag to prevent double /ah commands
    }
}
