package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Main panel with module toggles, numeric settings and utility screens. */
public final class FrostScreen extends Screen {
    private static final int ICE=0xFF69DDF5, TEXT=0xFFEAFBFF, MUTED=0xFF82A7C0;
    private final long opened=System.currentTimeMillis();
    private int page=0;
    private int scroll=0;
    private final float[] rowHover = new float[64];
    private long previousFrameNanos = System.nanoTime();
    public FrostScreen(){super(Component.literal("Frost Client"));}
    private void button(String label,int x,int y,int width,int h,Runnable callback) {
        addRenderableWidget(FrostButton.create(label,x,y,width,h,b->callback.run()));
    }
    private void rebuild() { clearWidgets();init(); }
    @Override protected void init() {
        int w=Math.min(600,width-26),h=Math.min(420,height-24),x=(width-w)/2,y=(height-h)/2;
        button("MODS",x+16,y+60,108,22,()->{page=0;rebuild();});
        button("SETTINGS",x+16,y+88,108,22,()->{page=1;rebuild();});
        button("HUD EDITOR",x+16,y+116,108,22,()->minecraft.setScreen(new HudEditorScreen()));
        button("CALCULATOR",x+16,y+144,108,22,()->minecraft.setScreen(new CalculatorScreen()));
        button("NETHER CONV.",x+16,y+172,108,22,()->minecraft.setScreen(new NetherConverterScreen()));
        button("WAYPOINTS",x+16,y+200,108,22,()->minecraft.setScreen(new WaypointScreen()));
        button("CLOSE",x+16,y+h-34,108,22,()->minecraft.setScreen(null));
        if(page==0) {
            int startY=y+68;
            for(int i=0;i<Math.min(rowsPerPage(),FrostConfig.MODULES.length-pageOffset());i++) {
                String key=FrostConfig.MODULES[i+pageOffset()];
                final String f=key;
                button(FrostClient.CONFIG.enabled(key)?"ON":"OFF",x+w-82,startY+i*28,66,20,()->{FrostClient.CONFIG.toggle(f);rebuild();});
            }
            if(pageOffset()>0)button("< PREV",x+140,y+h-37,84,22,()->{scroll=Math.max(0,scroll-rowsPerPage());rebuild();});
            if(pageOffset()+rowsPerPage()<FrostConfig.MODULES.length)button("NEXT >",x+w-98,y+h-37,84,22,()->{scroll+=rowsPerPage();rebuild();});
        } else {
            int startX=x+310;
            numeric(x+150,y+78,startX,"Low Health Hearts",()->String.format("%.1f",FrostClient.CONFIG.lowHealthHearts),
                ()->{FrostClient.CONFIG.lowHealthHearts=Math.max(.5,FrostClient.CONFIG.lowHealthHearts-.5);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.lowHealthHearts=Math.min(20,FrostClient.CONFIG.lowHealthHearts+.5);FrostClient.CONFIG.save();rebuild();});
            numeric(x+150,y+114,startX,"Armor Durability",()->""+FrostClient.CONFIG.armorDurabilityThreshold,
                ()->{FrostClient.CONFIG.armorDurabilityThreshold=Math.max(1,FrostClient.CONFIG.armorDurabilityThreshold-5);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.armorDurabilityThreshold=Math.min(1000,FrostClient.CONFIG.armorDurabilityThreshold+5);FrostClient.CONFIG.save();rebuild();});
            numeric(x+150,y+150,startX,"Potion Expiry (s)",()->""+FrostClient.CONFIG.potionExpirySeconds,
                ()->{FrostClient.CONFIG.potionExpirySeconds=Math.max(1,FrostClient.CONFIG.potionExpirySeconds-1);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.potionExpirySeconds=Math.min(120,FrostClient.CONFIG.potionExpirySeconds+1);FrostClient.CONFIG.save();rebuild();});
            numeric(x+150,y+186,startX,"Potion Tile Size",()->""+FrostClient.CONFIG.potionOverlaySize,
                ()->{FrostClient.CONFIG.potionOverlaySize=Math.max(26,FrostClient.CONFIG.potionOverlaySize-2);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.potionOverlaySize=Math.min(52,FrostClient.CONFIG.potionOverlaySize+2);FrostClient.CONFIG.save();rebuild();});
            numeric(x+150,y+222,startX,"Waypoint Marker Size",()->FrostClient.CONFIG.waypointMarkerSize+"%",
                ()->{FrostClient.CONFIG.waypointMarkerSize=Math.max(50,FrostClient.CONFIG.waypointMarkerSize-10);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.waypointMarkerSize=Math.min(200,FrostClient.CONFIG.waypointMarkerSize+10);FrostClient.CONFIG.save();rebuild();});
            button("Sprint Display: "+(FrostClient.CONFIG.sprintDisplayMode==0?"Text":"Icon"),x+151,y+271,280,22,()->{
                FrostClient.CONFIG.sprintDisplayMode=1-FrostClient.CONFIG.sprintDisplayMode;FrostClient.CONFIG.save();rebuild();});
            button("Placement Color: "+placementColorName(),x+151,y+300,280,22,()->{cyclePlacementColor();rebuild();});
            numeric(x+150,y+326,startX,"Placement Thickness",()->""+FrostClient.CONFIG.placementHighlightThickness,
                ()->{FrostClient.CONFIG.placementHighlightThickness=Math.max(1,FrostClient.CONFIG.placementHighlightThickness-1);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.placementHighlightThickness=Math.min(6,FrostClient.CONFIG.placementHighlightThickness+1);FrostClient.CONFIG.save();rebuild();});
        }
    }
    private int pageOffset(){return scroll;}
    private int rowsPerPage(){return Math.max(3, Math.min(9,(Math.min(390,height-24)-132)/28));}
    private interface ValueLabel {String get();}
    private void numeric(int x,int y,int right,String name,ValueLabel label,Runnable minus,Runnable plus){
        button("-",right,y+13,24,20,minus);button("+",right+116,y+13,24,20,plus);
    }

