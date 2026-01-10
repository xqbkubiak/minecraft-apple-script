package pl.bkubiak.bkrynek.client.mixin;

import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.bkubiak.bkrynek.core.config.ServersConfig;
import pl.bkubiak.bkrynek.client.util.ServerListPatcher;

@Mixin(ServerList.class)
public class ServerListMixin {

    @Inject(method = "loadFile", at = @At("TAIL"), require = 0, remap = false)
    private void bkrynek$afterLoadFile(CallbackInfo ci) {
        bkrynek$injectOrMove();
    }

    @Inject(method = "load", at = @At("TAIL"), require = 0, remap = false)
    private void bkrynek$afterLoad(CallbackInfo ci) {
        bkrynek$injectOrMove();
    }

    @Unique
    private void bkrynek$injectOrMove() {
        if (!ServersConfig.adsEnabled)
            return;
        ServerListPatcher.injectOrMove((ServerList) (Object) this);
    }
}
