package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetherConverterScreen extends Screen {
    private static final Pattern NUMBERS=Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private EditBox coords;
    private boolean netherToOverworld=false;
    private String answer="Paste X Y Z, or X Z";
    public NetherConverterScreen(){super(Component.literal("Frost Nether Converter"));}
    @Override protected void init(){
        int x=width/2-148,y=height/2-74;
        coords=new EditBox(font,x,y+40,296,21,Component.literal("Coordinates"));
        coords.setMaxLength(160);coords.setHint(Component.literal("e.g. 2763 29 8371"));
        addRenderableWidget(coords);
        addRenderableWidget(FrostButton.create("SWITCH DIRECTION",x,y+69,143,20,b->{netherToOverworld=!netherToOverworld;convert();}));
        addRenderableWidget(FrostButton.create("CONVERT",x+153,y+69,143,20,b->convert()));
        addRenderableWidget(FrostButton.create("BACK",x+97,y+130,100,20,b->minecraft.setScreen(new FrostScreen())));
    }
    private void convert(){
        Matcher m=NUMBERS.matcher(coords.getValue());double[] values=new double[3];int count=0;
        while(m.find()&&count<3)try{values[count++]=Double.parseDouble(m.group());}catch(NumberFormatException ignored){}
        if(count<2){answer="Enter X Z or X Y Z";return;}
        double x=values[0],z=count>=3?values[2]:values[1];
        double factor=netherToOverworld?8:0.125;
        answer=String.format(Locale.ROOT,"X: %.2f   Z: %.2f",x*factor,z*factor);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xE8132032);
        int x=width/2-148,y=height/2-74;
        g.fill(x-12,y-14,x+308,y+168,0xEE1B3348);
        g.drawString(font,"NETHER COORDINATE CONVERTER",x+7,y-6,0xFF77E9FF,true);
        g.drawString(font,netherToOverworld?"NETHER  →  OVERWORLD (×8)":"OVERWORLD  →  NETHER (÷8)",x+4,y+16,0xFFE2F9FE,false);
        g.drawString(font,answer,x+5,y+103,0xFF69DDF5,true);
        g.drawString(font,"Paste X Y Z with Ctrl+V; Y is ignored",x+5,y+118,0xFFADC9D8,false);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
