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

	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
		List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();
		Text title = ((HandledScreen) (Object) this).getTitle();
		SearchAutomationController.onScreenRender(title, handler, slots);

		if (!ToggleScanner.scanningEnabled) {
			return;
		}

		int matchedCount = 0;
		for (Slot slot : slots) {
			if (processSlot(context, slot)) {
				matchedCount++;
			}
		}

		if (BkRynekClient.serversConfig != null && BkRynekClient.serversConfig.soundsEnabled) {
			if (matchedCount != lastMatchedCount && matchedCount > 0) {
				AlarmSoundPlayer.playForMatchCount(
						BkRynekClient.serversConfig,
						ClientPriceListManager.getActiveProfile(),
						matchedCount);
			}
		}
		lastMatchedCount = matchedCount;

	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void onRemoved(CallbackInfo ci) {
		SearchAutomationController.onScreenClosed();
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
			if (focusedSlot != null && focusedSlot.hasStack()) {
				ItemStack stack = focusedSlot.getStack();
				PriceEntry pe = createPriceEntryFromStack(stack);

				MinecraftClient client = MinecraftClient.getInstance();
				client.setScreen(new QuickAddPriceScreen(pe, (Screen) (Object) this, success -> {
					if (success) {
						if (client.player != null) {
							client.player.sendMessage(
									ColorUtils.translateColorCodes(
											"&8[&2B&aK &fRynek&8] &aDodano przedmiot do listy! &7(x" + pe.requiredCount
													+ ")"),
									false);
						}
					}
				}));
				cir.setReturnValue(true);
			}
		} else if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
			if (pl.bkubiak.bkrynek.client.util.SetupManager.isActive()) {
				if (focusedSlot != null) {
					pl.bkubiak.bkrynek.client.util.SetupManager.handleSlotSelect(focusedSlot);
				} else {
					MinecraftClient.getInstance().player.sendMessage(
							ColorUtils.translateColorCodes(
									"&8[&2B&aK &fRynek&8] &cNajeźdź kursor na przedmiot przed naciśnięciem Ctrl+S!"),
							false);
				}
				cir.setReturnValue(true);
			}
		}
	}

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
			// Aggressive filtering of market metadata
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
		pe.customModelData = (cmd != null) ? cmd.value() : null;

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
		Integer customModelData = (cmd != null) ? cmd.value() : null;

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
			int realX = this.x + slot.x;
			int realY = this.y + slot.y;

			int baseColor = result.color;

			// Force full opacity for border
			int borderColor = (0xFF << 24) | (baseColor & 0x00FFFFFF);

			// Semi-transparent fill (approx 35%)
			int fillColor = (0x60 << 24) | (baseColor & 0x00FFFFFF);

			// Draw fill
			context.fill(realX, realY, realX + 16, realY + 16, fillColor);

			// Draw border (1px)
			context.fill(realX, realY, realX + 16, realY + 1, borderColor); // Top
			context.fill(realX, realY + 15, realX + 16, realY + 16, borderColor); // Bottom
			context.fill(realX, realY + 1, realX + 1, realY + 15, borderColor); // Left
			context.fill(realX + 15, realY + 1, realX + 16, realY + 15, borderColor); // Right

			return true;
		}

		return false;
	}

}
