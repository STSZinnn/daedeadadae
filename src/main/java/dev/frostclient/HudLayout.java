package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

/** One source of truth for HUD widget preview, dragging and live scaling. */
public final class HudLayout {
    public static final List<String> EDITABLE = List.of(
        "Server Scoreboard", "CPS Counter", "Ping Counter", "Blocks Per Second",
        "Reach Display", "Sprint Status", "Potion Effects Overlay", "Waypoints HUD",
        "Target HUD", "Health Indicators", "Potion Expiry Warning", "Low Armor Warning",
        "Low Health Warning", "Shield Status Indicators"
    );
    public record Rect(int x,int y,int width,int height) {}
    private HudLayout(){}

    public static Rect defaults(String key,int w,int h){
        return switch(key){
            case "Server Scoreboard" -> new Rect(w-165,h/2-55,160,110);
            case "CPS Counter" -> new Rect(8,8,140,16);
            case "Ping Counter" -> new Rect(8,26,140,16);
            case "Blocks Per Second" -> new Rect(8,44,158,16);
            case "Reach Display" -> new Rect(8,62,140,16);
            case "Sprint Status" -> new Rect(w/2-52,h-50,104,19);
            case "Potion Effects Overlay" -> new Rect(8,87,32,112);
            case "Waypoints HUD" -> new Rect(8,195,168,85);
            case "Target HUD" -> new Rect(w/2-102,9,204,76);
            case "Health Indicators" -> new Rect(w-145,h-93,145,77);
            case "Potion Expiry Warning" -> new Rect(w/2+23,h/2+13,100,18);
            case "Low Armor Warning" -> new Rect(w/2+22,h/2-42,108,21);
            case "Low Health Warning" -> new Rect(w/2-5,h/2-29,12,14);
            case "Shield Status Indicators" -> new Rect(w/2+15,h/2-8,12,12);
            default -> new Rect(8,8,100,18);
        };
    }

    public static Rect rect(String key,int w,int h) {
        Rect d=defaults(key,w,h);
        FrostConfig.HudPosition saved=FrostClient.CONFIG.positions.get(key);
        if(saved==null)return d;
        if(key.equals("Sprint Status") && saved.x==0 && saved.y==-50)return d;
        return new Rect(saved.x, saved.y<0?h+saved.y:saved.y,d.width,d.height);
    }

    public static void savePosition(String key,int x,int y) {
        FrostClient.CONFIG.positions.put(key,new FrostConfig.HudPosition(x,y));
        FrostClient.CONFIG.save();
    }

    public static float scale(String key){return FrostClient.CONFIG.hudScale(key)/100.0f;}

    /** Scales content about the top-left corner selected in the editor. */
    public static void draw(GuiGraphics g,String key,int w,int h,Runnable contents){
        Rect rect=rect(key,w,h);
        float size=scale(key);
        g.pose().pushMatrix();
        g.pose().translate(rect.x,rect.y);
        g.pose().scale(size,size);
        g.pose().translate(-rect.x,-rect.y);
        try {contents.run();} finally {g.pose().popMatrix();}
    }
}
