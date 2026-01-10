package pl.bkubiak.bkrynek.client.command;

import pl.bkubiak.bkrynek.client.keybinding.ToggleScanner;
import pl.bkubiak.bkrynek.client.BkRynekClient;
import pl.bkubiak.bkrynek.client.gui.BkRynekScreen;
import pl.bkubiak.bkrynek.core.util.CompositeKeyUtil;
import pl.bkubiak.bkrynek.core.util.Messages;
import pl.bkubiak.bkrynek.client.config.ConfigLoader;
import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.config.ServerEntry;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;
import pl.bkubiak.bkrynek.core.util.PriceFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import static pl.bkubiak.bkrynek.client.config.ConfigLoader.saveAllConfigs;

import java.util.List;
import java.util.Map;

public class ClientCommandRegistration {

        public static void registerCommands() {
                ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistration::registerBkrCommand);
        }

        private static void registerBkrCommand(
                        CommandDispatcher<FabricClientCommandSource> dispatcher,
                        CommandRegistryAccess registryAccess) {
                dispatcher.register(
                                ClientCommandManager.literal("bkr")
                                                .executes(ctx -> {
                                                        String activeProfile = ClientPriceListManager
                                                                        .getActiveProfile();
                                                        String message = Messages.format("mod.info",
                                                                        Map.of("profile", activeProfile));
                                                        ctx.getSource().sendFeedback(CommandUi.colored(message));
                                                        return 1;
                                                })
                                                .then(ClientCommandManager.literal("scan")
                                                                .executes(ctx -> {
                                                                        ToggleScanner.scanningEnabled = !ToggleScanner.scanningEnabled;
                                                                        String msgKey = ToggleScanner.scanningEnabled
                                                                                        ? "command.scanner.toggle.on"
                                                                                        : "command.scanner.toggle.off";
                                                                        ctx.getSource().sendFeedback(CommandUi
                                                                                        .colored(Messages.get(msgKey)));
                                                                        return 1;
                                                                }))
                                                .then(ClientCommandManager.literal("gui")
                                                                .executes(ctx -> {
                                                                        MinecraftClient.getInstance().execute(() -> {
                                                                                try {
                                                                                        System.out.println(
                                                                                                        "[BK-Rynek] Opening GUI with 100ms delay to avoid overlap...");
                                                                                        new Thread(() -> {
                                                                                                try {
                                                                                                        Thread.sleep(100);
                                                                                                        MinecraftClient.getInstance()
                                                                                                                        .execute(() -> {
                                                                                                                                System.out.println(
                                                                                                                                                "[BK-Rynek] Delayed execution of setScreen...");
                                                                                                                                MinecraftClient.getInstance()
                                                                                                                                                .setScreen(new BkRynekScreen());
                                                                                                                        });
                                                                                                } catch (Exception e) {
                                                                                                        e.printStackTrace();
                                                                                                }
                                                                                        }).start();
                                                                                } catch (Exception e) {
                                                                                        System.err.println(
                                                                                                        "[BK-Rynek] Failed to initiate GUI open: "
                                                                                                                        + e.getMessage());
                                                                                        e.printStackTrace();
                                                                                }
                                                                        });
                                                                        return 1;
                                                                }))
                                                .then(ClientCommandManager.literal("profiles")
                                                                .executes(ctx -> {
                                                                        String allProfiles = ClientPriceListManager
                                                                                        .listProfiles();
                                                                        String[] profiles = allProfiles.split(",\\s*");
                                                                        MutableText finalText = CommandUi.colored(
                                                                                        Messages.get("command.profiles.header"));
                                                                        finalText.append(Text.literal("\n"));

                                                                        String activeProfile = ClientPriceListManager
                                                                                        .getActiveProfile();
                                                                        for (String profile : profiles) {
                                                                                String trimmedProfile = profile.trim();
                                                                                String lineTemplate;
                                                                                if (trimmedProfile.equals(
                                                                                                activeProfile)) {
                                                                                        lineTemplate = Messages.format(
                                                                                                        "profile.picked.line",
                                                                                                        Map.of("profile",
                                                                                                                        trimmedProfile));
                                                                                        finalText.append(CommandUi
                                                                                                        .colored(lineTemplate));
                                                                                } else {
                                                                                        lineTemplate = Messages.format(
                                                                                                        "profile.available.line",
                                                                                                        Map.of("profile",
                                                                                                                        trimmedProfile));
                                                                                        MutableText lineText = CommandUi
                                                                                                        .clickable(
                                                                                                                        lineTemplate,
                                                                                                                        ClickEvent.Action.RUN_COMMAND,
                                                                                                                        "/bkr profile " + trimmedProfile,
                                                                                                                        "Kliknij, aby zmienic profil na "
                                                                                                                                        + trimmedProfile);
                                                                                        finalText.append(lineText);
                                                                                }
                                                                                finalText.append(Text.literal("\n"));
                                                                        }
                                                                        ctx.getSource().sendFeedback(finalText);
                                                                        return 1;
                                                                }))
                                                .then(ClientCommandManager.literal("profile")
                                                                .then(ClientCommandManager
                                                                                .argument("profile",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                .executes(ctx -> {
                                                                                        String profile = StringArgumentType
                                                                                                        .getString(ctx, "profile");
                                                                                        ClientPriceListManager
                                                                                                        .setActiveProfile(
                                                                                                                        profile);
                                                                                        String msg = Messages.format(
                                                                                                        "command.profile.change",
                                                                                                        Map.of("profile",
                                                                                                                        profile));
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        msg));
                                                                                        return 1;
                                                                                })))

                                                .then(ClientCommandManager.literal("list")
                                                                .executes(ctx -> {
                                                                        String activeProfile = ClientPriceListManager
                                                                                        .getActiveProfile();
                                                                        List<PriceEntry> entries = ClientPriceListManager
                                                                                        .getAllProfiles()
                                                                                        .get(activeProfile);
                                                                        MutableText finalText = Text.empty();

                                                                        if (entries != null) {
                                                                                for (PriceEntry pe : entries) {
                                                                                        String compositeKey = CompositeKeyUtil
                                                                                                        .getCompositeKeyFromEntry(
                                                                                                                        pe);
                                                                                        String friendlyName = CompositeKeyUtil
                                                                                                        .getFriendlyName(
                                                                                                                        compositeKey);
                                                                                        String priceStr = PriceFormatter
                                                                                                        .formatPrice(pe.maxPrice);

                                                                                        String itemLineStr = Messages
                                                                                                        .format("pricelist.item_line",
                                                                                                                        Map.of("item", friendlyName,
                                                                                                                                        "price",
                                                                                                                                        priceStr));
                                                                                        MutableText lineText = CommandUi
                                                                                                        .colored(itemLineStr)
                                                                                                        .append(Text.literal(
                                                                                                                        "\n"));
                                                                                        finalText.append(lineText);
                                                                                }
                                                                        }

                                                                        String msgHeader = Messages.format(
                                                                                        "command.list",
                                                                                        Map.of("profile", activeProfile,
                                                                                                        "list", ""));
                                                                        ctx.getSource().sendFeedback(
                                                                                        CommandUi.colored(msgHeader));
                                                                        ctx.getSource().sendFeedback(finalText);
                                                                        return 1;
                                                                }))
                                                .then(ClientCommandManager.literal("pomoc")
                                                                .executes(ctx -> {
                                                                        ctx.getSource().sendFeedback(CommandUi.colored(
                                                                                        Messages.get("command.help")));
                                                                        return 1;
                                                                }))
                                                .then(ClientCommandManager.literal("setup")
                                                                .executes(ctx -> {
                                                                        pl.bkubiak.bkrynek.client.util.SetupManager
                                                                                        .startSetup();
                                                                        return 1;
                                                                })
                                                                .then(ClientCommandManager.literal("info")
                                                                                .executes(ctx -> {
                                                                                        pl.bkubiak.bkrynek.client.util.SetupManager
                                                                                                        .showInfo();
                                                                                        return 1;
                                                                                }))
                                                                .then(ClientCommandManager.literal("cancel")
                                                                                .executes(ctx -> {
                                                                                        pl.bkubiak.bkrynek.client.util.SetupManager
                                                                                                        .stopSetup();
                                                                                        return 1;
                                                                                })))
                                                .then(ClientCommandManager.literal("webhook")
                                                                .then(ClientCommandManager
                                                                                .argument("url", StringArgumentType
                                                                                                .greedyString())
                                                                                .executes(ctx -> {
                                                                                        String url = StringArgumentType
                                                                                                        .getString(ctx, "url");
                                                                                        BkRynekClient.serversConfig.discordWebhookUrl = url;
                                                                                        saveAllConfigs(BkRynekClient.serversConfig);
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        pl.bkubiak.bkrynek.client.util.ColorUtils
                                                                                                                        .translateColorCodes(
                                                                                                                                        "&8[&2B&aK &fRynek&8] &aWebhook został ustawiony!"));
                                                                                        return 1;
                                                                                })))
                                                .then(ClientCommandManager.literal("config")

                                                                .then(ClientCommandManager.literal("reload")
                                                                                .executes(ctx -> {
                                                                                        BkRynekClient.serversConfig = ConfigLoader
                                                                                                        .loadConfig();
                                                                                        ClientPriceListManager
                                                                                                        .clearAllProfiles();
                                                                                        reinitProfilesFromConfig();
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        Messages.get("command.config.reload.success")));
                                                                                        return 1;
                                                                                })))
                                                .then(ClientCommandManager.literal("sounds")
                                                                .executes(ctx -> {
                                                                        boolean current = BkRynekClient.serversConfig.soundsEnabled;
                                                                        String msg = current
                                                                                        ? Messages.get("command.sounds.current_on")
                                                                                        : Messages.get("command.sounds.current_off");
                                                                        ctx.getSource().sendFeedback(
                                                                                        CommandUi.colored(msg));
                                                                        return 1;
                                                                })
                                                                .then(ClientCommandManager.literal("on")
                                                                                .executes(ctx -> {
                                                                                        BkRynekClient.serversConfig.soundsEnabled = true;
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        Messages.get("command.sounds.enabled")));
                                                                                        saveAllConfigs(BkRynekClient.serversConfig);
                                                                                        return 1;
                                                                                }))
                                                                .then(ClientCommandManager.literal("off")
                                                                                .executes(ctx -> {
                                                                                        BkRynekClient.serversConfig.soundsEnabled = false;
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        Messages.get("command.sounds.disabled")));
                                                                                        saveAllConfigs(BkRynekClient.serversConfig);
                                                                                        return 1;
                                                                                })))
                                                .then(ClientCommandManager.literal("afk")
                                                                .executes(ctx -> {
                                                                        boolean enabled = !pl.bkubiak.bkrynek.client.util.AfkController
                                                                                        .isEnabled();
                                                                        if (enabled) {
                                                                                pl.bkubiak.bkrynek.client.util.AfkController
                                                                                                .enable();
                                                                                ctx.getSource().sendFeedback(CommandUi
                                                                                                .colored(Messages.get(
                                                                                                                "command.afk.enabled")));
                                                                        } else {
                                                                                pl.bkubiak.bkrynek.client.util.AfkController
                                                                                                .disable();
                                                                                ctx.getSource().sendFeedback(CommandUi
                                                                                                .colored(Messages.get(
                                                                                                                "command.afk.disabled")));
                                                                        }
                                                                        return 1;
                                                                })
                                                                .then(ClientCommandManager.literal("on")
                                                                                .executes(ctx -> {
                                                                                        pl.bkubiak.bkrynek.client.util.AfkController
                                                                                                        .enable();
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        Messages.get("command.afk.enabled")));
                                                                                        return 1;
                                                                                }))
                                                                .then(ClientCommandManager.literal("off")
                                                                                .executes(ctx -> {
                                                                                        pl.bkubiak.bkrynek.client.util.AfkController
                                                                                                        .disable();
                                                                                        ctx.getSource().sendFeedback(
                                                                                                        CommandUi.colored(
                                                                                                                        Messages.get("command.afk.disabled")));
                                                                                        return 1;
                                                                                }))));
        }

        public static void syncMemoryToConfig() {
                if (BkRynekClient.serversConfig == null || BkRynekClient.serversConfig.servers == null) {
                        return;
                }
                Map<String, List<PriceEntry>> allProfiles = ClientPriceListManager.getAllProfiles();

                for (ServerEntry se : BkRynekClient.serversConfig.servers) {
                        List<PriceEntry> memList = allProfiles.getOrDefault(se.profileName, List.of());

                        se.prices.clear();
                        for (PriceEntry src : memList) {
                                PriceEntry pe = new PriceEntry();
                                pe.name = src.name;
                                pe.maxPrice = src.maxPrice;
                                pe.lore = src.lore;
                                pe.material = src.material;
                                pe.enchants = src.enchants;
                                pe.componentCount = src.componentCount;
                                pe.customModelData = src.customModelData;
                                se.prices.add(pe);
                        }
                }

                saveAllConfigs(BkRynekClient.serversConfig);
        }

        private static void reinitProfilesFromConfig() {
                for (ServerEntry entry : BkRynekClient.serversConfig.servers) {
                        ClientPriceListManager.setActiveProfile(entry.profileName);
                        for (PriceEntry pe : entry.prices) {
                                ClientPriceListManager.addPriceEntry(pe);
                        }
                }

                String address = BkRynekClient.getServerAddress();
                ServerEntry serverEntry = findServerEntryByAddress(address);
                if (serverEntry != null) {
                        ClientPriceListManager.setActiveProfile(serverEntry.profileName);
                } else {
                        ClientPriceListManager.setActiveProfile(BkRynekClient.serversConfig.defaultProfile);
                }
        }

        private static ServerEntry findServerEntryByAddress(String address) {
                if (BkRynekClient.serversConfig == null || BkRynekClient.serversConfig.servers == null)
                        return null;
                for (ServerEntry entry : BkRynekClient.serversConfig.servers) {
                        for (String domain : entry.domains) {
                                if (address.equalsIgnoreCase(domain) ||
                                                address.toLowerCase().endsWith("." + domain.toLowerCase())) {
                                        return entry;
                                }
                        }
                }
                return null;
        }

}
