package dev.frostclient;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Draw a projected outline at the exact adjacent block coordinate where a block will be placed.
 * Pure rendering; never alters the world or inventory. */
public final class PlacementHighlight {
    private PlacementHighlight(){}

    private static int outlineColor(){
        String raw = FrostClient.CONFIG.placementHighlightColor;
        try{
            if(raw == null) return 0xFAF4ED73;
            String hex = raw.trim().replace("#", "");
            if(hex.length() == 6) return (0xFA << 24) | Integer.parseInt(hex, 16);
            if(hex.length() == 8) return (int)Long.parseLong(hex, 16);
        }catch(Exception ignored){}
        return 0xFAF4ED73;
    }

    public static void render(GuiGraphics g,Minecraft mc){
        BlockPos pos=AccuratePlacement.proposedPosition(mc);
        if(pos==null)return;
        Camera camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized())return;
        Vector3f facing=camera.rotation().transform(new Vector3f(0,0,-1));
        Vec3 forward=new Vec3(facing.x,facing.y,facing.z);
        float min=-0.008f,max=1.008f;
        int[][] projected=new int[8][];
        for(int i=0;i<8;i++){
            Vec3 point=new Vec3(pos.getX()+((i&1)!=0?max:min),
                pos.getY()+((i&2)!=0?max:min),pos.getZ()+((i&4)!=0?max:min));
            Vec3 direction=point.subtract(camera.position());
            if(direction.lengthSqr()>64 || direction.dot(forward)<0.15)return;
            Vec3 projectedPoint=mc.gameRenderer.projectPointToScreen(point);
            if(!Double.isFinite(projectedPoint.x) || !Double.isFinite(projectedPoint.y))return;
            int x=(int)Math.round((projectedPoint.x+1)*g.guiWidth()/2d);
            int y=(int)Math.round((1-projectedPoint.y)*g.guiHeight()/2d);
            projected[i]=new int[]{x,y};
        }
        int color = outlineColor();
        int thickness = Math.max(1, FrostClient.CONFIG.placementHighlightThickness);
        for(int i=0;i<8;i++)for(int axis=0;axis<3;axis++){
            int j=i^(1<<axis);
            if(j>i)line(g,projected[i][0],projected[i][1],projected[j][0],projected[j][1],color,thickness);
        }
    }
    private static void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color,int thickness){
        double dx=x2-x1,dy=y2-y1,length=Math.hypot(dx,dy);
        if(length<0.7 || length>Math.max(g.guiWidth(),g.guiHeight())*4d)return;
        g.pose().pushMatrix();
        g.pose().translate(x1,y1);
        g.pose().rotate((float)Math.atan2(dy,dx));
        g.fill(0,0,(int)Math.ceil(length),Math.max(1, thickness),color);
        g.pose().popMatrix();
    }
}
