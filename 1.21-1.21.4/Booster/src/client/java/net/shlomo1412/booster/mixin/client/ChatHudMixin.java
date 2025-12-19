package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.MentionedInChatAlertModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept chat messages for mention detection.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    
    /**
     * Intercepts chat messages to check for player name mentions.
     */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", 
            at = @At("HEAD"))
    private void booster$onChatMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) return;
        
        String playerName = player.getName().getString();
        
        // Check for mentions
        MentionedInChatAlertModule mentionModule = ModuleManager.getInstance().getModule(MentionedInChatAlertModule.class);
        if (mentionModule != null) {
            mentionModule.checkForMention(message, playerName);
        }
    }
}
