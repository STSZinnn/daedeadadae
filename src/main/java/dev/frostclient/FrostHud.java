package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FrostHud {
    private static final int ICE=0xFF6ADCF3, TEXT=0xFFF3FBFF, PANEL=0xC80C1C2D, DANGER=0xFFFF536A;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static long lastSoundAt=0L;
    private FrostHud() {}
    public static void render(GuiGraphics g,DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.options.hideGui)return;
        FrostConfig c=FrostClient.CONFIG;
        if(c.enabled("CPS Counter")) panel(g,"CPS Counter","CPS  "+FrostClient.CLICKS.leftCps()+" | "+FrostClient.CLICKS.rightCps());
        if(c.enabled("Ping Counter")) {
            int ping=-1;
            if(mc.getConnection()!=null) {
                var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if(info!=null)ping=info.getLatency();
            }
            panel(g,"Ping Counter","PING  "+(ping<0?"--":ping+" ms"));
        }
        if(c.enabled("Blocks Per Second")) panel(g,"Blocks Per Second",String.format(Locale.ROOT,"SPEED  %.2f blocks/s",FrostClient.MOVEMENT.blocksPerSecond()));
        if(c.enabled("Reach Display")) {
            String reach="REACH  --";
            if(mc.hitResult instanceof EntityHitResult result && result.getEntity() instanceof LivingEntity)
                reach=String.format(Locale.ROOT,"REACH  %.2f blocks",Math.sqrt(mc.player.getEyePosition().distanceToSqr(result.getLocation())));
            panel(g,"Reach Display",reach);
        }
        if(c.enabled("Sprint Status") && mc.player.isSprinting()) {
            var r=HudLayout.rect("Sprint Status",g.guiWidth(),g.guiHeight());
            HudLayout.draw(g,"Sprint Status",g.guiWidth(),g.guiHeight(),()->{
                if(c.sprintDisplayMode==1)
                    g.blitSprite(RenderPipelines.GUI_TEXTURED,Gui.getMobEffectSprite(MobEffects.SPEED),r.x(),r.y(),18,18);
                else panelAt(g,r.x(),r.y(),"Sprint Toggled");
            });
        }
        if(c.enabled("Low Health Warning")&&mc.player.getHealth()<=c.lowHealthHearts*2) {
            var r=HudLayout.rect("Low Health Warning",g.guiWidth(),g.guiHeight());
            HudLayout.draw(g,"Low Health Warning",g.guiWidth(),g.guiHeight(),()->
                g.drawCenteredString(mc.font,"!",r.x()+r.width()/2,r.y(),DANGER));
        }
        if(c.enabled("Low Armor Warning"))
            HudLayout.draw(g,"Low Armor Warning",g.guiWidth(),g.guiHeight(),()->renderArmorWarning(g, mc, c));
        if(c.enabled("Shield Status Indicators"))
            HudLayout.draw(g,"Shield Status Indicators",g.guiWidth(),g.guiHeight(),()->renderShieldIndicator(g, mc));
        if(mc.hitResult instanceof EntityHitResult result && result.getEntity() instanceof LivingEntity target && target!=mc.player) {
            if(c.enabled("Target HUD"))
                HudLayout.draw(g,"Target HUD",g.guiWidth(),g.guiHeight(),()->target(g,target,c.enabled("Target Potion Effects")));
            if(c.enabled("Hitbox Viewer")) {
                int cx=g.guiWidth()/2,cy=g.guiHeight()/2;
                g.fill(cx-20,cy-20,cx-18,cy-7,0xCC72DFF2);
                g.fill(cx-20,cy-20,cx-7,cy-18,0xCC72DFF2);
                g.fill(cx+18,cy+7,cx+20,cy+20,0xCC72DFF2);
                g.fill(cx+7,cy+18,cx+20,cy+20,0xCC72DFF2);
            }
        }
        if(c.enabled("Health Indicators"))
            HudLayout.draw(g,"Health Indicators",g.guiWidth(),g.guiHeight(),()->nearby(g,mc));
        if(c.enabled("Potion Effects Overlay"))
            HudLayout.draw(g,"Potion Effects Overlay",g.guiWidth(),g.guiHeight(),()->effects(g,mc,c));
        if(c.enabled("Potion Expiry Warning"))
            HudLayout.draw(g,"Potion Expiry Warning",g.guiWidth(),g.guiHeight(),()->expiry(g,mc,c));
        if(c.enabled("Waypoints HUD"))
            HudLayout.draw(g,"Waypoints HUD",g.guiWidth(),g.guiHeight(),()->waypoints(g,mc,c));
        if(c.enabled("World Waypoint Markers")) WorldWaypointRenderer.render(g,mc,c);
        if(c.enabled("Reach Circle")) PlayerReachRenderer.render(g, mc, c);
        // The optional placement assist is now visual only while right-click is held.
        // It never injects use-item calls or bypasses vanilla placement delay.
        if(c.enabled("Placement Highlight") ||
                (c.enabled("Accurate Block Placement") && mc.options.keyUse.isDown()))
            PlacementHighlight.render(g,mc);
    }
    private static void renderArmorWarning(GuiGraphics g, Minecraft mc, FrostConfig c){
        // Dedicated row above and to the right of the crosshair; no labels to collide
        // with potion warnings below the crosshair. Draw the ACTUAL damaged armor stack.
        int shown=0;
        var r=HudLayout.rect("Low Armor Warning",g.guiWidth(),g.guiHeight());
        for(EquipmentSlot slot:ARMOR_SLOTS) {
            ItemStack item=mc.player.getItemBySlot(slot);
            if(item.isEmpty() || !item.isDamageableItem()) continue;
            int durability=item.getMaxDamage()-item.getDamageValue();
            if(durability > c.armorDurabilityThreshold) continue;
            int x=r.x()+shown*27, y=r.y();
            g.renderFakeItem(item,x,y);
            g.drawString(mc.font,"!",x+16,y-3,DANGER,true);
            shown++;
        }
        if(shown>0 && c.enabled("Armor Warning Sound") && System.currentTimeMillis()-lastSoundAt>12000) {
            lastSoundAt=System.currentTimeMillis();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.5f));
        }
    }
    private static void renderShieldIndicator(GuiGraphics g, Minecraft mc){
        boolean hasShield=mc.player.getOffhandItem().is(Items.SHIELD)||mc.player.getMainHandItem().is(Items.SHIELD);
        if(hasShield) {
            boolean stunned=mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
            var r=HudLayout.rect("Shield Status Indicators",g.guiWidth(),g.guiHeight());
            int sx=r.x(), sy=r.y();
            g.fill(sx,sy,sx+11,sy+11,stunned?0xAAFF4A57:0xAA46DB9B);
            g.drawString(mc.font,stunned?"X":"S",sx+2,sy+1,0xFFFFFFFF,true);
        }
    }
    private static void panel(GuiGraphics g,String key,String text){
        var r=HudLayout.rect(key,g.guiWidth(),g.guiHeight());
        HudLayout.draw(g,key,g.guiWidth(),g.guiHeight(),()->panelAt(g,r.x(),r.y(),text));
    }
    static void panelAt(GuiGraphics g,int x,int y,String text) {
        int w=Minecraft.getInstance().font.width(text)+14;
        g.fillGradient(x,y,x+w,y+15,PANEL,0xAA12243B);g.fill(x,y,x+2,y+15,ICE);
        g.drawString(Minecraft.getInstance().font,text,x+6,y+3,TEXT,true);
    }
    private static void target(GuiGraphics g,LivingEntity e,boolean showEffects) {
        Minecraft mc=Minecraft.getInstance();int w=204;
        var rect=HudLayout.rect("Target HUD",g.guiWidth(),g.guiHeight());
        int x=rect.x(),y=rect.y();
        g.fillGradient(x,y,x+w,y+48,PANEL,0xAA10233D);g.fillGradient(x,y,x+w,y+2,ICE,0xFF3A8DDF);
        g.drawString(mc.font,e.getName(),x+9,y+7,TEXT,true);
        float hp=Math.max(0,e.getHealth()), max=Math.max(1,e.getMaxHealth());
        g.fill(x+9,y+22,x+w-9,y+30,0xFF355068);
        g.fill(x+9,y+22,x+9+Math.round((w-18)*Math.min(1f,hp/max)),y+30,ICE);
        g.drawString(mc.font,String.format(Locale.ROOT,"%.1f / %.1f HP",hp,max),x+9,y+35,0xFFC5D9EA,false);
        if(showEffects&&!e.getActiveEffects().isEmpty()) {
            int shown=0;
            for(MobEffectInstance effect:e.getActiveEffects()){
                if(!effect.showIcon() || shown>=6)continue;
                int px=x+9+shown*31,py=y+51;
                g.fill(px-2,py-2,px+29,py+20,0xCC102638);
                vanillaEffectIcon(g,effect,px,py,18);
                String level=roman(effect.getAmplifier());
                g.drawString(mc.font,level,px+17,py+2,ICE,true);
                shown++;
            }
        }
    }
    private static void nearby(GuiGraphics g,Minecraft mc) {
        List<Player> players=new ArrayList<>();
        for(Player p:mc.level.players()) if(!p.equals(mc.player)&&p.distanceToSqr(mc.player)<=20*20) players.add(p);
        players.sort(Comparator.comparingDouble(p->p.distanceToSqr(mc.player)));
        var r=HudLayout.rect("Health Indicators",g.guiWidth(),g.guiHeight());
        int x=r.x(),y=r.y()+17;
        if(!players.isEmpty()) panelAt(g,x,y-17,"NEARBY HEALTH");
        for(int i=0;i<Math.min(3,players.size());i++) {
            Player p=players.get(i);
            panelAt(g,x,y+i*18,p.getName().getString()+"  "+String.format(Locale.ROOT,"%.1f",p.getHealth()/2)+" ♥");
        }
    }
    private static String roman(int n) {
        int v=n+1; if(v<=0)return "I";
        StringBuilder s=new StringBuilder();
        int[] nums={10,9,5,4,1};String[] forms={"X","IX","V","IV","I"};
        for(int i=0;i<nums.length;i++)while(v>=nums[i]){v-=nums[i];s.append(forms[i]);}
        return s.toString();
    }
    private static String duration(int ticks) {
        int seconds=Math.max(0,ticks)/20;
        return String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
    }
    // Vanilla 1.21.11 reads these very same mob-effect sprites in Gui.renderEffects.
    private static void vanillaEffectIcon(GuiGraphics g, MobEffectInstance effect, int x, int y, int size) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()), x, y, size, size);
    }
    private static void effects(GuiGraphics g,Minecraft mc,FrostConfig c) {
        var p=HudLayout.rect("Potion Effects Overlay",g.guiWidth(),g.guiHeight());
        List<MobEffectInstance> active=new ArrayList<>(mc.player.getActiveEffects());
        active.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        int size=c.potionOverlaySize;
        int n=0;
        for(MobEffectInstance e:active) {
            if(!e.showIcon())continue;
            if(n>=9)break;
            int x=p.x(),y=p.y()+n*(size+3);
            g.fillGradient(x,y,x+size,y+size,0xDC192638,0xCC091621);
            g.fill(x,y,x+size,y+2,0xFF75DBF1);
            int sprite=Math.min(18,size-10);
            vanillaEffectIcon(g,e,x+(size-sprite)/2,y+6,sprite);
            String numeral=roman(e.getAmplifier());
            g.drawString(mc.font,numeral,x+size-mc.font.width(numeral)-1,y+1,TEXT,true);
            g.drawCenteredString(mc.font,e.getDuration()<0?"∞":duration(e.getDuration()),x+size/2,y+size-9,0xFFB5E4F4);
            n++;
        }
    }
    private static void expiry(GuiGraphics g,Minecraft mc,FrostConfig c) {
        // Pure vanilla-effect icon and a small warning !, without timer or tile.
        // This row is BELOW the crosshair; armor warnings are ABOVE it.
        int n=0;
        var r=HudLayout.rect("Potion Expiry Warning",g.guiWidth(),g.guiHeight());
        List<MobEffectInstance> active=new ArrayList<>(mc.player.getActiveEffects());
        active.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        for(MobEffectInstance e:active) {
            if(!e.showIcon() || e.getDuration()<0 || !e.endsWithin(c.potionExpirySeconds*20)) continue;
            int x=r.x()+n*25,y=r.y();
            vanillaEffectIcon(g,e,x,y,13);
            g.drawString(mc.font,"!",x+14,y-2,DANGER,true);
            if(++n>=4)break;
        }
    }
    private static void waypoints(GuiGraphics g,Minecraft mc,FrostConfig c) {
        String current=mc.level.dimension().identifier().toString();int displayed=0;
        var p=HudLayout.rect("Waypoints HUD",g.guiWidth(),g.guiHeight());
        for(FrostConfig.Waypoint w:c.waypoints) {
            if(!current.equals(w.dimension))continue;
            double dx=mc.player.getX()-w.x,dz=mc.player.getZ()-w.z;
            int dist=(int)Math.sqrt(dx*dx+dz*dz);
            panelAt(g,p.x(),p.y()+displayed*17,"   "+w.name+" "+dist+"m  "+w.x+" "+w.y+" "+w.z);
            int color=ICE;try{color=0xFF000000|Integer.parseInt(w.color.substring(1),16);}catch(Exception ignored){}
            g.fill(p.x()+5,p.y()+displayed*17+5,p.x()+11,p.y()+displayed*17+11,color);
            if(++displayed>=5)break;
        }
    }
}
