package dev.frostclient.mixin;

import dev.frostclient.FrostClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Changes only the vanilla crosshair draw, retaining attack cooldown and spectator behavior. */
@Mixin(Gui.class)
public abstract class CrosshairMixin {
    private static final Identifier FROST_SNOWFLAKE = Identifier.fromNamespaceAndPath("frostclient", "hud/snowflake_crosshair");

    @ModifyArgs(method = "renderCrosshair", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
        ordinal = 0))
    private void frostClient$snowflakeSprite(Args args) {
        if(FrostClient.CONFIG.enabled("Snowflake Crosshair")) {
            // Vanilla CROSSHAIR inverts colors: use regular blending to preserve ice-blue pixels.
            args.set(0, RenderPipelines.GUI_TEXTURED);
            args.set(1, FROST_SNOWFLAKE);
        }
    }
}
