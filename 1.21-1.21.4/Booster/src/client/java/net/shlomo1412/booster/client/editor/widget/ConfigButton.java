package net.shlomo1412.booster.client.editor.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.screen.BoosterConfigScreen;

/**
 * Button that opens the Booster configuration screen.
 * Displays a gear icon (⚙) and opens the config when clicked.
 */
public class ConfigButton extends ButtonWidget {
    
    private static final int SIZE = 20;
    
    private ConfigButton(int x, int y, Screen currentScreen) {
        super(x, y, SIZE, SIZE, Text.literal("⚙"), 
            button -> MinecraftClient.getInstance().setScreen(new BoosterConfigScreen(currentScreen)),
            DEFAULT_NARRATION_SUPPLIER);
        
        setTooltip(Tooltip.of(Text.literal("§6Booster Config\n§7Open configuration screen")));
    }
    
    /**
     * Creates a new config button.
     *
     * @param x The X position
     * @param y The Y position
     * @param currentScreen The current screen (to return to)
     * @return A new ConfigButton
     */
    public static ConfigButton create(int x, int y, Screen currentScreen) {
        return new ConfigButton(x, y, currentScreen);
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Custom rendering for a nicer look
        int bgColor = this.isHovered() ? 0xFF3A3A3A : 0xFF2A2A2A;
        int borderColor = this.isHovered() ? 0xFFFFAA00 : 0xFF4A4A4A;
        
        // Background
        context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        
        // Border
        context.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        context.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        context.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        
        // Icon (gear)
        int textColor = this.isHovered() ? 0xFFFFAA00 : 0xFFCCCCCC;
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            this.getMessage(),
            getX() + width / 2,
            getY() + (height - 8) / 2,
            textColor
        );
    }
}