    private static final String[] PLACEMENT_COLORS={"#F4ED73","#69DDF5","#7DFF9B","#FF7070","#C89CFF","#FFFFFF"};
    private static final String[] PLACEMENT_COLOR_NAMES={"Gold","Ice Blue","Mint","Red","Purple","White"};
    private String placementColorName(){
        String current=FrostClient.CONFIG.placementHighlightColor==null?"#F4ED73":FrostClient.CONFIG.placementHighlightColor.toUpperCase();
        for(int i=0;i<PLACEMENT_COLORS.length;i++) if(PLACEMENT_COLORS[i].equalsIgnoreCase(current)) return PLACEMENT_COLOR_NAMES[i];
        return current;
    }
    private void cyclePlacementColor(){
        String current=FrostClient.CONFIG.placementHighlightColor==null?"#F4ED73":FrostClient.CONFIG.placementHighlightColor;
        int idx=0;
        for(int i=0;i<PLACEMENT_COLORS.length;i++) if(PLACEMENT_COLORS[i].equalsIgnoreCase(current)){ idx=i; break; }
        FrostClient.CONFIG.placementHighlightColor=PLACEMENT_COLORS[(idx+1)%PLACEMENT_COLORS.length];
        FrostClient.CONFIG.save();
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        long now = System.nanoTime();
        float dt = Math.min(0.075f,(now-previousFrameNanos)/1_000_000_000f);
        previousFrameNanos = now;
        float smoothing = 1f-(float)Math.exp(-dt*13f);
        float t=Math.min(1f,(System.currentTimeMillis()-opened)/260f), eased=1f-(1f-t)*(1f-t)*(1f-t);
        // Keep the world visible behind a translucent icy main menu.
        g.fill(0,0,width,height,((int)(45*eased)<<24)|0x030814);
        int w=Math.min(600,width-26),h=Math.min(420,height-24),x=(width-w)/2,y=(height-h)/2;
        float scale=.88f+.12f*eased;
        int pw=Math.round(w*scale),ph=Math.round(h*scale);
        int px=x+(w-pw)/2,py=y+(h-ph)/2;
        int alpha=Math.min(185,Math.round(185*eased));
        RoundedPanels.fill(g,px-3,py-3,pw+6,ph+6,10,0x5444B4E3);
        RoundedPanels.outlined(g,px,py,pw,ph,9,(alpha<<24)|0x10233A,0xBE7CE7FF);
        RoundedPanels.fill(g,px+8,py+3,Math.max(2,pw-16),3,2,0x997CE7FF);
        g.drawString(font,"FROST CLIENT",x+18,y+16,TEXT,true);
        g.drawString(font,"FABRIC 1.21.11  /  v0.6.2",x+18,y+33,MUTED,false);
        g.fillGradient(x+132,y+58,x+133,y+h-14,0xFF5CC4E7,0x55449CDD);
        if(page==0) {
            g.drawString(font,"CLIENT MODULES",x+145,y+42,ICE,false);
            for(int i=0;i<Math.min(rowsPerPage(),FrostConfig.MODULES.length-pageOffset());i++) {
                int ry=y+68+i*28;
                int row=i+pageOffset();
                String name=FrostConfig.MODULES[row];
                boolean hovered=mouseX>=x+139 && mouseX<=x+w-16 && mouseY>=ry-2 && mouseY<=ry+22;
                rowHover[row]+=((hovered?1f:0f)-rowHover[row])*smoothing;
                float hover=rowHover[row];
                int slide=Math.round(3*hover);
                int highlight=(int)(45*hover);
                g.fillGradient(x+139,ry-2,x+w-16,ry+22,0x70334F70+(highlight<<24),0x50304A62+(highlight<<24));
                if(hover>.02f)g.fill(x+139,ry-2,x+141,ry+22,0xFF71DBF3);
                g.drawString(font,name,x+149+slide,ry+6,hover>.35f?0xFFFFFFFF:TEXT,false);
            }
            g.drawString(font,"Page "+(scroll/rowsPerPage()+1)+" / "+((FrostConfig.MODULES.length+rowsPerPage()-1)/rowsPerPage()),x+252,y+h-33,MUTED,false);
        } else {
            g.drawString(font,"CONFIGURATION",x+145,y+42,ICE,false);
            String[] titles={"Low Health Hearts","Armor Durability","Potion Expiry (s)","Potion Tile Size","Waypoint Marker Size","Placement Thickness"};
            String[] vals={""+FrostClient.CONFIG.lowHealthHearts,""+FrostClient.CONFIG.armorDurabilityThreshold,
                ""+FrostClient.CONFIG.potionExpirySeconds,""+FrostClient.CONFIG.potionOverlaySize,
                FrostClient.CONFIG.waypointMarkerSize+"%",""+FrostClient.CONFIG.placementHighlightThickness};
            for(int i=0;i<6;i++) {
                int sy=y+78+i*36;
                boolean hovered=mouseX>=x+145 && mouseX<=x+w-30 && mouseY>=sy-4 && mouseY<=sy+28;
                int level=(int)(hovered?0x18:0x00);
                g.fillGradient(x+145,sy-4,x+w-30,sy+28,0x66304B69+(level<<24),0x3B263B53+(level<<24));
                g.drawString(font,titles[i],x+152,sy+2,TEXT,false);
                g.drawString(font,vals[i],x+368,sy+17,ICE,true);
            }
            g.drawString(font,"Placement Color: "+placementColorName(),x+152,y+355,MUTED,false);
            g.drawString(font,"Settings save to config/frostclient.json",x+152,y+372,MUTED,false);
        }
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==GLFW.GLFW_KEY_RIGHT_SHIFT){minecraft.setScreen(null);return true;}
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen(){return false;}
}
