package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/** Waypoint editor with a true click-and-drag HSV color wheel. */
public final class WaypointScreen extends Screen {
    private static final Identifier WHEEL=Identifier.fromNamespaceAndPath("frostclient","color_wheel");
    private final int initialX,initialY,initialZ;
    private EditBox name, coords, color;
    private int listScroll=0;
    private String info="";
    private boolean pickerOpen=false;
    private boolean draggingWheel=false,draggingValue=false;
    private float hue=.52f,saturation=.55f,brightness=1f;

    public WaypointScreen(){this(currentX(), currentY(), currentZ());}
    public WaypointScreen(int x,int y,int z){super(Component.literal("Frost Waypoints"));initialX=x;initialY=y;initialZ=z;}
    private static int currentX(){var mc=Minecraft.getInstance(); return mc.player==null?0:(int)Math.floor(mc.player.getX());}
    private static int currentY(){var mc=Minecraft.getInstance(); return mc.player==null?64:(int)Math.floor(mc.player.getY());}
    private static int currentZ(){var mc=Minecraft.getInstance(); return mc.player==null?0:(int)Math.floor(mc.player.getZ());}

    @Override protected void init(){
        int x=width/2-165,y=height/2-142;
        name=new EditBox(font,x+14,y+46,305,20,Component.literal("Waypoint name"));
        name.setMaxLength(44);name.setValue("New Waypoint");addRenderableWidget(name);
        coords=new EditBox(font,x+14,y+88,305,20,Component.literal("XYZ"));
        coords.setMaxLength(100);coords.setValue(initialX+" "+initialY+" "+initialZ);addRenderableWidget(coords);
        color=new EditBox(font,x+14,y+130,113,20,Component.literal("Color"));
        color.setMaxLength(7);color.setValue("#69DDF5");addRenderableWidget(color);
        addRenderableWidget(FrostButton.create("COLOR WHEEL",x+135,y+130,94,20,b->openPicker()));
        addRenderableWidget(FrostButton.create("USE POS",x+233,y+130,86,20,b->coords.setValue(currentX()+" "+currentY()+" "+currentZ())));
        addRenderableWidget(FrostButton.create("SAVE WAYPOINT",x+14,y+158,305,22,b->saveWaypoint()));
        addRenderableWidget(FrostButton.create("PREV",x+14,y+274,84,20,b->{listScroll=Math.max(0,listScroll-4);refresh();}));
        addRenderableWidget(FrostButton.create("NEXT",x+104,y+274,84,20,b->{listScroll+=4;refresh();}));
        addRenderableWidget(FrostButton.create("BACK",x+222,y+274,97,20,b->minecraft.setScreen(new FrostScreen())));
        int max=Math.min(4,Math.max(0,FrostClient.CONFIG.waypoints.size()-listScroll));
        for(int i=0;i<max;i++){
            int index=listScroll+i;
            addRenderableWidget(FrostButton.create("X",x+300,y+201+i*18,19,17,b->{FrostClient.CONFIG.waypoints.remove(index);FrostClient.CONFIG.save();refresh();}));
        }
    }

    private void refresh(){clearWidgets();init();}
    private void saveWaypoint(){
        try{
            String[] pieces=coords.getValue().trim().split("[\\s,]+");
            if(pieces.length<3){info="Provide three coordinates (X Y Z)";return;}
            int x=(int)Math.floor(Double.parseDouble(pieces[0])),y=(int)Math.floor(Double.parseDouble(pieces[1])),z=(int)Math.floor(Double.parseDouble(pieces[2]));
            String hex=color.getValue().trim();if(!hex.matches("#[0-9a-fA-F]{6}")){info="Color must be #RRGGBB";return;}
            FrostClient.addWaypoint(name.getValue().isBlank()?"Waypoint":name.getValue(),x,y,z,hex);
            info="Saved waypoint";listScroll=Math.max(0,FrostClient.CONFIG.waypoints.size()-4);
            refresh();
        }catch(Exception ignored){info="Invalid coordinates";}
    }

    private void openPicker(){
        try{
            String hex=color.getValue().replace("#","");
            int rgb=Integer.parseInt(hex,16);
            float[] hsb=Color.RGBtoHSB((rgb>>16)&255,(rgb>>8)&255,rgb&255,null);
            hue=hsb[0];saturation=hsb[1];brightness=hsb[2];
        }catch(Exception ignored){hue=.52f;saturation=.55f;brightness=1f;}
        pickerOpen=true;
    }
    private void setHex(){
        int rgb=Color.HSBtoRGB(hue,saturation,brightness)&0xFFFFFF;
        color.setValue(String.format(java.util.Locale.ROOT,"#%06X",rgb));
    }
    private int popupX(){return width/2-125;}
    private int popupY(){return height/2-114;}

