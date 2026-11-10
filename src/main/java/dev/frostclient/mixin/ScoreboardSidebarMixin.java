package dev.frostclient.mixin;

import dev.frostclient.FrostClient;
import dev.frostclient.HudLayout;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repositions and scales the actual vanilla server sidebar, preserving its colors and lines. */
@Mixin(Gui.class)
public abstract class ScoreboardSidebarMixin {
    private boolean frost$sidebarPushed=false;

    @Inject(method="displayScoreboardSidebar",at=@At("HEAD"),cancellable=true)
    private void frost$beforeScoreboard(GuiGraphics g,Objective objective,CallbackInfo ci){
        if(!FrostClient.CONFIG.enabled("Server Scoreboard")){
            ci.cancel();return;
        }
        var def=HudLayout.defaults("Server Scoreboard",g.guiWidth(),g.guiHeight());
        var current=HudLayout.rect("Server Scoreboard",g.guiWidth(),g.guiHeight());
        float scale=HudLayout.scale("Server Scoreboard");
        float dx=current.x()-def.x(),dy=current.y()-def.y();
        float anchorX=g.guiWidth()-3f,anchorY=g.guiHeight()/2f;
        g.pose().pushMatrix();
        g.pose().translate(anchorX+dx,anchorY+dy);
        g.pose().scale(scale,scale);
        g.pose().translate(-anchorX,-anchorY);
        frost$sidebarPushed=true;
    }
    @Inject(method="displayScoreboardSidebar",at=@At("RETURN"))
    private void frost$afterScoreboard(GuiGraphics g,Objective objective,CallbackInfo ci){
        if(frost$sidebarPushed){g.pose().popMatrix();frost$sidebarPushed=false;}
    }
}
