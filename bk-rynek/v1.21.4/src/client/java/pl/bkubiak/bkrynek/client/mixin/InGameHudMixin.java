package pl.bkubiak.bkrynek.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.bkubiak.bkrynek.client.util.SearchAutomationController;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message != null) {
            SearchAutomationController.onChatMessage(message);
        }
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Text title, CallbackInfo ci) {
        if (title != null) {
            SearchAutomationController.onChatMessage(title);
        }
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
        if (subtitle != null) {
            SearchAutomationController.onChatMessage(subtitle);
        }
    }
}
