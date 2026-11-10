package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;

/** Small rounded rectangles using vanilla 2D fill calls; no textures or shaders required. */
public final class RoundedPanels {
    private RoundedPanels(){}
    public static void fill(GuiGraphics g,int x,int y,int w,int h,int radius,int rgba){
        if(w<=0||h<=0)return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        if(r==0){g.fill(x,y,x+w,y+h,rgba);return;}
        g.fill(x,y+r,x+w,y+h-r,rgba);
        for(int i=0;i<r;i++){
            // Pixel-accurate quarter-circle cutout. No opaque square corners.
            double dy=r-i-0.5;
            int cut=(int)Math.ceil(r-Math.sqrt(Math.max(0,r*r-dy*dy)));
            g.fill(x+cut,y+i,x+w-cut,y+i+1,rgba);
            g.fill(x+cut,y+h-i-1,x+w-cut,y+h-i,rgba);
        }
    }
    public static void outlined(GuiGraphics g,int x,int y,int w,int h,int radius,int bg,int border){
        fill(g,x,y,w,h,radius,border);
        fill(g,x+1,y+1,w-2,h-2,Math.max(0,radius-1),bg);
    }
}
