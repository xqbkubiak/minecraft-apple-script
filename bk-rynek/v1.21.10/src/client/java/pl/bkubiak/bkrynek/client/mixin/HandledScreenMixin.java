package pl.bkubiak.bkrynek.client.mixin;

import pl.bkubiak.bkrynek.client.BkRynekClient;
import pl.bkubiak.bkrynek.client.keybinding.ToggleScanner;
import pl.bkubiak.bkrynek.client.util.AlarmSoundPlayer;
import pl.bkubiak.bkrynek.client.util.SearchAutomationController;
import pl.bkubiak.bkrynek.client.gui.QuickAddPriceScreen;
import pl.bkubiak.bkrynek.core.config.PriceEntry;
import pl.bkubiak.bkrynek.core.manager.ClientPriceListManager;
import pl.bkubiak.bkrynek.core.scan.ScanEvaluator;
import pl.bkubiak.bkrynek.core.scan.ScanInput;
import pl.bkubiak.bkrynek.core.scan.ScanResult;
import pl.bkubiak.bkrynek.core.util.ColorStripUtils;
import pl.bkubiak.bkrynek.client.util.ColorUtils;
import pl.bkubiak.bkrynek.core.util.EnchantStringParser;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Shadow
	protected int x;
	@Shadow
	protected int y;

	@Shadow
	protected Slot focusedSlot;

	private int lastMatchedCount = 0;
	private int currentMatchedCount = 0;

	@Inject(method = "render", at = @At("HEAD"))
	private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		currentMatchedCount = 0;
	}

	@Inject(method = "drawSlot", at = @At("HEAD"))
	private void onDrawSlotHead(DrawContext context, Slot slot, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) {
			return;
		}

		if (processSlot(context, slot)) {
			currentMatchedCount++;
		}
	}

	// State for debounce
	private boolean wasAPressed = false;
	private boolean wasSPressed = false;

	@Inject(method = "render", at = @At("TAIL"))
	private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
		List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();
		Text title = ((HandledScreen) (Object) this).getTitle();
		SearchAutomationController.onScreenRender(title, handler, slots);

		// Manual key check logic
		checkCustomKeybinds();

		if (!ToggleScanner.scanningEnabled) {
			return;
		}

		if (BkRynekClient.serversConfig != null && BkRynekClient.serversConfig.soundsEnabled) {
			if (currentMatchedCount != lastMatchedCount && currentMatchedCount > 0) {
				AlarmSoundPlayer.playForMatchCount(
						BkRynekClient.serversConfig,
						ClientPriceListManager.getActiveProfile(),
						currentMatchedCount);
			}
		}
		lastMatchedCount = currentMatchedCount;
	}

	private void checkCustomKeybinds() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null)
			return;

		long handle = client.getWindow().getHandle();
		boolean isCtrlDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

		// Check Ctrl+A
		boolean isADown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
		if (isADown && !wasAPressed && isCtrlDown) {
			handleCtrlA();
		}
		wasAPressed = isADown;

		// Check Ctrl+S
		boolean isSDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
		if (isSDown && !wasSPressed && isCtrlDown) {
			handleCtrlS();
		}
		wasSPressed = isSDown;
	}

	private void handleCtrlA() {
		if (focusedSlot != null && focusedSlot.hasStack()) {
			ItemStack stack = focusedSlot.getStack();
			PriceEntry pe = createPriceEntryFromStack(stack);

			MinecraftClient client = MinecraftClient.getInstance();
			client.setScreen(new QuickAddPriceScreen(pe, (Screen) (Object) this, success -> {
				if (success) {
					if (client.player != null) {
						client.player.sendMessage(
								ColorUtils.translateColorCodes("&8[&2B&aK &fRynek&8] &aDodano przedmiot do listy!"),
								false);
					}
				}
			}));
		}
	}

	private void handleCtrlS() {
		if (pl.bkubiak.bkrynek.client.util.SetupManager.isActive()) {
			if (focusedSlot != null) {
				pl.bkubiak.bkrynek.client.util.SetupManager.handleSlotSelect(focusedSlot);
			} else {
				MinecraftClient.getInstance().player.sendMessage(
						ColorUtils.translateColorCodes(
								"&8[&2B&aK &fRynek&8] &cNajeźdź kursor na przedmiot przed naciśnięciem Ctrl+S!"),
						false);
			}
		}
	}

	// onKeyPressed REMOVED to avoid signature mismatch crashes

	private PriceEntry createPriceEntryFromStack(ItemStack stack) {
		PriceEntry pe = new PriceEntry();
		pe.name = ColorStripUtils.stripAllColorsAndFormats(stack.getName().getString());

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);
		List<String> loreLines = new ArrayList<>();
		for (int i = 1; i < tooltip.size(); i++) {
			String line = ColorStripUtils.stripAllColorsAndFormats(tooltip.get(i).getString()).trim();
			if (line.isEmpty())
				continue;

			String lower = line.toLowerCase();
			if (lower.contains("wystawi") || lower.contains("cena") || lower.contains("koszt") ||
					lower.contains("kliknij") || lower.contains("zakup") ||
					lower.contains("wygasnie") || lower.contains("wygaśnie") || lower.contains("expire") ||
					lower.contains("sprzeda") || lower.contains("seller") || lower.contains("buy") ||
					lower.contains("kwota") ||
					lower.contains("when in ") || lower.contains("attack damage") ||
					lower.contains("durability:") || lower.contains("projectile:")) {
				continue;
			}
			loreLines.add(line);
		}
		pe.lore = String.join(";", loreLines);

		pe.material = Registries.ITEM.getId(stack.getItem()).toString();

		String rawEnchants = stack.getEnchantments().toString();
		pe.enchants = EnchantStringParser.parse(rawEnchants);

		pe.componentCount = stack.getComponents().size();
		CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
		pe.customModelData = (cmd != null) ? Math.round(cmd.floats().get(0)) : null;
		pe.requiredCount = stack.getCount();

		return pe;
	}

	private boolean processSlot(DrawContext context, Slot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty())
			return false;

		String displayName = stack.getName().getString();
		String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);
		List<String> loreLines = new ArrayList<>();
		for (int i = 1; i < tooltip.size(); i++) {
			String noColor = ColorStripUtils.stripAllColorsAndFormats(tooltip.get(i).getString()).trim();
			if (noColor.isEmpty())
				continue;
			loreLines.add(noColor);
		}

		String rawEnchants = stack.getEnchantments().toString();
		String enchantmentsString = EnchantStringParser.parse(rawEnchants);
		if (!enchantmentsString.isEmpty()) {
			loreLines.add(enchantmentsString);
		}

		Identifier id = Registries.ITEM.getId(stack.getItem());
		String materialId = id.toString();

		int stackSize = stack.getCount();
		int componentCount = stack.getComponents().size();
		CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
		Integer customModelData = (cmd != null) ? Math.round(cmd.floats().get(0)) : null;

		ScanInput input = new ScanInput(
				noColorName,
				loreLines,
				materialId,
				enchantmentsString,
				stackSize,
				slot.id,
				componentCount,
				customModelData);
		ScanResult result = ScanEvaluator.evaluate(input, BkRynekClient.serversConfig, BkRynekClient.isSumMode);
		if (result.highlight) {
			if (SearchAutomationController.isActive()) {
				SearchAutomationController.onItemMatch(slot, result.foundPrice);
			}
			int realX = slot.x;
			int realY = slot.y;

			int baseColor = result.color;

			int borderColor = (0xFF << 24) | (baseColor & 0x00FFFFFF);
			int fillColor = (0x60 << 24) | (baseColor & 0x00FFFFFF);

			// Używamy fillGradient (bezpieczne w 1.21.8, brak RenderSystem.setShaderColor)
			context.fillGradient(realX, realY, realX + 16, realY + 16, fillColor, fillColor);

			// Ramka
			context.fill(realX, realY, realX + 16, realY + 1, borderColor);
			context.fill(realX, realY + 15, realX + 16, realY + 16, borderColor);
			context.fill(realX, realY + 1, realX + 1, realY + 15, borderColor);
			context.fill(realX + 15, realY + 1, realX + 16, realY + 15, borderColor);

			return true;
		}

		return false;
	}

}
