package pl.bkubiak.bkrynek.client;

import pl.bkubiak.bkrynek.client.command.ClientCommandRegistration;
import pl.bkubiak.bkrynek.client.config.ConfigLoader;
import pl.bkubiak.bkrynek.client.keybinding.ToggleScanner;
import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.config.ServerEntry;
import pl.bkubiak.bkrynek.core.config.ServersConfig;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import pl.bkubiak.bkrynek.client.util.ColorUtils;
import pl.bkubiak.bkrynek.core.util.Messages;
import pl.bkubiak.bkrynek.core.util.RemoteAdConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import pl.bkubiak.bkrynek.client.util.AfkController;

import java.util.Map;

public class BkRynekClient implements ClientModInitializer {
	public static ServersConfig serversConfig;
	public static boolean isSumMode = true;
	public static boolean isSniperMode = true;

	@Override
	public void onInitializeClient() {
		System.out.println("[BK-Rynek] Initializing Client Mod...");
		ToggleScanner.init();

        ClientTickEvents.END_CLIENT_TICK.register(AfkController::onTick);

		serversConfig = ConfigLoader.loadConfig();

		// Preload remote ad config removed
		// RemoteAdConfig.preloadAsync();

		for (ServerEntry entry : serversConfig.servers) {
			ClientPriceListManager.setActiveProfile(entry.profileName);
			for (PriceEntry pe : entry.prices) {
				ClientPriceListManager.addPriceEntry(pe);
			}
		}

		ClientPriceListManager.setActiveProfile(serversConfig.defaultProfile);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String address = getServerAddress();
			ServerEntry entry = findServerEntry(address);
			if (entry != null) {
				ClientPriceListManager.setActiveProfile(entry.profileName);
				if (client.player != null) {
					String welcomeMsg = Messages.format("player.join", Map.of("profile", entry.profileName));
					client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
				}
			} else {
				String def = serversConfig.defaultProfile;
				ClientPriceListManager.setActiveProfile(def);
				if (client.player != null) {
					String welcomeMsg = Messages.format("player.join", Map.of("profile", def));
					client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
				}
			}
		});

		ClientCommandRegistration.registerCommands();

		net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			pl.bkubiak.bkrynek.client.util.SearchAutomationController.onChatMessage(message);
		});

		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			if (pl.bkubiak.bkrynek.client.util.SearchAutomationController.isActive()) {
				String text = "&8[&2B&aK &fRynek&8] &fStatus: &aON";
				net.minecraft.text.Text renderedText = ColorUtils.translateColorCodes(text);
				int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
				int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
				int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(renderedText);

				drawContext.drawTextWithShadow(
						MinecraftClient.getInstance().textRenderer,
						renderedText,
						width / 2 - textWidth / 2,
						height - 55,
						0xFFFFFFFF);
			}

			if (pl.bkubiak.bkrynek.client.util.SetupManager.isActive()) {
				String setupText = pl.bkubiak.bkrynek.client.util.SetupManager.getSetupInstruction();
				net.minecraft.text.Text renderedSetup = ColorUtils.translateColorCodes(setupText);
				int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
				int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
				int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(renderedSetup);

				drawContext.drawTextWithShadow(
						MinecraftClient.getInstance().textRenderer,
						renderedSetup,
						width / 2 - textWidth / 2,
						height - 70, // Slightly above the status
						0xFFFFFFFF);
			}
		});
	}

	public static String getServerAddress() {
		if (MinecraftClient.getInstance().getCurrentServerEntry() != null) {
			return MinecraftClient.getInstance().getCurrentServerEntry().address;
		}
		return "singleplayer";
	}

	public static ServerEntry findServerEntry(String address) {
		for (ServerEntry entry : serversConfig.servers) {
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
