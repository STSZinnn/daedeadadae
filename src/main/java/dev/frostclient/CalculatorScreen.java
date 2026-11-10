package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.math.MathContext;

public final class CalculatorScreen extends Screen {
    private String display="0",saved="";
    private String op="";
    private boolean replace=false;
    public CalculatorScreen(){super(Component.literal("Frost Calculator"));}
    @Override protected void init(){
        int x=width/2-109,y=height/2-108;
        String[][] rows={{"7","8","9","÷"},{"4","5","6","×"},{"1","2","3","-"},{"0",".","=","+"}};
        for(int row=0;row<4;row++)for(int col=0;col<4;col++){
            String s=rows[row][col];
            addRenderableWidget(FrostButton.create(s,x+col*55,y+49+row*29,50,24,b->press(s)));
        }
        addRenderableWidget(FrostButton.create("CLEAR",x,y+170,100,20,b->{display="0";saved="";op="";}));
        addRenderableWidget(FrostButton.create("BACK",x+113,y+170,103,20,b->minecraft.setScreen(new FrostScreen())));
    }
    private void press(String text){
        if("+-×÷".contains(text)&&text.length()==1){saved=display;op=text;replace=true;return;}
        if(text.equals("=")){
            if(op.isEmpty())return;
            try{
                BigDecimal a=new BigDecimal(saved),b=new BigDecimal(display);
                BigDecimal result=switch(op){
                    case "+"->a.add(b);case "-"->a.subtract(b);case "×"->a.multiply(b);
                    default->a.divide(b,MathContext.DECIMAL64);};
                display=result.stripTrailingZeros().toPlainString();
            }catch(Exception e){display="Error";}
            op="";replace=true;return;
        }
        if(replace||display.equals("0")||display.equals("Error")){display=text.equals(".")?"0.":text;replace=false;}
        else if(!text.equals(".")||!display.contains("."))display+=text;
        if(display.length()>22)display=display.substring(0,22);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xE8122131);
        int x=width/2-109,y=height/2-108;
        g.fill(x-12,y-12,x+230,y+209,0xF022374B);
        g.drawString(font,"FROST CALCULATOR",x+5,y-5,0xFF67DDFC,true);
        g.fill(x,y+14,x+216,y+43,0xFF0A1727);
        String shown=display;
        if(font.width(shown)>198)shown=shown.substring(Math.max(0,shown.length()-18));
        g.drawString(font,shown,x+210-font.width(shown),y+25,0xFFE8FCFF,true);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
