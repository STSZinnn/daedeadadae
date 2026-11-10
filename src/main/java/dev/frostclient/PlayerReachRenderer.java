package dev.frostclient;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Ground-projected melee range indicator for OTHER players.
 * The ring radius comes from the remote player's held weapon's ATTACK_RANGE
 * component, or their vanilla entity interaction range where no weapon override exists.
 * Green means the local player's bounding box is currently within that player's range;
 * red means it isn't. Server-only range overrides that aren't synced to the client
 * cannot be known reliably.
 */
public final class PlayerReachRenderer {
    private PlayerReachRenderer() {}

    private static final int OUT_OF_RANGE = 0xDBF0504B;
    private static final int IN_RANGE = 0xDC41E77D;

    public static void render(GuiGraphics gui, Minecraft mc, FrostConfig ignoredConfig) {
        if(mc.player == null || mc.level == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized()) return;

        Vec3 cam = camera.position();
        Vector3f forwardVector = camera.rotation().transform(new Vector3f(0,0,-1));
        Vec3 forward = new Vec3(forwardVector.x,forwardVector.y,forwardVector.z);
        int shown = 0;
        for(Player player : mc.level.players()) {
            if(player == mc.player || player.isSpectator() || player.isInvisible()) continue;
            if(player.distanceToSqr(mc.player) > 20 * 20) continue;
            if(!mc.player.hasLineOfSight(player)) continue;
            if(shown++ >= 8) break;

            AttackRange attackRange = player.getMainHandItem().get(DataComponents.ATTACK_RANGE);
            if(attackRange == null) attackRange = AttackRange.defaultFor(player);
            double radius = Math.max(0.0,Math.min(64.0,attackRange.effectiveMaxRange(player)));
            if(radius < .1) continue;
            boolean localPlayerInReach = attackRange.isInRange(player, mc.player.getBoundingBox(), 0.0);
            int color = localPlayerInReach ? IN_RANGE : OUT_OF_RANGE;
            drawRing(gui,mc,cam,forward,player.getX(),player.getY()+0.055,player.getZ(),radius,color);
        }
    }

    private static void drawRing(GuiGraphics gui,Minecraft mc,Vec3 cam,Vec3 forward,
                                 double cx,double cy,double cz,double radius,int color) {
        final int samples = 72;
        Vec3 prev = null;
        for(int i=0;i<=samples;i++) {
            double angle = i * 2 * Math.PI / samples;
            Vec3 current = new Vec3(cx + Math.cos(angle)*radius,cy,cz + Math.sin(angle)*radius);
            if(prev != null) {
                Point a = project(prev,mc,cam,forward,gui.guiWidth(),gui.guiHeight());
                Point b = project(current,mc,cam,forward,gui.guiWidth(),gui.guiHeight());
                if(a != null && b != null && Math.abs(a.x-b.x)<360 && Math.abs(a.y-b.y)<360)
                    line(gui,a.x,a.y,b.x,b.y,color);
            }
            prev=current;
        }
    }

    private static Point project(Vec3 world,Minecraft mc,Vec3 cam,Vec3 forward,int w,int h) {
        Vec3 dir=world.subtract(cam);
        if(dir.dot(forward)<0.12) return null;
        Vec3 screen=mc.gameRenderer.projectPointToScreen(world);
        if(!Double.isFinite(screen.x) || !Double.isFinite(screen.y)) return null;
        int x=(int)Math.round((screen.x+1.0)*w*0.5);
        int y=(int)Math.round((1.0-screen.y)*h*0.5);
        if(x<-32 || x>w+32 || y<-32 || y>h+32)return null;
        return new Point(x,y);
    }

    private static void line(GuiGraphics gui,int ax,int ay,int bx,int by,int color) {
        double dx=bx-ax,dy=by-ay;
        double distance=Math.sqrt(dx*dx+dy*dy);
        if(distance<.5)return;
        gui.pose().pushMatrix();
        gui.pose().translate(ax,ay);
        gui.pose().rotate((float)Math.atan2(dy,dx));
        gui.fill(0,-1,(int)Math.ceil(distance),1,color);
        gui.pose().popMatrix();
    }
    private record Point(int x,int y) {}
}
