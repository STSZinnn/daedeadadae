package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Click any HUD tile to select it, drag to reposition it, and resize that selection. */
public final class HudEditorScreen extends Screen {
    private static final List<String> EDITABLE=HudLayout.EDITABLE;
    private static final int ICE=0xFF79E9FF;
    private String selected="Server Scoreboard";
    private String dragging=null;
    private int grabX,grabY;
    public HudEditorScreen(){super(Component.literal("Frost Client HUD Editor"));}
    private void select(int offset){
        int index=EDITABLE.indexOf(selected);
        selected=EDITABLE.get(Math.floorMod(index+offset,EDITABLE.size()));
    }
    private void scaleBy(int amount){FrostClient.CONFIG.setHudScale(selected,FrostClient.CONFIG.hudScale(selected)+amount);}
    private void reset(){
        FrostClient.CONFIG.positions.remove(selected);
        FrostClient.CONFIG.hudScales.remove(selected);
        FrostClient.CONFIG.save();
    }
    @Override protected void init(){
        int x=Math.max(7,width-207);
        addRenderableWidget(FrostButton.create("<",x+7,38,30,19,b->select(-1)));
        addRenderableWidget(FrostButton.create(">",x+165,38,30,19,b->select(1)));
        addRenderableWidget(FrostButton.create("-",x+7,83,36,19,b->scaleBy(-10)));
        addRenderableWidget(FrostButton.create("+",x+159,83,36,19,b->scaleBy(10)));
        addRenderableWidget(FrostButton.create("RESET",x+7,110,86,19,b->reset()));
        addRenderableWidget(FrostButton.create("DONE",x+105,110,90,19,b->{FrostClient.CONFIG.save();minecraft.setScreen(new FrostScreen());}));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x65050E19);
        g.fill(0,0,width,27,0xB9091A2A);
        g.drawString(font,"FROST  /  HUD EDITOR",10,9,ICE,true);
        if(width>430)g.drawString(font,"Click a tile, drag to move, +/- to scale",146,9,0xFFC3E2EC,false);
        for(String key:EDITABLE){
            var r=HudLayout.rect(key,width,height);
            float factor=HudLayout.scale(key);
            int w=Math.max(26,Math.round(r.width()*factor));
            int h=Math.max(15,Math.round(r.height()*factor));
            int border=selected.equals(key)?0xFF8CF1FF:0xA671B9C7;
            int fill=selected.equals(key)?0x7A1F5678:0x56112337;
            RoundedPanels.outlined(g,r.x(),r.y(),w,h,5,fill,border);
            g.enableScissor(r.x()+2,r.y()+2,r.x()+w-2,r.y()+h-2);
            g.drawString(font,key,r.x()+6,r.y()+4,0xFFE5F8FF,true);
            if(h>30)g.drawString(font,"Drag to reposition",r.x()+6,r.y()+19,0xFF91C4D5,false);
            g.disableScissor();
        }
        int px=Math.max(7,width-207);
        RoundedPanels.outlined(g,px,30,202,105,7,0xCC0C2235,0xFF4C97B1);
        g.drawCenteredString(font,"SELECT HUD ELEMENT",px+101,35,ICE);
        g.drawCenteredString(font,selected,px+101,62,0xFFFFFFFF);
        g.drawCenteredString(font,"Size: "+FrostClient.CONFIG.hudScale(selected)+"%",px+101,88,0xFFBCEBFA);
        g.drawString(font,"Saved to frostclient.json",10,height-13,0xFFA8C7D4,false);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        // Preserve vanilla button behavior before handling tile dragging.
        if(super.mouseClicked(e,doubleClick))return true;
        if(e.button()==0){
            for(int i=EDITABLE.size()-1;i>=0;i--){
                String key=EDITABLE.get(i);
                HudLayout.Rect r=HudLayout.rect(key,width,height);
                int ww=Math.max(26,Math.round(r.width()*HudLayout.scale(key)));
                int hh=Math.max(15,Math.round(r.height()*HudLayout.scale(key)));
                if(e.x()>=r.x()&&e.x()<r.x()+ww&&e.y()>=r.y()&&e.y()<r.y()+hh){
                    selected=key;dragging=key;grabX=(int)e.x()-r.x();grabY=(int)e.y()-r.y();return true;
                }
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging!=null){
            HudLayout.Rect r=HudLayout.rect(dragging,width,height);
            int w=Math.max(26,Math.round(r.width()*HudLayout.scale(dragging)));
            int h=Math.max(15,Math.round(r.height()*HudLayout.scale(dragging)));
            int x=Math.max(0,Math.min(width-w,(int)e.x()-grabX));
            int y=Math.max(28,Math.min(height-h,(int)e.y()-grabY));
            FrostClient.CONFIG.positions.put(dragging,new FrostConfig.HudPosition(x,y));
            return true;
        }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(dragging!=null){dragging=null;FrostClient.CONFIG.save();return true;}
        return super.mouseReleased(e);
    }
    @Override public void onClose(){FrostClient.CONFIG.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