    private boolean chooseWheel(double px,double py) {
        double dx=px-(popupX()+77),dy=py-(popupY()+102);
        double length=Math.hypot(dx,dy);
        if(length>59)return false;
        hue=(float)((Math.atan2(-dy,dx)/(2*Math.PI)+1)%1);
        saturation=(float)Math.max(0,Math.min(1,length/56.0));
        setHex();return true;
    }
    private boolean chooseValue(double mx,double my) {
        int barX=popupX()+157,barY=popupY()+45;
        if(mx<barX-7||mx>barX+30||my<barY-5||my>barY+121)return false;
        brightness=(float)Math.max(0,Math.min(1,1-(my-barY)/115.0));
        setHex();return true;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(!pickerOpen)return super.mouseClicked(event,doubleClick);
        if(event.button()!=GLFW.GLFW_MOUSE_BUTTON_LEFT)return true;
        double mx=event.x(),my=event.y();
        int x=popupX(),y=popupY();
        if(mx>=x+63&&mx<=x+187&&my>=y+185&&my<=y+210){pickerOpen=false;return true;}
        if(chooseWheel(mx,my)){draggingWheel=true;return true;}
        if(chooseValue(mx,my)){draggingValue=true;return true;}
        if(mx<x||mx>x+250||my<y||my>y+228)pickerOpen=false;
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(pickerOpen){
            if(draggingWheel){chooseWheel(event.x(),event.y());return true;}
            if(draggingValue){chooseValue(event.x(),event.y());return true;}
            return true;
        }
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(pickerOpen){draggingWheel=false;draggingValue=false;return true;}
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(pickerOpen){if(event.key()==GLFW.GLFW_KEY_ESCAPE){pickerOpen=false;return true;}return true;}
        return super.keyPressed(event);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderTransparentBackground(g);
        g.fill(0,0,width,height,0xB0091322);
        int x=width/2-165,y=height/2-142;
        g.fillGradient(x-12,y-14,x+332,y+304,0xD4162B43,0xC0091425);
        g.fillGradient(x-12,y-14,x+332,y-10,0xFF7CE7FF,0xFF428CDE);
        g.drawString(font,"WAYPOINT MANAGER",x+14,y,0xFF79DEFA,true);
        g.drawString(font,"Click chat coords to prefill a waypoint",x+14,y+14,0xFFB9DDE8,false);
        g.drawString(font,"Name",x+14,y+34,0xFFEAFBFF,false);
        g.drawString(font,"X Y Z (paste allowed)",x+14,y+76,0xFFEAFBFF,false);
        g.drawString(font,"Color",x+14,y+118,0xFFEAFBFF,false);
        g.drawString(font,"SAVED WAYPOINTS",x+14,y+186,0xFF8DE5F7,true);
        for(int i=0;i<4 && listScroll+i<FrostClient.CONFIG.waypoints.size();i++){
            var wp=FrostClient.CONFIG.waypoints.get(listScroll+i);
            int rowY=y+202+i*18;
            g.fillGradient(x+12,rowY-2,x+298,rowY+13,0x65304967,0x40233549);
            String line=wp.name+"  ("+wp.x+", "+wp.y+", "+wp.z+")";
            if(font.width(line)>252)line=line.substring(0,Math.min(30,line.length()))+"...";
            g.drawString(font,line,x+16,rowY,0xFFD0E5F1,false);
        }
        g.drawString(font,info,x+14,y+256,0xFF79DEFA,false);
        super.render(g,mx,my,partial);
        if(pickerOpen)renderPicker(g);
    }

    private void renderPicker(GuiGraphics g){
        int x=popupX(),y=popupY();
        g.fill(0,0,width,height,0xCE06111F);
        g.fillGradient(x,y,x+250,y+228,0xFF1B354B,0xFF081626);
        g.fill(x,y,x+250,y+3,0xFF79DCF6);
        g.drawString(font,"PICK WAYPOINT COLOR",x+14,y+13,0xFFEAFBFF,true);
        g.blitSprite(RenderPipelines.GUI_TEXTURED,WHEEL,x+19,y+44,116,116);
        // Circle marker: white square with dark outline for all hues.
        double a=hue*2*Math.PI;
        int px=x+77+(int)Math.round(Math.cos(a)*saturation*56);
        int py=y+102-(int)Math.round(Math.sin(a)*saturation*56);
        g.fill(px-4,py-4,px+5,py+5,0xFF08131B);
        g.fill(px-3,py-3,px+4,py+4,0xFFFFFFFF);
        g.fill(px-1,py-1,px+2,py+2,0xFF152539);
        for(int i=0;i<116;i++){
            int rgb=Color.HSBtoRGB(hue,saturation,(float)(1-i/115.0));
            g.fill(x+157,y+45+i,x+181,y+46+i,0xFF000000|(rgb&0xFFFFFF));
        }
        int barY=y+45+(int)Math.round((1-brightness)*115);
        g.fill(x+152,barY-2,x+186,barY+2,0xFFFFFFFF);
        int selected=Color.HSBtoRGB(hue,saturation,brightness);
        g.fill(x+197,y+46,x+232,y+80,0xFF000000|selected);
        g.drawString(font,"BRIGHT",x+154,y+170,0xFFB8DCE7,false);
        g.drawCenteredString(font,color.getValue(),x+125,y+172,0xFFEDF9FF);
        g.fillGradient(x+63,y+185,x+187,y+210,0xFF285876,0xFF19415E);
        g.drawCenteredString(font,"DONE",x+125,y+193,0xFFF1FAFF);
        g.drawCenteredString(font,"Click / drag to pick color",x+125,y+215,0xFFB5D2E3);
    }
    @Override public boolean isPauseScreen(){return false;}
}
