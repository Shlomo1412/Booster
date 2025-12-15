package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.SearchMessagesModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to add search highlighting to the chat HUD.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    
    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;
    
    @Shadow
    @Final
    private List<ChatHudLine> messages;
    
    @Shadow
    private int scrolledLines;
    
    /**
     * Inject after rendering each chat line to add highlight overlay.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRenderTail(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) return;
        
        SearchMessagesModule searchModule = ModuleManager.getInstance().getModule(SearchMessagesModule.class);
        if (searchModule == null || !searchModule.isEnabled()) return;
        
        String query = searchModule.getCurrentSearchQuery();
        if (query == null || query.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return; // Only show when chat screen is open
        
        // Render highlight overlays for matching messages
        booster$renderSearchHighlights(context, searchModule, client);
    }
    
    @Unique
    private void booster$renderSearchHighlights(DrawContext context, SearchMessagesModule searchModule, MinecraftClient client) {
        if (visibleMessages.isEmpty()) return;
        
        String query = searchModule.getCurrentSearchQuery();
        boolean caseSensitive = false; // Could get from module settings
        String searchQuery = caseSensitive ? query : query.toLowerCase();
        
        int highlightColor = searchModule.getHighlightColor();
        // Make highlight semi-transparent
        int bgColor = (highlightColor & 0x00FFFFFF) | 0x40000000; // 25% opacity background
        
        int lineHeight = 9;
        int chatWidth = client.inGameHud.getChatHud().getWidth();
        
        // Calculate chat position
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        
        // Chat is rendered at the bottom left
        int chatX = 4;
        int chatY = scaledHeight - 40; // Approximate chat Y position
        
        context.getMatrices().push();
        
        int visibleCount = Math.min(visibleMessages.size(), 10); // Limit to visible lines
        
        for (int i = 0; i + scrolledLines < visibleMessages.size() && i < visibleCount; i++) {
            ChatHudLine.Visible line = visibleMessages.get(i + scrolledLines);
            
            // Get the text content
            StringBuilder textBuilder = new StringBuilder();
            line.content().accept((index, style, codePoint) -> {
                textBuilder.appendCodePoint(codePoint);
                return true;
            });
            String lineText = textBuilder.toString();
            String searchText = caseSensitive ? lineText : lineText.toLowerCase();
            
            if (searchText.contains(searchQuery)) {
                // Calculate Y position for this line (chat renders bottom-up)
                int lineY = chatY - (i + 1) * lineHeight;
                
                // Find all occurrences and highlight them
                int startIndex = 0;
                while ((startIndex = searchText.indexOf(searchQuery, startIndex)) != -1) {
                    // Calculate X position for the match
                    String beforeMatch = lineText.substring(0, startIndex);
                    int matchX = chatX + client.textRenderer.getWidth(beforeMatch);
                    int matchWidth = client.textRenderer.getWidth(query);
                    
                    // Draw highlight background
                    context.fill(matchX, lineY, matchX + matchWidth, lineY + lineHeight, bgColor);
                    
                    // Draw underline
                    context.fill(matchX, lineY + lineHeight - 1, matchX + matchWidth, lineY + lineHeight, highlightColor);
                    
                    startIndex += searchQuery.length();
                }
            }
        }
        
        context.getMatrices().pop();
    }
}
