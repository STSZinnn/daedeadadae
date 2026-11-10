package dev.frostclient.mixin;
import dev.frostclient.FrostClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes mouse presses without changing their behavior. Optional for compatibility. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method="onButton",at=@At("HEAD"),require=0)
    private void frostclient$click(long handle,int button,int action,int modifiers,CallbackInfo ci) {
        if (action==1 && (button==0 || button==1) && Minecraft.getInstance().screen==null)
            FrostClient.CLICKS.record(button==0);
    }
}
