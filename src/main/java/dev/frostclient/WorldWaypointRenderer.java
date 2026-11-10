package dev.frostclient;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** World-positioned markers rendered at a user-defined, distance-INDEPENDENT HUD size. */
public final class WorldWaypointRenderer {
    private WorldWaypointRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft mc, FrostConfig config) {
        Camera camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized() || mc.player==null || mc.level==null)return;
        Vec3 cameraPos=camera.position();
        Vector3f facing=camera.rotation().transform(new Vector3f(0f,0f,-1f));
        Vec3 forward=new Vec3(facing.x,facing.y,facing.z);
        String dimension=mc.level.dimension().identifier().toString();
        List<FrostConfig.Waypoint> markers=new ArrayList<>();
        for(FrostConfig.Waypoint wp:config.waypoints)
            if(wp!=null && dimension.equals(wp.dimension))markers.add(wp);
        markers.sort(Comparator.comparingDouble(w->cameraPos.distanceToSqr(new Vec3(w.x+.5,w.y+1.5,w.z+.5))));

        int drawn=0;
        // Size is computed ONLY from config; distance never affects the marker dimensions.
        float size=Math.max(.50f,Math.min(2.0f,config.waypointMarkerSize/100f));
        for(FrostConfig.Waypoint wp:markers){
            if(drawn>=16)break;
            Vec3 point=new Vec3(wp.x+.5,wp.y+1.5,wp.z+.5);
            Vec3 offset=point.subtract(cameraPos);
            double distance=offset.length();
            if(distance<.1 || offset.dot(forward)<.15)continue;
            Vec3 projected=mc.gameRenderer.projectPointToScreen(point);
            if(!Double.isFinite(projected.x) || !Double.isFinite(projected.y))continue;

            int sw=graphics.guiWidth(),sh=graphics.guiHeight();
            double tx=(projected.x+1.0)*sw*.5;
            double ty=(1.0-projected.y)*sh*.5;
            String name=wp.name==null||wp.name.isBlank()?"Waypoint":wp.name;
            if(name.length()>20)name=name.substring(0,19)+"…";
            String dist=String.format(Locale.ROOT,"%d m",Math.round(distance));
            int labelWidth=Math.max(mc.font.width(name),mc.font.width(dist))+16;
            int marginX=(int)Math.ceil(size*labelWidth/2)+4;
            int marginTop=(int)Math.ceil(size*42)+2;
            int marginBottom=(int)Math.ceil(size*12)+2;
            if(sw<=2*marginX || sh<=marginTop+marginBottom)continue;
            boolean offscreen=tx<marginX||tx>sw-marginX||ty<marginTop||ty>sh-marginBottom;
            int x=(int)Math.round(Math.max(marginX,Math.min(sw-marginX,tx)));
            int y=(int)Math.round(Math.max(marginTop,Math.min(sh-marginBottom,ty)));
            int rgb=parseColor(wp.color),color=0xFF000000|rgb;
            int opacity=Math.max(135,Math.min(225,225-(int)(distance/80)));

            // GUI pose scaling ensures the whole marker stays 50-200% size at ANY range.
            graphics.pose().pushMatrix();
            graphics.pose().translate(x,y);
            graphics.pose().scale(size,size);
            int lx=-labelWidth/2,ly=-40;
            graphics.fill(lx,ly,lx+labelWidth,ly+26,(opacity<<24)|0x071725);
            graphics.fill(lx,ly,lx+labelWidth,ly+2,color);
            graphics.drawCenteredString(mc.font,name,0,ly+5,0xFFF1FBFF);
            graphics.drawCenteredString(mc.font,dist,0,ly+15,0xFFACDAE9);
            graphics.fill(-2,-6,3,7,0xE0061822);
            graphics.fill(-6,-2,7,3,0xE0061822);
            graphics.fill(-1,-5,2,6,color);
            graphics.fill(-5,-1,6,2,color);
            if(offscreen){
                String arrow=tx<marginX?"◀":tx>sw-marginX?"▶":ty<marginTop?"▲":"▼";
                graphics.drawCenteredString(mc.font,arrow,0,9,color);
            }
            graphics.pose().popMatrix();
            drawn++;
        }
    }
    private static int parseColor(String hex){
        try{if(hex!=null&&hex.matches("#[0-9a-fA-F]{6}"))return Integer.parseInt(hex.substring(1),16);}
        catch(NumberFormatException ignored){}
        return 0x69DDF5;
    }
}
