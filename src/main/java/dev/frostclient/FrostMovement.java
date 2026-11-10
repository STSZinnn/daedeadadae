package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Actual horizontal distance moved in the most recent client tick, not an average of past speeds. */
public final class FrostMovement {
    private Vec3 lastPos;
    private String levelId = "";
    private double blocksPerSecond = 0;
    private boolean wasAttacking = false, wasUsing = false;

    public double blocksPerSecond() { return blocksPerSecond; }

    public void tick(Minecraft mc) {
        if(mc.player == null || mc.level == null) {
            lastPos = null; blocksPerSecond = 0; wasAttacking = false; wasUsing = false;
            return;
        }
        if(mc.screen == null) {
            boolean attack = mc.options.keyAttack.isDown(), use = mc.options.keyUse.isDown();
            if(attack && !wasAttacking) FrostClient.CLICKS.record(true);
            if(use && !wasUsing) FrostClient.CLICKS.record(false);
            wasAttacking = attack; wasUsing = use;
        }
        String nowLevel = mc.level.dimension().identifier().toString();
        Vec3 pos = mc.player.position();
        if(lastPos != null && nowLevel.equals(levelId)) {
            double moved = Math.hypot(pos.x-lastPos.x, pos.z-lastPos.z);
            // Do not report teleport / respawn jumps as movement speed.
            blocksPerSecond = moved > 12.0 ? 0.0 : moved * 20.0;
        } else {
            blocksPerSecond = 0;
        }
        lastPos = pos;
        levelId = nowLevel;
    }
}
