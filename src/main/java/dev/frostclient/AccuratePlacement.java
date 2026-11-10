package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Vanilla-safe placement preview support.
 *
 * Important: This class deliberately NEVER performs an item interaction or
 * synthesizes right-clicks. Minecraft's built-in use-key handler owns all
 * placement, including cooldowns, target validation, and normal reach.
 */
public final class AccuratePlacement {
    private AccuratePlacement() {}

    /**
     * Returns a preview target only for a real block looked at by vanilla.
     * Never raycasts farther than Minecraft itself to manufacture a target.
     * This is a preview, not a guarantee of server-side build permission.
     */
    public static BlockPos proposedPosition(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null)
            return null;

        // Minecraft's actual interaction target already honors vanilla reach.
        if (!(mc.hitResult instanceof BlockHitResult hit)
                || mc.hitResult.getType() != HitResult.Type.BLOCK)
            return null;

        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem))
            return null;

        // Use the same click context as ordinary block placement. This handles
        // placing alongside solid blocks and replacing vegetation correctly.
        BlockPlaceContext context = new BlockPlaceContext(
                mc.player, InteractionHand.MAIN_HAND, held, hit);
        if (!context.canPlace())
            return null;

        BlockPos target = context.getClickedPos();
        if (!mc.level.getWorldBorder().isWithinBounds(target))
            return null;
        if (!mc.level.mayInteract(mc.player, hit.getBlockPos())
                || !mc.level.mayInteract(mc.player, target))
            return null;
        if (!mc.level.getBlockState(target).canBeReplaced(context))
            return null;

        // Visual only. Vanilla and the server decide whether placement succeeds.
        return target.immutable();
    }
}
