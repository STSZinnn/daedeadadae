package dev.frostclient.mixin;

import dev.frostclient.FrostChatCoordinates;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Link coordinates directly in the displayed chat line, without duplicate messages. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(
        method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0
    )
    private Component frostClient$linkCoordinates(Component message) {
        return FrostChatCoordinates.linkify(message);
    }
}
